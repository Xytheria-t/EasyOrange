#!/usr/bin/env python3
"""注释预算校验 —— 拦「注释写成第二遍代码」与「注释长文」。

背景：AGENTS.md「注释」一节的判据是「删掉这行，读者对控制流 / 取舍会不会退化成
猜」。但判据靠自觉会漂：仓库实测主源码注释占 16.4%（6503/39760 行），长类注释
散文（单类 13~24 行）与端口接口上「每个方法一行 javadoc 复述方法名」是两大来源。
前一轮已用 `check-comment-dup.py` 拦「类注释已覆盖还再写一遍」，本脚本补上
**结构上限**——把「偏多是资产」的另一面（冗余是负债）变成可执行门禁。

判据（三条硬闸 + 一条提示）：
  1. **类级 javadoc 正文 ≤ 8 行**：超了就不是「做什么 / 取舍 / 边界」三句话的量，
     那是散文。长解释的落点是 `doc/interview/02` 与 ADR，不是启动类。
  2. **单文件注释行 ≤ 非注释非空代码行**：注释比代码多 = 第二遍实现。行内 `//`
     计入注释，其所在行同时计入代码。极小的值对象（代码 < 20 行）按 1:1 判不现实
     —— 三字段 record 写满契约就有 4~5 行注释，故给 `代码行 + 8` 的绝对余量，
     余量之外仍算超限（端口接口 / 枚举挂满复述型 javadoc 是这闸要抓的主要形态）。
  3. **注释内禁 TODO / FIXME**：不留死代码与 TODO 是 AGENTS.md 的口径，落地成闸。
  4. 提示（不拦）：注释里出现 `20xx-xx-xx` —— 实测数字与日期的权威落点是
     `doc/工程指标.md`；确有承载实测数字的就地注释允许，故只提示不拦。

计数口径：`//`、`/*`、`*`、`*/` 开头的行算注释；行尾 `//` 注释同样计入。
类级 javadoc 的「正文行」= 去掉 `/**`、`*/`、空行、纯标签（`@param` 等）与
纯标记（`<p>`、`<ul>`、`<b>`）后仍有实质文字的行。测试源码不设闸（断言说明本就该
写清），只统计 `src/main/java`。

用法：
    python3 .githooks/check-comment-budget.py           # 退出码 0=在预算内 / 1=超限
    python3 .githooks/check-comment-budget.py --report  # 连分模块密度与 TOP 榜全列
"""

from __future__ import annotations

import re
import sys
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
BACKEND = ROOT / "easyorange-backend"

MAX_CLASS_DOC_LINES = 8      # 类级 javadoc 正文行上限
MAX_RATIO = 1.0              # 单文件 注释行 / 代码行 上限
SMALL_FILE_LINES = 20        # 代码行少于此的文件改用绝对余量
SMALL_FILE_SLACK = 8         # 小文件的注释余量（代码行 + 8）
TOP_N = 12                   # 报告里列出的超限文件数

COMMENT_START = ("//", "/*", "*", "*/")
TRAILING = re.compile(r"\S\s+//")
TYPE_DECL = re.compile(r"^(?:@\w[\w.]*\s*)*(?:public |final |abstract |sealed |non-sealed |static )*"
                       r"(?:class|interface|enum|record)\b")
TAG_LINE = re.compile(r"^@\w+")
MARKUP_ONLY = re.compile(r"^(?:</?(?:p|ul|ol|li|b|i|br|code|pre)\s*/?>\s*)+$", re.I)
DATE = re.compile(r"20\d\d-\d\d-\d\d")
DEBT = re.compile(r"\b(?:TODO|FIXME|XXX)\b")

# 类注释块之后的注解行（@Component 之类）要让路，才认得出紧跟的类型声明
ANNOTATION = re.compile(r"^@[\w.]+(\(.*\))?\s*$")


def strip_markup(text: str) -> str:
    text = re.sub(r"</?(?:p|ul|ol|li|b|i|br|pre)\s*/?>", " ", text, flags=re.I)
    return re.sub(r"\{@[^}]*\}", " ", text)


def prose_lines(block: list[str]) -> int:
    """类 javadoc 的正文行数：空行、纯标签行、纯标记行都不计。"""
    n = 0
    for raw in block:
        s = raw.strip()
        if s in ("/**", "*/", "*", "/**/", ""):
            continue
        if s.startswith("*"):
            s = s.lstrip("*").strip()
        if not s or TAG_LINE.match(s) or MARKUP_ONLY.match(s):
            continue
        if len(re.sub(r"\s", "", strip_markup(s))) >= 6:
            n += 1
    return n


