#!/usr/bin/env python3
"""结构计数漂移校验与单点校准 — 校验「N 模块 / N Port / N ADR / N 消费者 / N 表 / N 条 ArchUnit 规则 / N 个 Prompt 模板 / N 前端测试文件 / N 条金标准集」与代码事实。

背景：这几类计数随代码频繁变动、靠人肉同步，已经漂移过一整轮 —— README 写 11 条 ADR（实际 12）、
根 AGENTS.md 写 11 个（实际 12）、面试文档写 10 个消费者（实际 12）、mermaid 写 32 表（实际 33）、
`ArchitectureRulesTest` 被写成 10 条（实际 12，文件 javadoc 自己写着 12）、Prompt 模板一处写 6
（实际 8，同一文件另一处写对）。评审/面试当场可查，一个数字错了会连带质疑其余全部数字。

单点约定（工单-易变数字治理）：
  1. 上述计数**只在 `doc/工程指标.md` 的「结构计数」区块维护**（唯一落点）；其余文档一律写
     `[结构计数](…)` 链接或定性表述（「Port 接口编译期隔离」），不再复制数字；
  2. 该区块由 `--fix` 从代码事实重算回写（幂等），提交时由 pre-commit 校验；
  3. **反向检查**：计数出现在该区块之外即报错并指出 `文件:行` —— **无例外**（`modules` 亦不例外：
     数字只在区块里，正文写「Maven 多模块」）。唯一按值判定的是裸写「N 模块」：命中值等于模块总数
     才算重复落点，`4 模块`（CQRS 作用域）这类子集口径放过（见 `BARE_MODULES`）。

计数**以代码为准**（每项都给出推导方式），文档必须与之一致。**ADR 正文参与校验**：ADR 规则 4
「正文即现状」要求实现细节随代码演进直接改正文（不写「现状更新」横幅），所以 `doc/adr/NNNN-*.md`
与其它文档同标准；唯二豁免：`0000-template.md`（模板，不是决策记录）与状态为「已替代 / 部分已替代」
的 ADR（正文描述的是当时的决策，本身即历史记录）。

用法：
    python3 .githooks/check-metrics-drift.py          # 退出码 0=一致 / 1=漂移或计数散落
    python3 .githooks/check-metrics-drift.py --fix    # 重算并原地回写单点区块（幂等）
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

SSOT = ROOT / "doc/工程指标.md"
BLOCK_ANCHOR = '<a name="结构计数"></a>'
BLOCK_HEADING = "## 结构计数（脚本生成，勿手改）"
FIX_CMD = "python3 .githooks/check-metrics-drift.py --fix"

SKIP_DIRS = {".git", ".zcode", "node_modules", "target", "dist", ".venv", "archunit_store"}

# Markdown 加粗：`**49**` / `**49 个**` / `49 个` 三种断法都要能认 —— 历史漏检正是从这儿来的
# （`doc/工程指标.md` 的表格把数字与关键词分到两个单元，旧正则只认「N 个 Port」相邻写法）。
_B = r"\*{0,2}"


def _cell(label: str) -> str:
    """表格写法：标签单元含关键词、数值单元是数字（`| 跨模块 Port 接口数 | **48** |`）。"""
    return rf"\|\s*[^|\n]*{label}[^|\n]*\|\s*{_B}(\d[\d,]*){_B}\s*\|"


# 裸写「N 模块」（不带 Maven）：`4 模块`（CQRS 作用域）、`8 模块 domain`（域覆盖率口径）在词法上
# 与「又写了一遍模块总数」没有区别，只能**按值判定** —— 命中值等于模块总数才算重复落点，小于总数
# 的当子集口径放过。这是唯一一处按值判定的模式，不算独立类别、不进单点区块。
BARE_MODULES = re.compile(rf"{_B}(\d+){_B}\s*个?\s*模块")

# (名称, 匹配「在说总数」的写法, 期望值来源)
# 刻意写窄：模式宁可漏检也不误报 —— 会误报的检查器最终会被 SKIP 掉，比没有更糟。
# 已知需回避的同形异义：`4 模块`（CQRS 作用域，见 BARE_MODULES）、`V1 的 26 张表`（单脚本表数，
# 非总数，故 `N 张表` 不设模式）、`11 个 DLQ`（ADR-0005 决策时点口径，正文豁免）。
# 已知漏检：词表之外的同义改写（如 `十一模块` 这种中文数字写法）。
CLAIM_PATTERNS: dict[str, tuple[re.Pattern[str], str]] = {
    "modules": (
        re.compile(
            rf"{_B}(\d+){_B}\s*个?\s*Maven\s*模块|{_B}(\d+){_B}\s*模块解耦|{_cell('模块数')}"
        ),
        "easyorange-backend/pom.xml 的 <module> 数",
    ),
    "ports": (re.compile(rf"{_B}(\d+){_B}\s*个?\s*Port\b|{_cell('Port')}"), "main 源码 `interface *Port` 数"),
    "adrs": (
        # `ADR 决策记录（N 个…）` 是「关键词在前」的改写，`决策 N 篇` 是 ADR 索引里的写法。
        re.compile(
            rf"{_B}(\d+){_B}\s*[条个]\s*ADR\b|ADR\s*决策记录\s*（\s*{_B}(\d+){_B}\s*个|决策\s*{_B}(\d+){_B}\s*篇"
        ),
        "doc/adr/ 下 NNNN-*.md（排除 0000-template）",
    ),
    "consumers": (
        # 「N 个消费者」的三种同义改写也拦：「独立下游」「DLQ 队列」「consumer group」——
        # 实测过 `11 个 DLQ 队列` / `11 个独立下游` 这类换词写法在正文里长期漂移。
        re.compile(
            rf"{_B}(\d+){_B}\s*个?\s*(?:事件)?消费者"
            rf"|{_B}(\d+){_B}\s*个?\s*(?:独立)?下游"
            rf"|{_B}(\d+){_B}\s*个?\s*DLQ\s*队列"
            rf"|{_cell('消费者')}"
        ),
        "main 源码 @RabbitListener 引用的业务队列常量数（不含 DlqAnomalyListener）",
    ),
    "tables": (
        re.compile(rf"MySQL\s*·\s*{_B}(\d+){_B}\s*表|{_cell('数据库表数')}"),
        "全部 V*.sql 的 CREATE TABLE − DROP TABLE",
    ),
    "archunit_rules": (
        # `12 条规则` / `12 条 @ArchTest` / `ArchUnit 12 条…`（后者后面接什么词都算在说规则数）
        re.compile(
            rf"{_B}(\d+){_B}\s*条\*{{0,2}}\s*[`（(]?\s*(?:@ArchTest|ArchUnit|规则)|ArchUnit\s*{_B}(\d+){_B}\s*条"
        ),
        "ArchitectureRulesTest 的 @ArchTest 数",
    ),
    # Prompt 模板数漂移过一轮（同一文件一处写 6、另一处写 8），改 prompt 时最容易忘同步。
    # `模板（**5 个**：…）` 这种「关键词在数字前」的写法也认；「10 个 YAML 配置属性」被 `模板` 挡住。
    "prompt_templates": (
        re.compile(
            rf"{_B}(\d+){_B}\s*个\s*(?:YAML\s*)?模板"
            rf"|模板\s*（\s*{_B}(\d+){_B}\s*个"
            rf"|{_B}(\d+){_B}\s*个\s*[Pp]rompt\b"
            rf"|Prompt\s*{_B}(\d+){_B}\s*个\s*YAML"
        ),
        "easyorange-ai/src/main/resources/prompts/*.yml 数",
    ),
    # 用例数无法静态算（it.each 会展开、Playwright 与 Vitest 分流），只校验文件数：
    # Vitest 报的 "Test Files N passed" == src 下 *.test.ts(x) 的数量（tests/e2e/*.spec.ts 属 Playwright）。
    # 只认「N 文件」后跟用例数或右括号的写法（`**106** 文件）` / `108 文件 / 975 用例`），
    # 避开无关的「N 文件」（如 `Portal/Dialog 使用 106 处`）。
    "frontend_test_files": (
        re.compile(
            rf"{_B}(\d+){_B}\s*文件(?=\s*/\s*[\d,]+\s*用例|\s*[)）])|{_cell('前端测试文件数')}"
        ),
        "easyorange-frontend/src 下 *.test.ts(x) 数",
    ),
    # 金标准集条数：README / 工程指标 / 集成文档 / 面试脚本多处独立陈述，语料与用例集刚扩过一轮
    # （5 篇 → 23 篇语料、用例集重编），是最容易整体漂移的一组数字。
    # 反向的「金标准集 N 条」必须后面紧跟分隔符或行尾才算总数 —— 否则会误伤
    # 「金标准集 15 条**检索**用例」这类「全集里的子集」写法（实际发生过：全集 = 生成 + 检索）。
    "golden_set_cases": (
        re.compile(
            rf"{_B}(\d+){_B}\s*条\s*金标准集|金标准集\s*{_B}(\d+){_B}\s*条(?=\s*[（(，,、。+｜|]|\s*$)"
        ),
        "eval/golden-set.yaml 的 `- id:` 条目数",
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
    # 不是业务消费者，计入会让「N 个消费者」这条永远对不上。
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

    golden = BACKEND / "easyorange-ai/src/main/resources/eval/golden-set.yaml"
    facts["golden_set_cases"] = (
        len(re.findall(r"^\s+- id:", golden.read_text(encoding="utf-8"), re.MULTILINE))
        if golden.exists()
        else 0
    )

    return facts


def is_exempt_file(path: Path) -> bool:
    """模板文件、「已替代 / 部分已替代」的 ADR、工单不参与校验。

    已替代篇记的是当时的决策与代码形态，正文本就与现状不符；现役 ADR 按规则 4 改正文，故全量校验。
    工单（`doc/工单-*.md`）是交接文档，正文会引用迁移前后的示例数字（`49 Port` / `106 文件`），
    拿它当「文档声称」来校验只会逼着改写交接记录。
    """
    if path.name.endswith("-template.md") or path.name.startswith("工单-"):
        return True
    if not path.name[:4].isdigit():
        return False
    head = path.read_text(encoding="utf-8")[:600]
    match = re.search(r"^- \*\*状态\*\*：(.+)$", head, re.MULTILINE)
    return bool(match and "替代" in match.group(1))


def block_region(text: str) -> tuple[int, int] | None:
    """单点区块的行区间（0-based 半开），从锚点到下一个二级标题；找不到返回 None。"""
    lines = text.splitlines()
    start = next((i for i, line in enumerate(lines) if line.strip() == BLOCK_HEADING), None)
    if start is None:
        return None
    if start and lines[start - 1].strip() == BLOCK_ANCHOR:
        start -= 1
    end = next((i for i in range(start + 1, len(lines)) if lines[i].startswith("## ")), len(lines))
    return start, end


def read_block_facts() -> dict[str, int] | None:
    """读单点区块里 fenced code block 的 `名称: 数字`；缺失或残缺返回 None。"""
    if not SSOT.exists():
        return None
    text = SSOT.read_text(encoding="utf-8")
    region = block_region(text)
    if region is None:
        return None
    start, end = region
    body = "\n".join(text.splitlines()[start:end])
    fence = re.search(r"^```[a-z]*\n(.*?)^```", body, re.MULTILINE | re.DOTALL)
    if not fence:
        return None
    found = dict(re.findall(r"^([a-z_]+):\s*(\d+)$", fence.group(1), re.MULTILINE))
    return {k: int(v) for k, v in found.items()}


def render_block(facts: dict[str, int]) -> str:
    """单点区块里 code block 的规范内容（键序即 CLAIM_PATTERNS 的定义顺序，保证 --fix 幂等）。"""
    return "\n".join(f"{key}: {facts[key]}" for key in CLAIM_PATTERNS)


def render_inline(facts: dict[str, int]) -> str:
    """一行式计数摘要（日志用）。"""
    return " / ".join(f"{key}={facts[key]}" for key in CLAIM_PATTERNS)


def scan_claims() -> tuple[list[tuple[str, int, str, str, str, bool]], int]:
    """扫描全仓 md 的结构计数写法，返回 ([(相对路径, 行号, 名称, 数字, 命中文本, 按值判定)], 检查处数)。

    单点区块自身与豁免文件跳过 —— 区块是唯一允许出现这些数字的地方，其余位置由调用方判违规。
    末位 `按值判定` 为真时（只有裸写「N 模块」），命中值等于总数才算违规，小于总数当子集口径放过。
    """
    hits: list[tuple[str, int, str, str, str, bool]] = []
    checked = 0
    for path in sorted(ROOT.rglob("*.md")):
        rel = path.relative_to(ROOT)
        if SKIP_DIRS.intersection(rel.parts[:-1]) or is_exempt_file(path):
            continue
        text = path.read_text(encoding="utf-8")
        region = block_region(text) if path == SSOT else None
        skip = set(range(*region)) if region else set()
        for lineno, line in enumerate(text.splitlines(), 1):
            if lineno - 1 in skip:
                continue
            found = [
                (key, match)
                for key, (pattern, _source) in CLAIM_PATTERNS.items()
                for match in pattern.finditer(line)
            ]
            found += [("modules", match) for match in BARE_MODULES.finditer(line)]
            for key, match in found:
                raw = next((g for g in match.groups() if g), None)
                if raw is None:
                    continue
                checked += 1
                tolerant = match.re is BARE_MODULES
                hits.append((str(rel), lineno, key, raw.replace(",", ""), match.group(0).strip(), tolerant))
    return hits, checked


def main() -> int:
    facts = code_facts()
    fix = "--fix" in sys.argv[1:]

    if fix:
        current = read_block_facts()
        if current is None:
            print(
                f"[metrics-drift] FAIL: 读不到 {SSOT.relative_to(ROOT)} 的「{BLOCK_HEADING}」区块"
                f"（或区块内缺 fenced code block）——请先手工补出该节与锚点 {BLOCK_ANCHOR}。",
                file=sys.stderr,
            )
            return 1
        if current == facts:
            print(f"[metrics-drift] OK 单点区块已是最新：{render_inline(facts)}")
            return 0
        region = block_region(SSOT.read_text(encoding="utf-8"))
        assert region is not None
        start, end = region
        lines = SSOT.read_text(encoding="utf-8").splitlines(keepends=True)
        body = "".join(lines[start:end])
        patched = re.sub(
            r"^```[a-z]*\n.*?^```",
            f"```text\n{render_block(facts)}\n```",
            body,
            count=1,
            flags=re.MULTILINE | re.DOTALL,
        )
        SSOT.write_text("".join(lines[:start]) + patched + "".join(lines[end:]), encoding="utf-8")
        print(f"[metrics-drift] FIXED 已回写单点区块：{render_inline(facts)}")
        return 0

    drift: list[str] = []
    scattered: list[str] = []

    block = read_block_facts()
    if block is None:
        drift.append(
            f"  {SSOT.relative_to(ROOT)} 找不到「{BLOCK_HEADING}」区块（或区块内缺 fenced code block）"
            f" —— 这 9 类结构计数的唯一落点就是它；修：{FIX_CMD}"
        )
    else:
        for key, value in facts.items():
            if key not in block:
                drift.append(f"  {SSOT.relative_to(ROOT)} 单点区块缺 `{key}`（实际 {value}）；修：{FIX_CMD}")
            elif block[key] != value:
                drift.append(
                    f"  {SSOT.relative_to(ROOT)} 单点区块写 {key}: {block[key]}，实际 {value}；修：{FIX_CMD}"
                )

    hits, checked = scan_claims()
    for rel, lineno, key, raw, text, tolerant in hits:
        if tolerant and int(raw) != facts[key]:
            # 子集口径（`4 模块` CQRS 作用域 / `8 模块 domain`）：只在等于总数时才算重复落点
            continue
        scattered.append(
            f"  {rel}:{lineno} 出现 {key} 计数「{text}」"
            f"（实际 {facts[key]}）—— 结构计数只在 {SSOT.relative_to(ROOT)} 的「结构计数」区块维护"
        )

    if drift or scattered:
        print("[metrics-drift] FAIL: 结构计数与代码事实不一致 / 散落在单点区块之外：", file=sys.stderr)
        for item in sorted(set(drift + scattered)):
            print(item, file=sys.stderr)
        print(
            "\n代码事实：" + " / ".join(f"{k}={v}" for k, v in facts.items()) + "\n"
            f"修法：① 单点区块漂移 → {FIX_CMD}（自动回写）；"
            f"② 区块之外出现计数 → 改成链接 {SSOT.relative_to(ROOT)}#结构计数 或定性表述"
            "（ADR 正文按规则 4 直接改）。跳过本次校验：SKIP=git-hooks。",
            file=sys.stderr,
        )
        return 1

    print(
        f"[metrics-drift] OK 单点区块与代码事实一致（{render_inline(facts)}）；"
        f"区块外校验 {checked} 处计数，无散落"
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
