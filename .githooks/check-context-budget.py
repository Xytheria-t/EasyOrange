#!/usr/bin/env python3
"""AI 上下文预算校验 — 交给模型的指令文件不得无限膨胀。

背景：这类文件曾膨胀到 12 份 AGENTS.md（9 万字符）外加一个 8.6 万字符的通用 rules 包，
大半是 `find` 一秒可得的目录树与端口表 —— 已删净，本脚本守住不再反弹。不写工具名：
加载语义随工具版本变，点名的叙述必漂移（2026-10 实测，一次改名就废了一段断言）。

于是约定：

  1. **只有 3 份 AGENTS.md**：`AGENTS.md`（根，每会话常驻）、`easyorange-backend/AGENTS.md`、
     `easyorange-frontend/AGENTS.md`（非每轮常驻，只在进到该目录干活时加载）。新增第 4 份
     即失败 —— 模块边界由 `ArchitectureRulesTest` 可执行地守卫，不需要每个模块再写一份散文。
  2. **根 AGENTS.md 是唯一每轮都付费的文件**，预算 3,000 字符。放「违反即返工」的硬约束与
     参考索引（指向 doc/），不放目录树、类清单、演进叙事 —— 那些属于 README / ADR / doc/。
  3. **单个嵌套册预算 13,500 字符**：懒加载文件可以厚，但不能退化成需要人通读的百科。
     用单文件上限而非合计 —— 嵌套册并不同时进上下文（根 + 后端、根 + 前端，二选一），
     合计会把永不共存的文件绑在同一个数字上：改前端册被后端册的体积卡住。
     该上限等价于「根 + 任一嵌套 ≤ 16,500」，即实际同驻量的上界。

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
# 单个嵌套册预算：与根并不同时受限，故按文件而非按合计（见 docstring 约定 3）
NESTED_BUDGET = 13_500

SKIP_DIRS = {".git", "node_modules", "target", "dist", ".venv", ".claude"}


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
            f"  {rel}：不允许新增第 4 份 AGENTS.md（模块边界由 ArchitectureRulesTest 守卫），"
            "内容并入 easyorange-backend/AGENTS.md 或删除"
        )

    # 规则 2 / 3：预算（逐文件判，不求和 —— 嵌套册与根并不同时进上下文）
    sizes = {
        rel: len((ROOT / rel).read_text(encoding="utf-8"))
        for rel in sorted(found)
        if (ROOT / rel).exists()
    }
    root_size = sizes.get(Path("AGENTS.md"), 0)
    if root_size > ROOT_BUDGET:
        problems.append(
            f"  AGENTS.md：{root_size:,} 字符 > 预算 {ROOT_BUDGET:,}（每会话常驻，"
            "把目录树 / 类清单 / 演进叙事移到 doc/ 或 ADR）"
        )

    nested = {rel: n for rel, n in sizes.items() if rel != Path("AGENTS.md")}
    for rel, size in nested.items():
        if size > NESTED_BUDGET:
            problems.append(
                f"  {rel}：{size:,} 字符 > 嵌套册预算 {NESTED_BUDGET:,}"
                "（进该目录即加载，把可 `find` / `grep` 得到的清单移出去）"
            )

    # 每个嵌套册各自带预算：只写 "12,774、2,914/13,500" 会读成「只有最后一个有预算」，
    # 而这条输出正是用来瞄余量的——余量看不清等于没有。
    shape = f"根 {root_size:,}/{ROOT_BUDGET:,}"
    if nested:
        shape += "，嵌套 " + "、".join(f"{n:,}/{NESTED_BUDGET:,}" for n in nested.values())

    if problems:
        print("[context-budget] FAIL: AI 上下文预算或结构违规：", file=sys.stderr)
        for item in problems:
            print(item, file=sys.stderr)
        print(
            f"\n现状：{len(found)} 份 AGENTS.md，{shape}。\n"
            "判据：声明只有「读代码看不出来」的约定（反直觉默认、静默失败、跨文件装配事实），"
            "不写可 `find` / `grep` 得到的清单。跳过本次校验：SKIP=git-hooks。",
            file=sys.stderr,
        )
        return 1

    print(f"[context-budget] OK {len(found)} 份 AGENTS.md，{shape}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
