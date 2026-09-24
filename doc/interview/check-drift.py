#!/usr/bin/env python3
"""走读防漂移：核 02-代码走读.md 点名的类与代码骨架是否还对得上 easyorange-backend。

两个模式：

1. 默认（路径巡检）：核 02「**文件**」行点名的 .java 路径仍在源码里。改名 / 删类 / 搬
   包会红——大改项目结构时最常见的漂移。
2. `--changed`（重核清单）：读本轮 git 改动，凡碰到 02 点名的类，列出该重核的节号。
   类还在但整类被重写时默认模式不红（路径没变），这正是「AI 判定不现代就大改整类」
   场景的漏网处——脚本在这里只给工作清单，节内容仍要对着源码重核。

用法（仓库根目录）：

    python3 doc/interview/check-drift.py            # 路径巡检，无漂移退出 0
    python3 doc/interview/check-drift.py --changed  # 本轮改动的重核清单，命中退出 1

范围：只核 02 的 .java 路径与节归属，不核散文与关键代码块的逐字文本——02 的代码块
是简写速写骨架（面试要讲的是控制流与降级口径，不是逐行复现），逐字比对会永远红。
"""

import re
import subprocess
import sys
from pathlib import Path

DOC = Path(__file__).parent / "02-代码走读.md"
ROOT = Path(__file__).parents[2]
BACKEND = ROOT / "easyorange-backend"
JAVA_TOKEN = re.compile(r"`([^`]*\.java)`")


def walked_sections() -> list[tuple[str, list[tuple[str, str]]]]:
    """返回 02 的 [(节标题, [(文档里写的路径, 文件名)])]，按文档顺序。

    文档路径里的 `.../` 是省略段，只保留它后面的模块内后缀参与匹配；裸文件名
    （如 §3 的 `ChatPromptAssembler.java`）只按文件名匹配。
    """
    out: list[tuple[str, list[tuple[str, str]]]] = []
    section = ""
    for line in DOC.read_text(encoding="utf-8").splitlines():
        if line.startswith("## "):
            section = line.lstrip("# ").strip()
        entries = [(t, Path(t.rsplit(".../", 1)[-1]).name) for t in JAVA_TOKEN.findall(line)]
        if entries:
            out.append((section, entries))
    return out


def matches(java_paths: list[str], token: str, name: str) -> bool:
    if "/" in token:
        return any(p.endswith("/" + token.rsplit(".../", 1)[-1]) for p in java_paths)
    return any(p.endswith("/" + name) for p in java_paths)


def changed_classes(sections) -> list[tuple[str, str, str]]:
    """返回 [(节标题, 类名, 改动文件路径)]：本轮 git 改动里碰到 02 点名的类。"""
    diff = subprocess.run(
        ["git", "diff", "--name-only", "HEAD"],
        cwd=ROOT, capture_output=True, text=True, check=True,
    ).stdout.split()
    by_name = {name: sec for sec, entries in sections for _, name in entries}
    return [
        (by_name[p.name], p.stem, p.as_posix())
        for p in map(Path, diff)
        if p.suffix == ".java" and p.name in by_name
    ]


def main() -> int:
    sections = walked_sections()

    if "--changed" in sys.argv:
        hits = changed_classes(sections)
        if not hits:
            print("OK：本轮改动没碰到 02 点名的类")
            return 0
        print("本轮改动碰到 02 点名的类——路径巡检未必红（整类重写不改名不改包），"
              "这些节要对源码重核：")
        for sec, name, path in hits:
            print(f"  [{sec}] {name} ← {path}")
        return 1

    if not BACKEND.is_dir():
        raise SystemExit(f"找不到 {BACKEND}")
    java_paths = [p.as_posix() for p in BACKEND.rglob("*.java")]
    missing = [
        (sec, token)
        for sec, entries in sections
        for token, name in entries
        if not matches(java_paths, token, name)
    ]
    if not missing:
        total = sum(len(entries) for _, entries in sections)
        print(f"OK：02 点名的 {total} 处 .java 路径全部存在")
        return 0
    print("走读漂移：02 点名的路径在源码里已不存在，改代码后必须同步改 02：")
    for sec, token in missing:
        print(f"  [{sec}] {token}")
    return 1


if __name__ == "__main__":
    sys.exit(main())
