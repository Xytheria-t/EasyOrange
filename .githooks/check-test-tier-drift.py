#!/usr/bin/env python3
"""测试口径漂移校验 — 文档对「集成测试」的声明 vs 代码事实。

背景：`doc/工程指标.md` §3.1 曾长期写着「Testcontainers 集成测试无法跑 → 全量改为单元测试 +
Mockito mock」，当时的架构文档测试分层表写着「集成测试已移除（WSL2 Docker 兼容性限制）」，
而代码里 11 个 `*IT` 早已由 failsafe 在 `mvn verify` 真实跑 MySQL/Redis/RabbitMQ（TD-001，2026-08-07 已还）。
两份文档互相矛盾、且都与代码不符 —— 评审/面试当场可查，会连带质疑其余数字。约定：

  1. IT 是否存在、经什么机制运行，**以代码为准**：`*IT.java` 文件 + pom 的 failsafe 绑定；
  2. 文档不得声明 IT 已移除 / 无法运行 / 全量退回单测；
  3. 反向同理：文档声称「failsafe 绑定 `mvn verify`」时，pom 必须真有该绑定。

用法：
    python3 .githooks/check-test-tier-drift.py      # 退出码 0=一致 / 1=漂移
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
BACKEND = ROOT / "easyorange-backend"
POM = BACKEND / "pom.xml"

# 文档扫描范围：仓库内全部 *.md，跳过依赖/构建产物目录
SKIP_DIRS = {".git", "node_modules", "target", "dist", ".venv"}

# 代码事实存在 IT 时，文档不得出现的「能力已不存在」声明
FORBIDDEN_CLAIMS = [
    (re.compile(r"集成测试.{0,12}已移除"), "声称集成测试已移除"),
    (re.compile(r"集成测试.{0,12}无法跑"), "声称集成测试无法运行"),
    (re.compile(r"全量改为单元测试"), "声称全量退回单元测试"),
]

FAILSAFE_CLAIM = re.compile(r"failsafe")
FAILSAFE_PLUGIN = "<artifactId>maven-failsafe-plugin</artifactId>"
FAILSAFE_GOAL = "<goal>integration-test</goal>"


def doc_files() -> list[Path]:
    return sorted(
        p
        for p in ROOT.rglob("*.md")
        if not SKIP_DIRS.intersection(p.relative_to(ROOT).parts[:-1])
    )


def it_files() -> list[Path]:
    return [p for p in BACKEND.rglob("*IT.java") if "target" not in p.parts]


def failsafe_bound(pom_text: str) -> bool:
    return FAILSAFE_PLUGIN in pom_text and FAILSAFE_GOAL in pom_text


def main() -> int:
    if not POM.exists():
        print(f"[test-tier-drift] 缺少文件，跳过校验：{POM.relative_to(ROOT)}")
        return 0

    pom_text = POM.read_text(encoding="utf-8")
    its = it_files()
    bound = failsafe_bound(pom_text)
    drift: list[str] = []

    for path in doc_files():
        rel = path.relative_to(ROOT)
        for lineno, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
            if its and bound:
                for pattern, what in FORBIDDEN_CLAIMS:
                    if pattern.search(line):
                        drift.append(f"  {rel}:{lineno} {what}，但代码有 {len(its)} 个 *IT 且 failsafe 已绑定")
            if FAILSAFE_CLAIM.search(line) and not bound:
                drift.append(f"  {rel}:{lineno} 提到 failsafe，但 pom 未绑定 maven-failsafe-plugin/integration-test")

    if drift:
        print("[test-tier-drift] FAIL: 文档的测试口径与代码事实不一致：", file=sys.stderr)
        for item in sorted(set(drift)):
            print(item, file=sys.stderr)
        print(
            f"\n代码事实：{len(its)} 个 *IT 文件，failsafe 绑定={'是' if bound else '否'}"
            "（详见 doc/技术债务清单.md TD-001）。\n"
            "修法：改文档使其与代码一致（唯一权威落点见 TD-001）；跳过本次校验：SKIP=git-hooks。",
            file=sys.stderr,
        )
        return 1

    print(f"[test-tier-drift] OK {len(its)} 个 *IT / failsafe 绑定={'是' if bound else '否'}，文档口径一致")
    return 0


if __name__ == "__main__":
    sys.exit(main())
