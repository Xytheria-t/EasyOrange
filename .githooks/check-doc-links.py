#!/usr/bin/env python3
"""文档链接校验 — 仓库内 *.md 的相对链接与锚点是否真实存在。

背景：文档散在 `doc/`、各模块 `AGENTS.md`、根 `README.md`，册间与节间互相引用很密
（`02 §5` 这类节号引用靠人肉维护，相对链接靠路径）。文件改名 / 拆册 / 重排章号时，
链接会静默失效——渲染出来仍可点，只是跳到 404，评审与面试时当场暴露。约定：

  1. 相对链接目标必须存在（文件或目录）；
  2. 带 `#锚点` 的链接，锚点必须能在目标文件的标题里按 GitHub slug 规则生成；
  3. 外链（http/https/mailto）不校验——不引入网络依赖；
  4. `NN §x` / `NN 题 N` 这类**节号引用无法机械校验**（没有显式锚点），仍靠改动时人工同步；
     本脚本只能保证「文件名与锚点」这半边不出错。

用法：
    python3 .githooks/check-doc-links.py      # 退出码 0=全通 / 1=有失效链接
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent

# 扫描范围：仓库内全部 *.md，跳过依赖 / 构建产物 / 本地会话产物
SKIP_DIRS = {".git", "node_modules", "target", "dist", ".venv", ".zcode", "build"}

# [文字](目标) —— 目标是相对路径或 URL，可带 #锚点
LINK_RE = re.compile(r"\[[^\]]*\]\(([^)\s]+?)\)")
HEADING_RE = re.compile(r"^#{1,6}\s+(.*?)\s*$", re.M)
# 手写锚点：<a name="x"></a> / <a id="x"></a>（标题带括号后缀时用来稳定锚点，如「结构计数」）
HTML_ANCHOR_RE = re.compile(r"""<a\s+(?:name|id)=["']([^"']+)["']""", re.I)
EXTERNAL = ("http://", "https://", "mailto:", "tel:")


def github_slug(heading: str) -> str:
    """GitHub 标题锚点规则：小写 → 去标点 → 空白转连字符（保留 CJK 与 _-）。"""
    s = re.sub(r"`", "", heading.strip().lower())
    s = re.sub(r"[^\w\s-]", "", s, flags=re.UNICODE)
    return re.sub(r"\s+", "-", s.strip())


def anchors_of(path: Path) -> set[str]:
    text = path.read_text(encoding="utf-8")
    return {github_slug(h) for h in HEADING_RE.findall(text)} | set(HTML_ANCHOR_RE.findall(text))


def scan() -> list[tuple[Path, str, str]]:
    bad: list[tuple[Path, str, str]] = []
    for md in sorted(ROOT.rglob("*.md")):
        if any(p in SKIP_DIRS for p in md.parts):
            continue
        text = md.read_text(encoding="utf-8")
        for raw in LINK_RE.findall(text):
            if raw.startswith(EXTERNAL):
                continue
            target, _, anchor = raw.partition("#")
            resolved = md if not target else (md.parent / target).resolve()
            if target and not resolved.exists():
                bad.append((md, raw, "目标不存在"))
                continue
            if anchor and resolved.is_file():
                if anchor not in anchors_of(resolved):
                    bad.append((md, raw, "锚点不存在"))
    return bad


def main() -> int:
    bad = scan()
    if not bad:
        print("[doc-links] OK 全部相对链接与锚点可解析")
        return 0
    print(f"[doc-links] FAIL {len(bad)} 处失效链接：")
    for md, raw, why in bad:
        print(f"  {md.relative_to(ROOT)}: [{why}] {raw}")
    print("\n修法：改链接指向真实文件/锚点，或删掉已失效的引用。")
    return 1


if __name__ == "__main__":
    sys.exit(main())
