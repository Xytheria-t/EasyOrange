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
4. 白名单条目在扫描范围内零引用 → 违规。条目失去引用说明它对应的例外已经不存在
   （值被令牌收编，或那段代码被删），留着就是给后来者开的后门：同样的值再写进来
   不会被拦，而账本上看不出这条早已不作数。

白名单是「现存已知例外」的账本：第三方品牌色（微信/QQ）、Canvas 与 SVG stop
等结构性场景的设计色、金银铜热词牌、页面局部令牌的衍生 alpha，以及标注
「遗留」的待暖化债（冷黑 rgba(17,24,39) 系）。

准入判据是「与最近令牌的差是否可辨」，不是「值不值得建令牌」：暖灰阶的半步、
另一色相的骨架灰这类肉眼不可辨的近似值是漂移不是例外，snap 到相邻令牌即可，
登记反而把漂移固化成了政策（2026-10 清掉 8 条正是这类）。新增例外必须带理由。

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
# tsx/ts 侧专用判据（见 check_ts_file）：
#   SVG 呈现属性里的 var() 浏览器支持不一（Chrome/Firefox 不认），色值只能写字面量——
#   脚本 docstring 的「SVG stop 等结构性场景的设计色」白名单条目说的就是它。
SVG_PRESENTATION_ATTR = re.compile(r'\b(?:stopColor|stop-color|stroke|fill|floodColor|flood-color)\s*=\s*["\']')
# var() 的 fallback 是自描述降级（令牌缺失时才生效），其字面色值放行
VAR_WITH_FALLBACK = re.compile(r"var\(\s*--[a-zA-Z][a-zA-Z0-9-]*\s*,[^)]*\)")
# tsx 里注入局部令牌的写法 {'--x': value}：值是令牌本体，与 CSS 的 `--x:` 定义行同口径
TS_VAR_DEF = re.compile(r"['\"](--[a-zA-Z][a-zA-Z0-9-]*)['\"]\s*:")
LINE_COMMENT = re.compile(r"//.*$")
BLOCK_COMMENT = re.compile(r"/\*.*?\*/")

# ---------------------------------------------------------------------------
# 白名单：现存已知例外的精确值。新增必须带理由，别图省事往里塞。
#
# 准入判据是「与最近令牌的差是否可辨」，不是「值不值得建令牌」——差在肉眼不可辨
# 量级的近似值（暖灰阶半步、另一个色相的骨架灰）是漂移不是例外，一律 snap 到相邻
# 令牌，别登记；登记的是结构上无法引用令牌的取值（第三方品牌色 / Canvas / SVG stop）。
# 零引用的条目由下面的 audit_allowlist 拦下——条目一旦失去引用就是给后来者开的后门。
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
    # 本项目色阶里的色的 alpha 形态（--gray-400 / --purple-300）
    # 暖中性 (229,224,219) 2026-10 销账：C 端两处已改走 color-mix(var(--gray-200) N%)，
    # admin.css 那五处属管理端自有令牌体系，不由本脚本管。
    (168, 160, 152),
    (216, 180, 254),
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


def ts_files() -> list[Path]:
    """C 端 ts / tsx —— 色值散在 tsx 里（内联 style、SVG、调色板常量），此前只查 CSS 是盲区。"""
    return sorted(
        p for p in list(SRC.rglob("*.ts")) + list(SRC.rglob("*.tsx"))
        if "admin" not in p.relative_to(SRC).parts and ".test." not in p.name
    )


def check_ts_file(path: Path) -> list[str]:
    """ts / tsx 的裸色值检查 —— 与 CSS 同白名单，豁免按该文件形态另定。"""
    problems: list[str] = []
    in_block_comment = False
    for i, raw in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        code, in_block_comment = strip_comment(raw, in_block_comment)
        code = LINE_COMMENT.sub("", code)
        if "data:" in code and "url(" in code:
            continue
        if SVG_PRESENTATION_ATTR.search(code):
            continue  # SVG 呈现属性不支持 var()，色值只能字面量
        if TS_VAR_DEF.search(code):
            continue  # {'--x': value} 是局部令牌定义，值是令牌本体
        code = VAR_WITH_FALLBACK.sub("var(--)", code)  # fallback 字面色放行
        if not code.strip():
            continue
        for kind in check_color_line(code, False):
            problems.append(f"  {path.relative_to(ROOT)}:{i} [{kind}] {raw.strip()[:100]}")
    return problems


