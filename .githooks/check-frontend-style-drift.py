#!/usr/bin/env python3
"""C 端样式漂移：拦住页面 CSS 里新冒出来的裸色值与悬空 CSS 变量引用。

C 端视觉取值的来源是 `easyorange-frontend/src/styles/tokens.css`（全局加载）。
2026-09 全站收口过一轮：约 600 处裸 hex/rgba 映射进令牌、11 档散落的品牌橙
alpha 收敛为 --primary-alpha-1~5、清理 10 处悬空 var() 引用（其中 --card-glow
导致 AI 卡 hover 光晕从未渲染过）。没有门禁的话下一轮页面还会散回去，所以锁住：
已存值进白名单（精确值，见下方 ALLOW 常量），白名单外即违规。

判定规则：

1. 页面 CSS（src/ 下除 src/admin/ 外的 .css）出现白名单外的裸 hex 或
   rgb()/rgba() → 违规。豁免：`:root` / `@theme` 块（令牌本体）、`--x:` 定义行、
   mask 声明里的 #fff/#000（打孔技巧的功能色）、data: URI 内联资源、注释。
2. 引用了「无 fallback 且全仓库未定义」的 CSS 变量 → 违规。悬空 var() 静默
   失效不报错，这类 bug 只能靠机器拦；带 fallback 的引用是自描述降级，放行。
   定义集含全部 CSS（含 admin.css，同一 SPA 全局生效）与 tsx/ts 注入的变量。
3. admin 目录不在本脚本范围：tsx 侧由 check-admin-style-drift.py 管，
   admin.css 是令牌本体。

白名单是「现存已知例外」的账本：第三方品牌色（微信/QQ）、Canvas 与 SVG stop
等结构性场景的设计色、金银铜热词牌、页面局部令牌的衍生 alpha，以及标注
「遗留」的待暖化债（冷黑 rgba(17,24,39) 系）。新增例外必须带理由加进白名单。

用法（仓库根目录）：

    python3 .githooks/check-frontend-style-drift.py            # 查全部 CSS
    python3 .githooks/check-frontend-style-drift.py --staged   # 裸色值只查暂存文件

退出码：0 通过；1 有违规。
"""

import argparse
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "easyorange-frontend" / "src"

HEX = re.compile(r"#[0-9A-Fa-f]{3,8}\b")
RGB = re.compile(r"\brgba?\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)")
CSS_VAR_DEF = re.compile(r"^\s*(--[a-zA-Z][a-zA-Z0-9-]*)\s*:")
CSS_VAR_USE = re.compile(r"var\(\s*(--[a-zA-Z][a-zA-Z0-9-]*)\s*(,)?")
MASK_DECL = re.compile(r"^(?:-webkit-)?mask(?:-image)?\s*:")

# ---------------------------------------------------------------------------
# 白名单：现存已知例外的精确值。新增必须带理由，别图省事往里塞。
# ---------------------------------------------------------------------------

# 第三方品牌色（微信 / QQ / 邮箱客户端），语义色不归我们管
HEX_ALLOW = {
    "#07c160",
    "#12b7f5",
    "#e6162d",
    # 低频装饰色（AI 功能区点缀 / 图表 / 徽章），未到建令牌档的量
    "#94a3b8",
    "#92400e",
    "#78350f",
    "#6d28d9",
    "#7c3aed",
    "#6366f1",
    "#4f46e5",
    "#6172f3",
    "#8b5cf6",
    "#a78bfa",
    "#ec4899",
    "#f472b6",
    "#06b6d4",
    "#0f766e",
    "#b45309",
    "#f87171",
    # 页面自造品牌色的衍生（profile 域 --profile-accent 系，遗留待收敛）
    "#172033",
    "#ef7d23",
    "#ffcf96",
    # 暖白 / 波浪分隔线 / 氛围背景设计色，无精确令牌
    "#fff9f5",
    "#fff5f0",
    "#f5f0eb",
    "#fff0e4",
    "#ffe4d4",
    "#ffd4c7",
    "#fff8f0",
    "#fffaf4",
    "#fff5ea",
    "#f4f7fb",
    "#fff8e7",
    "#fff6f0",
    "#faf8f5",
    "#f7f5f2",
    # 金银铜热词牌（排行榜前三名）
    "#ffd700",
    "#ffa500",
    "#c0c0c0",
    "#a0a0a0",
    "#cd7f32",
    "#b87333",
}

