#!/usr/bin/env python3
"""AI 上下文预算校验 — 交给模型的指令文件不得无限膨胀。

背景：仓库曾同时存在 1 份根 AGENTS.md（6,958 字符）+ 11 个模块级 AGENTS.md
（合计 84,363 字符）+ `.claude/rules/ecc/`（32 文件 / 86,724 字符的通用编码规则包）。
其中 `.claude/rules/ecc/` 在 ZCode 里根本没有加载机制（ZCode 只识别 AGENTS.md / skills /
commands / MCP / hooks / plugins，没有 path 激活的 rules），是纯死重；模块级 AGENTS.md 也不
会被自动注入（ZCode 只从工作目录向上解析**一个** workspace AGENTS.md），只在被显式读取时
消耗上下文，而它们的内容大半是目录树与端口对照表 —— `find` 一秒可得，写进文档只会漂移。

于是约定：

  1. **只有 3 份 AGENTS.md**：`AGENTS.md`（根，每会话常驻）、`easyorange-backend/AGENTS.md`、
     `easyorange-frontend/AGENTS.md`（懒加载，进对应目录工作时才读）。新增第 4 份即失败 ——
     模块边界由 `ArchitectureRulesTest` 可执行地守卫，不需要每个模块再写一份散文。
  2. **根 AGENTS.md 是唯一每轮都付费的文件**，预算 3,000 字符。放「违反即返工」的硬约束与
     参考索引（指向 doc/），不放目录树、类清单、演进叙事 —— 那些属于 README / ADR / doc/。
  3. 三份合计预算 18,000 字符：懒加载文件可以厚，但不能退化成需要人通读的百科。

用法：
    python3 .githooks/check-context-budget.py      # 退出码 0=在预算内 / 1=超预算或结构违规
"""

from __future__ import annotations

import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent

# 允许存在的 AGENTS.md（相对仓库根）
ALLOWED = (
    Path("AGENTS.md"),
    Path("easyorange-backend/AGENTS.md"),
    Path("easyorange-frontend/AGENTS.md"),
)

# 根文件预算：唯一每会话自动注入的文件
ROOT_BUDGET = 3_000
# 全部文件合计预算
TOTAL_BUDGET = 18_000

SKIP_DIRS = {".git", "node_modules", "target", "dist", ".venv"}


def find_agents_files() -> list[Path]:
    return sorted(
        p
        for p in ROOT.rglob("AGENTS.md")
        if not SKIP_DIRS.intersection(p.relative_to(ROOT).parts[:-1])
    )


def main() -> int:
    found = {p.relative_to(ROOT) for p in find_agents_files()}
    problems: list[str] = []

    # 规则 1：结构 —— 不得新增第 4 份
    unexpected = sorted(found - set(ALLOWED))
    for rel in unexpected:
        problems.append(
            f"  {rel}：模块级 AGENTS.md 不会被自动加载（ZCode 只解析工作目录向上的一个），"
            "内容并入 easyorange-backend/AGENTS.md 或删除"
        )

    # 规则 2 / 3：预算
    root_path = ROOT / "AGENTS.md"
    total = 0
    if root_path.exists():
        root_size = len(root_path.read_text(encoding="utf-8"))
        total += root_size
        if root_size > ROOT_BUDGET:
            problems.append(
                f"  AGENTS.md：{root_size:,} 字符 > 预算 {ROOT_BUDGET:,}（每会话常驻，"
                "把目录树 / 类清单 / 演进叙事移到 doc/ 或 ADR）"
            )

    for rel in sorted(found ^ {Path("AGENTS.md")}):
        path = ROOT / rel
        if path.exists():
            total += len(path.read_text(encoding="utf-8"))

    if total > TOTAL_BUDGET:
        problems.append(f"  AGENTS.md 合计 {total:,} 字符 > 预算 {TOTAL_BUDGET:,}")

    if problems:
        print("[context-budget] FAIL: AI 上下文预算或结构违规：", file=sys.stderr)
        for item in problems:
            print(item, file=sys.stderr)
        print(
            f"\n现状：{len(found)} 份 AGENTS.md，合计 {total:,} 字符"
            f"（根 {len(root_path.read_text(encoding='utf-8')) if root_path.exists() else 0:,} / 预算 {ROOT_BUDGET:,}）。\n"
            "判据：声明只有「读代码看不出来」的约定（反直觉默认、静默失败、跨文件装配事实），"
            "不写可 `find` / `grep` 得到的清单。跳过本次校验：SKIP=git-hooks。",
            file=sys.stderr,
        )
        return 1

    print(
        f"[context-budget] OK {len(found)} 份 AGENTS.md，"
        f"根 {len(root_path.read_text(encoding='utf-8')):,}/{ROOT_BUDGET:,} 字符，合计 {total:,}/{TOTAL_BUDGET:,}"
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