def read_code(path: Path) -> str:
    """整文件读成剥掉注释的代码文本（白名单审计用：注释里的值不算引用）。"""
    out: list[str] = []
    in_block_comment = False
    for raw in path.read_text(encoding="utf-8").splitlines():
        code, in_block_comment = strip_comment(raw, in_block_comment)
        out.append(LINE_COMMENT.sub("", code))
    return "\n".join(out)


def audit_allowlist() -> list[str]:
    """白名单里零引用的条目。与 --staged 无关：白名单是全仓账本，每次运行都核。

    没有这条审计时条目只增不减——值被令牌收编或代码被删之后没人回来销账，条目
    就成了敞着的后门（本轮清出的 #f87171 全仓零引用，#b5aea8 则早就被 SVG 呈现
    属性的规则豁免覆盖、纯属冗余，两种都不会被别的检查发现）。
    """
    sources = [
        read_code(p) for p in css_files() if p.relative_to(SRC).parts[0] != "admin"
    ] + [read_code(p) for p in ts_files()]
    lowered = [s.lower() for s in sources]
    problems: list[str] = []
    for value in sorted(HEX_ALLOW):
        if not any(value in s for s in lowered):
            problems.append(f"  [死条目] hex 白名单 {value} 在扫描范围内零引用")
    for base in sorted(RGB_ALLOW):
        pat = re.compile(rf"rgba?\(\s*{base[0]}\s*,\s*{base[1]}\s*,\s*{base[2]}\b")
        if not any(pat.search(s) for s in sources):
            problems.append(f"  [死条目] 基色白名单 rgb{base} 在扫描范围内零引用")
    return problems


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--staged", action="store_true", help="裸色值只查暂存的 C 端文件")
    args = parser.parse_args()

    files = css_files()
    dead = audit_allowlist()
    c_css = lambda p: not p.relative_to(SRC).parts[0] == "admin"  # noqa: E731
    if args.staged:
        import subprocess

        changed = subprocess.run(
            ["git", "diff", "--name-only", "--cached", "HEAD"],
            cwd=ROOT, capture_output=True, text=True, check=True,
        ).stdout.split()
        c_side = [
            ROOT / name
            for name in changed
            if name.startswith("easyorange-frontend/src/")
            and not name.startswith("easyorange-frontend/src/admin/")
        ]
        staged_css = {p for p in c_side if p.suffix == ".css"}
        staged_ts = [p for p in c_side if p.suffix in (".ts", ".tsx") and ".test." not in p.name]
        if not staged_css and not staged_ts and not dead:
            print("[c-style] OK：本次暂存无 C 端 CSS / ts / tsx")
            return 0
        color_files = {p for p in staged_css if c_css(p)}
    else:
        color_files = {p for p in files if c_css(p)}
        staged_ts = None  # None = 全量

    defined = collect_definitions(files)
    problems: list[str] = list(dead)
    used: dict[str, bool] = {}
    for path in files:
        file_problems, file_used = check_file(path, path in color_files)
        problems.extend(file_problems)
        for var, has_fallback in file_used.items():
            used[var] = used.get(var, False) or has_fallback

    ts_scope = ts_files() if staged_ts is None else staged_ts
    for path in ts_scope:
        problems.extend(check_ts_file(path))

    dangling = sorted(v for v, has_fallback in used.items() if v not in defined and not has_fallback)
    for var in dangling:
        problems.append(f"  [悬空变量] {var} 被 var() 引用但全仓库无定义（无 fallback，静默失效）")

    if not problems:
        print(
            f"[c-style] OK：{len(color_files)} 个 C 端 CSS + {len(ts_scope)} 个 ts/tsx 无白名单外裸色值，"
            f"{len(files)} 个 CSS 无悬空变量引用（白名单 {len(HEX_ALLOW)} hex / {len(RGB_ALLOW)} 基色）"
        )
        return 0

    print("[c-style] C 端样式漂移——视觉取值只能来自 styles/tokens.css：")
    print("\n".join(problems))
    print("  修法：① 需要新颜色 → 在 tokens.css 加令牌（品牌橙 alpha 用 --primary-alpha-1~5 档）")
    print("        ② 确属结构性例外（第三方品牌色 / Canvas / SVG stop）→ 白名单加精确值并写明理由")
    print("        ③ 悬空变量 → 补令牌定义或删除引用；拼错令牌不会报错，只会静默丢样式")
    print("        ④ 死条目 → 从白名单删掉，它对应的例外已经不存在")
    return 1


if __name__ == "__main__":
    sys.exit(main())