# rgba/rgb 白名单按「基色三元组」收，任意 alpha 合法——防的是发明新基色。
RGB_ALLOW = {
    # 中性：白玻璃 / 纯黑遮罩阴影 / 暖黑阴影（令牌 --shadow-* 的基色）
    (255, 255, 255),
    (0, 0, 0),
    (42, 37, 32),
    (26, 22, 18),
    # 品牌色 alpha 形态（橙 / 玫红 / 紫 / 金 / 绿 / 玫红深 / 红系 / 蓝系）
    (249, 115, 22),
    (251, 113, 133),
    (195, 155, 211),
    (147, 51, 234),
    (251, 191, 36),
    (245, 158, 11),
    (16, 185, 129),
    (5, 150, 105),
    (52, 211, 153),
    (244, 63, 94),
    (225, 29, 72),
    (239, 68, 68),
    (220, 38, 38),
    (59, 130, 246),
    (96, 165, 250),
    (139, 92, 246),
    (99, 102, 241),
    (251, 146, 60),
    (234, 88, 12),
    # 遗留冷黑（product-detail / profile 多层阴影，待暖化债）
    (17, 24, 39),
    (21, 30, 52),
    (23, 32, 51),
    # 页面局部令牌衍生 alpha（profile 域 --profile-* / 浮岛 / 氛围光球）
    (217, 92, 7),
    (239, 125, 35),
    (255, 190, 126),
    (105, 153, 255),
    (255, 153, 64),
    (88, 132, 255),
    (255, 206, 120),
    (97, 114, 243),
    (102, 126, 234),
    (118, 75, 162),
    # 暖白 / 冷白背景渐变 stop（无精确令牌的氛围色）
    (254, 250, 245),
    (255, 247, 237),
    (255, 250, 245),
    (255, 248, 240),
    (254, 243, 238),
    (240, 253, 244),
    (250, 248, 245),
    (248, 246, 243),
    (245, 243, 240),
    (232, 228, 224),
    (250, 251, 253),
    (248, 249, 252),
    (255, 247, 239),
    (255, 253, 249),
    (255, 252, 247),
    (255, 243, 224),
    (255, 237, 213),
    (255, 253, 250),
    (255, 250, 244),
    (255, 251, 245),
    (255, 245, 246),
    (252, 248, 255),
    (249, 250, 252),
}


def strip_comment(line: str, in_comment: bool) -> tuple[str, bool]:
    """跨行块注释状态机：返回 (剥离注释后的代码, 新的注释状态)。"""
    out: list[str] = []
    i = 0
    while i < len(line):
        if in_comment:
            j = line.find("*/", i)
            if j == -1:
                return "", True
            in_comment = False
            i = j + 2
        else:
            j = line.find("/*", i)
            if j == -1:
                out.append(line[i:])
                break
            out.append(line[:j])
            i = j + 2
            in_comment = True
    return "".join(out), in_comment


def css_files() -> list[Path]:
    if not SRC.is_dir():
        raise SystemExit(f"找不到 {SRC}")
    return sorted(p for p in SRC.rglob("*.css"))


def collect_definitions(files: list[Path]) -> set[str]:
    """全部 CSS 的 --x: 定义 + tsx/ts 注入的 '--x' 字符串（悬空检查的定义集）。"""
    defined: set[str] = set()
    for p in files:
        for line in p.read_text(encoding="utf-8").splitlines():
            m = CSS_VAR_DEF.match(line)
            if m:
                defined.add(m.group(1))
    for p in SRC.rglob("*.ts"):
        defined |= set(re.findall(r"'(--[a-zA-Z][a-zA-Z0-9-]*)'", p.read_text(encoding="utf-8")))
    for p in SRC.rglob("*.tsx"):
        defined |= set(re.findall(r"'(--[a-zA-Z][a-zA-Z0-9-]*)'", p.read_text(encoding="utf-8")))
        defined |= set(re.findall(r'"(--[a-zA-Z][a-zA-Z0-9-]*)"', p.read_text(encoding="utf-8")))
    return defined


def check_color_line(code: str, mask_decl: bool) -> list[str]:
    """单行裸色值检查，返回违规类型列表。mask 声明中的 #fff/#000 是功能色。"""
    hits: list[str] = []
    for m in HEX.finditer(code):
        v = m.group(0).lower()
        if mask_decl and v in {"#fff", "#000"}:
            continue
        if v not in HEX_ALLOW:
            hits.append(f"裸 hex {v}")
    for m in RGB.finditer(code):
        base = (int(m.group(1)), int(m.group(2)), int(m.group(3)))
        if base not in RGB_ALLOW:
            hits.append(f"裸 rgb()/rgba() rgb{base}")
    return list(dict.fromkeys(hits))


