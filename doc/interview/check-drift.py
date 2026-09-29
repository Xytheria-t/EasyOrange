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
    python3 doc/interview/check-drift.py --symbols  # 类名巡检，散文里的项目类名是否还在

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


# 第三方 / JDK / 框架自带的类，文档会提到但不在本仓源码里。
# 要写进这个清单得给出理由——它是「允许不校验」的唯一入口。
EXTERNAL_CLASSES = {
    "ChatClient",  # Spring AI 的高层门面，本项目只用到 ChatModel 一层
    "ChatModel",  # Spring AI 抽象接口，由本仓 AiModelConfig 实现
    "ToolCallback",  # Spring AI 工具执行回调
}

# 已从源码删除、但 02 保留类名作为「为什么删」的证据。
# 与 EXTERNAL_CLASSES 分开：这里的名字必须曾经属于本仓，删了就该在文档里消失。
DELETED_CLASSES = {
    "AiSearchEnhancerAdapter",  # §4 讲删除理由，随搜索页四路增强整节删除（2026-09）
}

CODE_FENCE = re.compile(r"```.*?```", re.S)
IDENT = re.compile(r"`([A-Z][A-Za-z0-9]*)`")


def symbol_drift() -> list[tuple[int, str]]:
    """散文中形如 `FooBar` 的项目类名，在源码里已查无此物。

    跳过围栏代码块——02 的代码块是明示的示意名骨架，逐字比对必然红。
    """
    prose = CODE_FENCE.sub("", DOC.read_text(encoding="utf-8"))
    java = "\n".join(p.read_text(encoding="utf-8", errors="ignore") for p in BACKEND.rglob("*.java"))
    out = []
    for i, line in enumerate(prose.splitlines(), 1):
        for name in IDENT.findall(line):
            if name in EXTERNAL_CLASSES or name in DELETED_CLASSES:
                continue
            if re.search(rf"\b{re.escape(name)}\b", java):
                continue
            out.append((i, name))
    return out


def main_symbols() -> int:
    if not BACKEND.is_dir():
        raise SystemExit(f"找不到 {BACKEND}")
    bad = symbol_drift()
    if not bad:
        print("OK：02 散文中提到的项目类名在源码里都存在")
        return 0
    print(f"走读漂移：02 散文里提到但源码中查无此物的类名 {len(bad)} 处：")
    for line, name in bad:
        print(f"  L{line}: {name}（改过类名要同步 02；确实是外部类就加进 EXTERNAL_CLASSES）")
    return 1


if __name__ == "__main__":
    if "--symbols" in sys.argv:
        sys.exit(main_symbols())
    sys.exit(main())
