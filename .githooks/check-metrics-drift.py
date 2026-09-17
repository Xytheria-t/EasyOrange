#!/usr/bin/env python3
"""结构计数漂移校验 — 文档声称的「N 模块 / N Port / N ADR / N 消费者 / N 表 / N 条 ArchUnit 规则 / N 个 Prompt 模板」vs 代码事实。

背景：这几类计数无单一来源、靠人肉同步，已经漂移过一整轮 —— README 写 11 条 ADR（实际 12）、
根 AGENTS.md 写 11 个（实际 12）、面试文档写 10 个消费者（实际 12）、mermaid 写 32 表（实际 33）、
`ArchitectureRulesTest` 被写成 10 条（实际 12，文件 javadoc 自己写着 12）、Prompt 模板一处写 6
（实际 8，同一文件另一处写对）。评审/面试当场可查，一个数字错了会连带质疑其余全部数字。

约定：
  1. 计数**以代码为准**（下表每项都给出推导方式），文档必须与之一致；
  2. **ADR 正文豁免**：ADR 模板规则 9 要求「已接受的 ADR 实现细节漂移时不改正文，在文件顶部加
     `> **现状更新（日期）**` 横幅说明」——所以 `doc/adr/NNNN-*.md` 的非横幅行按历史口径豁免，
     横幅行（含「现状更新」的引用块）仍参与校验；
  3. 其余文档（README / AGENTS.md / doc/**）一律参与校验。

用法：
    python3 .githooks/check-metrics-drift.py      # 退出码 0=一致 / 1=漂移
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
BACKEND = ROOT / "easyorange-backend"
MIGRATION = BACKEND / "easyorange-application/src/main/resources/db/migration"
ARCH_TEST = (
    BACKEND
    / "easyorange-application/src/test/java/com/cartethyia/easyorange/architecture/ArchitectureRulesTest.java"
)

SKIP_DIRS = {".git", ".zcode", "node_modules", "target", "dist", ".venv", "archunit_store"}

# (名称, 只匹配「在说总数」的写法, 期望值来源)
# 刻意写窄：模式宁可漏检也不误报 —— 会误报的检查器最终会被 SKIP 掉，比没有更糟。
# 已知需回避的同形异义：`4 模块`（CQRS 作用域）、`V1 的 32 张表`（单脚本表数，非总数）、
# `11 个 DLQ`（ADR-0005 决策时点口径，正文豁免）。
CLAIM_PATTERNS: dict[str, tuple[re.Pattern[str], str]] = {
    "modules": (
        re.compile(r"(\d+)\s*个?\s*Maven\s*模块|(\d+)\s*模块解耦"),
        "easyorange-backend/pom.xml 的 <module> 数",
    ),
    "ports": (re.compile(r"(\d+)\s*个?\s*Port\b"), "main 源码 `interface *Port` 数"),
    "adrs": (re.compile(r"(\d+)\s*[条个]\s*ADR\b"), "doc/adr/ 下 NNNN-*.md（排除 0000-template）"),
    "consumers": (
        re.compile(r"(\d+)\s*个?\s*(?:事件)?消费者"),
        "main 源码 @RabbitListener 引用的业务队列常量数（不含 DlqAnomalyListener）",
    ),
    "tables": (re.compile(r"MySQL\s*·\s*(\d+)\s*表"), "全部 V*.sql 的 CREATE TABLE − DROP TABLE"),
    "archunit_rules": (
        re.compile(r"(\d+)\s*条\s*(?:@ArchTest|ArchUnit)\b|(\d+)\s*条\s*规则\b"),
        "ArchitectureRulesTest 的 @ArchTest 数",
    ),
    # Prompt 模板数漂移过一轮（同一文件一处写 6、另一处写 8），改 prompt 时最容易忘同步。
    # 模式只认「N 个（YAML）模板」与「Prompt N 个 YAML」两种既有写法；「10 个 YAML 配置属性」
    # 这类同形异义被 `模板` 挡住。刻意漏检：`YAML 模板（**11 个**：…）` 数字在词后。
    "prompt_templates": (
        re.compile(r"(\d+)\s*个\s*(?:YAML\s*)?模板|Prompt\s*(\d+)\s*个\s*YAML"),
        "easyorange-ai/src/main/resources/prompts/*.yml 数",
    ),
    # 用例数无法静态算（it.each 会展开、Playwright 与 Vitest 分流），只校验文件数：
    # Vitest 报的 "Test Files N passed" == src 下 *.test.ts(x) 的数量（tests/e2e/*.spec.ts 属 Playwright）。
    # 模式只认「N 文件 / M 用例」与「…，N 文件）」两种既有写法，避免误伤无关的「N 文件」。
    "frontend_test_files": (
        re.compile(r"(\d+)\s*文件(?=\s*/\s*[\d,]+\s*用例|\s*[)）])"),
        "easyorange-frontend/src 下 *.test.ts(x) 数",
    ),
}


def _java_sources() -> list[Path]:
    return [
        p
        for p in BACKEND.rglob("*.java")
        if "target" not in p.parts and "test" not in p.relative_to(BACKEND).parts
    ]


def code_facts() -> dict[str, int]:
    facts: dict[str, int] = {}

    root_pom = BACKEND / "pom.xml"
    facts["modules"] = len(re.findall(r"<module>", root_pom.read_text(encoding="utf-8")))

    port_re = re.compile(r"^\s*(?:public\s+)?interface\s+\w*Port\b", re.MULTILINE)
    facts["ports"] = sum(len(port_re.findall(p.read_text(encoding="utf-8"))) for p in _java_sources())

    # 排除 0000-template.md：它是模板不是决策记录
    facts["adrs"] = len(
        [p for p in (ROOT / "doc/adr").glob("[0-9]*.md") if "template" not in p.name]
    )

    # 只数 @RabbitListener 引用的业务队列常量：DlqAnomalyListener 一人监听全部 DLQ 队列、
    # 不是业务消费者，计入会让「12 个消费者」这条永远对不上。
    queue_re = re.compile(r"@RabbitListener\([\s\S]{0,200}?queues\s*=\s*RabbitMQConfig\.(\w+)")
    queues: set[str] = set()
    for p in _java_sources():
        queues.update(queue_re.findall(p.read_text(encoding="utf-8")))
    facts["consumers"] = len(queues)

    creates = drops = 0
    for sql in sorted(MIGRATION.glob("V*.sql")):
        text = sql.read_text(encoding="utf-8")
        creates += len(re.findall(r"CREATE\s+TABLE", text, re.IGNORECASE))
        drops += len(re.findall(r"DROP\s+TABLE", text, re.IGNORECASE))
    facts["tables"] = creates - drops

    arch_re = re.compile(r"^\s*@ArchTest\s*$", re.MULTILINE)
    facts["archunit_rules"] = len(arch_re.findall(ARCH_TEST.read_text(encoding="utf-8")))

    prompts_dir = BACKEND / "easyorange-ai/src/main/resources/prompts"
    facts["prompt_templates"] = len(list(prompts_dir.glob("*.yml")))

    frontend_src = ROOT / "easyorange-frontend/src"
    facts["frontend_test_files"] = (
        len(list(frontend_src.rglob("*.test.ts"))) + len(list(frontend_src.rglob("*.test.tsx")))
        if frontend_src.exists()
        else 0
    )

    return facts


# ADR 里标注「这是历史口径」的措辞：ADR 的职责之一就是记录数字的口径演变
# （模板规则 9：「已接受的 ADR 若实现细节漂移，不改正文，在顶部加现状更新横幅」），
# 所以横幅里出现「决策时点 11 → 回升至 12」这类轨迹是**正确写法**，不该判漂移。
HISTORICAL_MARKERS = ("决策时点", "历史", "轨迹", "正文保留", "收敛为", "回升至")


def is_adr_historical_line(path: Path, line: str) -> bool:
    """ADR 正文、以及带历史标记的横幅行（记录口径演变）豁免数值校验。"""
    rel = path.relative_to(ROOT)
    if not (rel.parts[:2] == ("doc", "adr") and re.match(r"\d{4}-", path.name)):
        return False
    if "现状更新" not in line:
        return True
    return any(marker in line for marker in HISTORICAL_MARKERS)


def main() -> int:
    facts = code_facts()
    drift: list[str] = []
    checked = 0

    for path in sorted(ROOT.rglob("*.md")):
        rel = path.relative_to(ROOT)
        if SKIP_DIRS.intersection(rel.parts[:-1]):
            continue
        for lineno, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
            if is_adr_historical_line(path, line):
                continue
            for key, (pattern, source) in CLAIM_PATTERNS.items():
                for match in pattern.finditer(line):
                    raw = next((g for g in match.groups() if g), None)
                    if raw is None:
                        continue
                    checked += 1
                    if int(raw) != facts[key]:
                        drift.append(
                            f"  {rel}:{lineno} 声称 {raw}（{key}），实际 {facts[key]}"
                            f" —— 「{match.group(0).strip()}」；权威来源：{source}"
                        )

    if drift:
        print("[metrics-drift] FAIL: 文档的结构计数与代码事实不一致：", file=sys.stderr)
        for item in sorted(set(drift)):
            print(item, file=sys.stderr)
        print(
            "\n代码事实：" + " / ".join(f"{k}={v}" for k, v in facts.items()) + "\n"
            "修法：改文档为实际值（数字单一来源见 doc/工程指标.md；ADR 正文豁免、只在『现状更新』"
            "横幅改口径）。跳过本次校验：SKIP=git-hooks。",
            file=sys.stderr,
        )
        return 1

    print(
        f"[metrics-drift] OK 校验 {checked} 处计数，全部一致："
        + " / ".join(f"{k}={v}" for k, v in facts.items())
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