def check_file(path: Path, check_colors: bool) -> tuple[list[str], dict[str, bool]]:
    """返回 (违规行, 本文件收集到的 var() 引用 → 是否带 fallback)。令牌块内只收定义不查色值。"""
    lines = path.read_text(encoding="utf-8").splitlines()
    problems: list[str] = []
    used: dict[str, bool] = {}
    depth = 0
    token_block_depth: int | None = None
    mask_decl = False
    in_comment = False
    for i, line in enumerate(lines, 1):
        if token_block_depth is not None and depth <= token_block_depth:
            token_block_depth = None
        code, in_comment = strip_comment(line, in_comment)
        if "data:" in code and "url(" in code:
            continue  # data: URI 内联资源（SVG noise 等），色值在资源内部
        selector = code.split("{", 1)[0]
        entering_token_block = (
            token_block_depth is None
            and "{" in code
            and (":root" in selector or "@theme" in selector)
        )
        if entering_token_block:
            token_block_depth = depth
        in_token_block = token_block_depth is not None
        is_var_def = CSS_VAR_DEF.match(code.strip()) is not None
        # mask 声明（开启行含分号结尾也算）里的 #fff/#000 是打孔技巧的功能色
        is_mask_line = mask_decl or bool(MASK_DECL.match(code.strip()))
        if MASK_DECL.match(code.strip()):
            mask_decl = True
        if mask_decl and ";" in code:
            mask_decl = False

        for var, has_fallback in CSS_VAR_USE.findall(code):
            used[var] = used.get(var, False) or bool(has_fallback)

        # 令牌块与 --x: 定义行（含 .profile-body 这类局部令牌）是令牌本体，不查色值
        if check_colors and not in_token_block and not is_var_def and code.strip():
            for kind in check_color_line(code, is_mask_line):
                problems.append(f"  {path.relative_to(ROOT)}:{i} [{kind}] {line.strip()[:100]}")

        depth += code.count("{") - code.count("}")
    return problems, used


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--staged", action="store_true", help="裸色值只查暂存的 C 端 CSS")
    args = parser.parse_args()

    files = css_files()
    c_css = lambda p: not p.relative_to(SRC).parts[0] == "admin"  # noqa: E731
    if args.staged:
        import subprocess

        changed = subprocess.run(
            ["git", "diff", "--name-only", "--cached", "HEAD"],
            cwd=ROOT, capture_output=True, text=True, check=True,
        ).stdout.split()
        staged = {
            ROOT / name
            for name in changed
            if name.startswith("easyorange-frontend/src/")
            and not name.startswith("easyorange-frontend/src/admin/")
            and name.endswith(".css")
        }
        if not staged:
            print("[c-style] OK：本次暂存无 C 端 CSS")
            return 0
        color_files = {p for p in staged if c_css(p)}
    else:
        color_files = {p for p in files if c_css(p)}

    defined = collect_definitions(files)
    problems: list[str] = []
    used: dict[str, bool] = {}
    for path in files:
        file_problems, file_used = check_file(path, path in color_files)
        problems.extend(file_problems)
        for var, has_fallback in file_used.items():
            used[var] = used.get(var, False) or has_fallback

    dangling = sorted(v for v, has_fallback in used.items() if v not in defined and not has_fallback)
    for var in dangling:
        problems.append(f"  [悬空变量] {var} 被 var() 引用但全仓库无定义（无 fallback，静默失效）")

    if not problems:
        print(
            f"[c-style] OK：{len(color_files)} 个 C 端 CSS 无白名单外裸色值，"
            f"{len(files)} 个 CSS 无悬空变量引用（白名单 {len(HEX_ALLOW)} hex / {len(RGB_ALLOW)} 基色）"
        )
        return 0

    print("[c-style] C 端样式漂移——视觉取值只能来自 styles/tokens.css：")
    print("\n".join(problems))
    print("  修法：① 需要新颜色 → 在 tokens.css 加令牌（品牌橙 alpha 用 --primary-alpha-1~5 档）")
    print("        ② 确属结构性例外（第三方品牌色 / Canvas / SVG stop）→ 白名单加精确值并写明理由")
    print("        ③ 悬空变量 → 补令牌定义或删除引用；拼错令牌不会报错，只会静默丢样式")
    return 1


if __name__ == "__main__":
    sys.exit(main())
