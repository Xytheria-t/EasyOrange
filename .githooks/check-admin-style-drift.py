#!/usr/bin/env python3
"""管理端样式漂移：拦住 tsx 里新冒出来的裸色值与 Tailwind 任意色值。

管理端的视觉取值只有一个来源——`easyorange-frontend/src/admin/styles/admin.css`
的 `:root` 令牌与 `.admin-*` 类。此前 tsx 里散着 181 处裸 hex 与 92 处 Tailwind
任意色值（`text-[#6E6862]` 这类），同一概念在表格、弹窗、抽屉各写一套，且改配色
要翻十几个文件。约定写进 AGENTS.md 没人记得，所以在这里机器拦。

判定规则（只管新增，不管存量——存量已清零）：

1. tsx / ts 里出现裸 hex（`#F97316`）或裸 `rgb()/rgba()` 字面量 → 违规。
   唯一例外是 `styles/` 下的 CSS 文件本身，以及 `chartTheme.ts` 这种只做
   `var(--admin-*)` 引用的模块（它不含字面量，规则 1 已经覆盖）。
2. Tailwind 任意值里带颜色（`text-[#...]`、`bg-[#...]`、`border-[#...]`）→ 违规。
   要用令牌就写 `text-(--admin-muted)` 这种 CSS 变量简写。
3. 引用了未定义的 `--admin-*` 令牌 → 违规（防拼错静默失效，这类 bug 不会报错）。

用法（仓库根目录）：

    python3 .githooks/check-admin-style-drift.py            # 查全部管理端文件
    python3 .githooks/check-admin-style-drift.py --staged   # 只查本次暂存的文件

退出码：0 通过；1 有违规。
"""

import argparse
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ADMIN = ROOT / "easyorange-frontend" / "src" / "admin"
CSS = ADMIN / "styles" / "admin.css"

HEX = re.compile(r"#[0-9A-Fa-f]{3,8}\b")
RGB = re.compile(r"\brgba?\(\s*[\d.]+\s*,")
TWINDY_COLOR = re.compile(r"\b(?:text|bg|border|from|to|via|ring|fill|stroke|shadow|outline|decoration|accent|caret|divide)-\[(?:#[0-9A-Fa-f]{3,8}|rgba?\()")
ADMIN_VAR = re.compile(r"--admin-[a-z0-9-]+")
CSS_VAR_USE = re.compile(r"var\(\s*(--admin-[a-z0-9-]+)")


def defined_tokens() -> set[str]:
    if not CSS.is_file():
        raise SystemExit(f"找不到 {CSS}")
    return set(ADMIN_VAR.findall(CSS.read_text(encoding="utf-8")))


def source_files() -> list[Path]:
    if not ADMIN.is_dir():
        raise SystemExit(f"找不到 {ADMIN}")
    return sorted(p for p in ADMIN.rglob("*") if p.suffix in {".ts", ".tsx"})


def check_file(path: Path, defined: set[str]) -> list[str]:
    lines = path.read_text(encoding="utf-8").splitlines()
    out: list[str] = []
    for i, line in enumerate(lines, 1):
        # 行内注释 / 文档链接里的颜色写法不算视觉取值
        code = line.split("//", 1)[0] if not line.lstrip().startswith("*") else ""
        if not code.strip():
            continue
        hits = []
        if HEX.search(code):
            hits.append("裸 hex")
        if RGB.search(code):
            hits.append("裸 rgb()/rgba()")
        if TWINDY_COLOR.search(code):
            hits.append("Tailwind 任意色值")
        for var in set(CSS_VAR_USE.findall(code)):
            if var not in defined:
                hits.append(f"未定义令牌 {var}")
        for kind in dict.fromkeys(hits):
            out.append(f"  {path.relative_to(ROOT)}:{i} [{kind}] {line.strip()[:100]}")
    return out


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--staged", action="store_true", help="只查本次暂存的管理端文件")
    args = parser.parse_args()

    if args.staged:
        import subprocess

        changed = subprocess.run(
            ["git", "diff", "--name-only", "--cached", "HEAD"],
            cwd=ROOT, capture_output=True, text=True, check=True,
        ).stdout.split()
        files = [
            ROOT / name for name in changed
            if name.startswith("easyorange-frontend/src/admin/") and Path(name).suffix in {".ts", ".tsx"}
        ]
        if not files:
            print("[admin-style] OK：本次暂存无管理端文件")
            return 0
    else:
        files = source_files()

    defined = defined_tokens()
    problems: list[str] = []
    for path in files:
        problems.extend(check_file(path, defined))

    if not problems:
        print(f"[admin-style] OK：{len(files)} 个管理端文件无裸色值 / 任意色值 / 未定义令牌"
              f"（已定义 {len(defined)} 个 --admin-* 令牌）")
        return 0

    print("[admin-style] 管理端样式漂移——视觉取值只能来自 styles/admin.css：")
    print("\n".join(problems))
    print("  修法：① 需要新颜色 → 在 admin.css 的 :root 加令牌，页面写 var(--admin-x)")
    print("        ② 需要 Tailwind 类 → 用变量简写 text-(--admin-muted)，不要写 text-[#6E6862]")
    print("        ③ 重复的视觉块 → 在 admin.css 提一个 .admin-* 类，tsx 只留 className")
    return 1


if __name__ == "__main__":
    sys.exit(main())