def scan(path: Path) -> dict:
    src = path.read_text(encoding="utf-8")
    lines = src.split("\n")
    comments = codes = 0
    comment_text: list[str] = []
    for line in lines:
        s = line.strip()
        if s.startswith(COMMENT_START):
            comments += 1
            comment_text.append(s)
        elif TRAILING.search(line):
            comments += 1
            codes += 1
            comment_text.append(line.split("//", 1)[1])
        elif s:
            codes += 1

    class_doc = 0
    i = 0
    while i < len(lines):
        if lines[i].strip() == "/**":
            j = i
            while j < len(lines) and "*/" not in lines[j]:
                j += 1
            if j < len(lines):
                k = j + 1
                while k < len(lines) and (not lines[k].strip() or ANNOTATION.match(lines[k].strip())):
                    k += 1
                if k < len(lines) and TYPE_DECL.match(lines[k].strip()):
                    class_doc = max(class_doc, prose_lines(lines[i : j + 1]))
            i = j + 1
        i += 1

    return {
        "comments": comments,
        "codes": codes,
        "class_doc": class_doc,
        "dates": [s for s in comment_text if DATE.search(s)],
        "debt": sorted({m.group(0) for s in comment_text for m in DEBT.finditer(s)}),
    }


def java_sources() -> list[Path]:
    return sorted(BACKEND.glob("easyorange-*/src/main/java/**/*.java"))


def main(argv: list[str]) -> int:
    report = "--report" in argv
    files = java_sources()
    module_totals: dict[str, list[int]] = defaultdict(lambda: [0, 0])
    over_doc: list[tuple[int, Path]] = []
    over_ratio: list[tuple[float, int, int, Path]] = []
    debt: list[tuple[Path, list[str]]] = []
    dated: list[tuple[Path, int]] = []
    max_doc = 0
    total_c = total_k = 0

    for f in files:
        r = scan(f)
        mod = f.relative_to(BACKEND).parts[0]
        module_totals[mod][0] += r["comments"]
        module_totals[mod][1] += r["codes"]
        total_c += r["comments"]
        total_k += r["codes"]
        max_doc = max(max_doc, r["class_doc"])
        if r["class_doc"] > MAX_CLASS_DOC_LINES:
            over_doc.append((r["class_doc"], f))
        codes = r["codes"]
        limit = codes if codes >= SMALL_FILE_LINES else codes + SMALL_FILE_SLACK
        if r["comments"] > limit and codes:
            over_ratio.append((r["comments"] / codes, r["comments"], codes, f))
        if r["debt"]:
            debt.append((f, r["debt"]))
        if r["dates"]:
            dated.append((f, len(r["dates"])))

    if report:
        for mod in sorted(module_totals, key=lambda m: -module_totals[m][0]):
            c, k = module_totals[mod]
            print(f"  {mod:24} 注释 {c:5d} / 代码 {k:5d} = {100.0 * c / k:5.1f}%")
    print(
        f"[comment-budget] 统计 {len(files)} 个主源码文件：注释 {total_c} / 代码 {total_k} "
        f"= {100.0 * total_c / total_k:.1f}%，类 javadoc 最长 {max_doc} 行"
    )

    failed = False
    if over_doc:
        failed = True
        print("[comment-budget] FAIL: 类 javadoc 正文超长（散文该去 doc/interview/02 或 ADR）：", file=sys.stderr)
        for n, f in sorted(over_doc, reverse=True)[:TOP_N]:
            print(f"  {f.relative_to(ROOT)}  {n} 行 > {MAX_CLASS_DOC_LINES}", file=sys.stderr)
    if over_ratio:
        failed = True
        print("[comment-budget] FAIL: 注释行多于代码行（第二遍实现）：", file=sys.stderr)
        for ratio, c, k, f in sorted(over_ratio, reverse=True)[:TOP_N]:
            print(f"  {f.relative_to(ROOT)}  注释 {c} / 代码 {k} = {ratio:.2f}", file=sys.stderr)
    if debt:
        failed = True
        print("[comment-budget] FAIL: 注释里留了 TODO / FIXME：", file=sys.stderr)
        for f, tags in debt[:TOP_N]:
            print(f"  {f.relative_to(ROOT)}  {', '.join(tags)}", file=sys.stderr)
    if dated and report:
        print(f"[comment-budget] 提示：{len(dated)} 个文件的注释带日期（实测数字的落点是 doc/工程指标.md）：")
        for f, n in dated[:TOP_N]:
            print(f"  {f.relative_to(ROOT)}  {n} 处")
    if failed:
        return 1
    print(f"[comment-budget] OK 注释预算内（类 javadoc ≤ {MAX_CLASS_DOC_LINES} 行 / 单文件注释 ≤ 代码行 / 无 TODO）")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
