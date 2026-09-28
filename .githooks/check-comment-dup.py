#!/usr/bin/env python3
"""跨层级重复注释校验 —— 拦「类注释已写清，方法/行内注释又写一遍」。

背景：AGENTS.md「注释」一节的取舍判据是「删掉这行，读者对控制流 / 取舍会不会
退化成猜」。由此推出一条容易被绕开的细则——**类注释已覆盖的不在行内重复**。
这条比「不复述代码」隐蔽得多：单看行内注释通顺，只对照类注释才发现是第二遍。
实际清过一轮（PaymentCommandHandler / AuditLogAspect / OfflineMessageStoreService
等 12 个文件），注释行数没怎么降，读起来却少了一层回声。

判据：同一文件里，类级 javadoc 与其它注释（方法 javadoc / 行内 //）的最长公共
子串达到阈值即命中。

精度控制（缺了这层就是纯噪音，首版 22 处候选里 9 处是误报）：
  1. 重复片段须含 **≥6 个汉字**——枚举名 / 字段名 / 配置 key 撞词
     （`PARTIALLY_REFUNDED`、`remember_preference`、`dailyTokenLimit`）不是注释重复；
  2. `{@link}` / `{@code}` 是引用不是散文，剥掉再比对，否则拿标识符当命中。

白名单：词法判不准的**语义例外**（典型是「类注释讲契约，行内注释锚定字面量
含义」——`tryLock(timeout, -1, ...)` 上方那句）。按「文件 + 归一化注释文本」
匹配，不用行号，改动行号不会失效。用 `--emit` 生成待粘贴行。

用法：
    python3 .githooks/check-comment-dup.py           # 退出码 0=无新增重复 / 1=命中
    python3 .githooks/check-comment-dup.py --report  # 连白名单一起全列（人工巡检用）
    python3 .githooks/check-comment-dup.py --emit    # 输出白名单候选行（--report 的子集）
"""

from __future__ import annotations

import re
import sys
from difflib import SequenceMatcher
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
BACKEND = ROOT / "easyorange-backend"
ALLOWLIST = ROOT / ".githooks/comment-dup-allowlist.txt"

MIN_OVERLAP = 14   # 公共子串最少字符数，低于此视为措辞巧合
MIN_CJK = 6        # 重复片段最少汉字数，低于此视为标识符撞词而非散文重复

# {@link Foo} / {@code bar} 是引用不是散文，先剥掉，否则会拿标识符当重复命中
INLINE_TAG = re.compile(r"\{@(?:link|code|literal|value)\s+[^}]*\}")
# 标点与空白在比对时抹平：同一句话换个断句方式不该算重复
PUNCT = re.compile(r"[\s*_/\\{}()\[\]<>—－\-·、,，.。:：;；!！?？#\"'`|+=&%$@~^]")

BLOCK = re.compile(r"/\*\*(.*?)\*/", re.S)
LINE = re.compile(r"^[ \t]*//[ \t]?(.*)$")
TYPE_DECL = re.compile(
    r"^[ \t]*(?:@[A-Za-z].*\n[ \t]*)*"
    r"(?:public|final|abstract|sealed|non-sealed|static)*\s*"
    r"(?:class|interface|enum|record)\s+\w+",
    re.M,
)
CJK = re.compile(r"[一-鿿]")


def normalize(text: str) -> str:
    """归一化：剥引用标签、去标点空白。既是比对用的键，也是白名单的键。"""
    return PUNCT.sub("", INLINE_TAG.sub(" ", text))


def is_prose(fragment: str) -> bool:
    """重复片段得像中文散文才算是注释重复。"""
    return len(CJK.findall(fragment)) >= MIN_CJK


def class_doc_of(src: str) -> tuple[str, int] | None:
    """返回 (归一化后的类注释, 命中长度) —— 定位类型声明前最近的 javadoc 块。"""
    decl = TYPE_DECL.search(src)
    if not decl:
        return None
    blocks = list(BLOCK.finditer(src[: decl.start()]))
    if not blocks:
        return None
    body = blocks[-1].group(1)
    return normalize(body), len(normalize(body))


def scan_file(path: Path) -> list[dict]:
    """返回该文件里「类注释 vs 其它注释」的重叠命中。"""
    src = path.read_text(encoding="utf-8", errors="ignore")
    found = class_doc_of(src)
    if not found:
        return []
    doc, _ = found
    if len(doc) < 20:
        return []

    hits: list[dict] = []
    for m in BLOCK.finditer(src):
        body = normalize(m.group(1))
        if not body or body == doc:
            continue
        sm = SequenceMatcher(None, doc, body).find_longest_match()
        if sm.size >= MIN_OVERLAP and is_prose(doc[sm.a : sm.a + sm.size]):
            hits.append({
                "line": src.count("\n", 0, m.start()) + 1,
                "snippet": doc[sm.a : sm.a + sm.size],
                "size": sm.size,
                "key": body,
            })

    for i, line in enumerate(src.split("\n"), 1):
        lm = LINE.match(line)
        if not lm:
            continue
        body = normalize(lm.group(1))
        if len(body) < MIN_OVERLAP:
            continue
        sm = SequenceMatcher(None, doc, body).find_longest_match()
        if sm.size >= MIN_OVERLAP and is_prose(doc[sm.a : sm.a + sm.size]):
            hits.append({
                "line": i,
                "snippet": doc[sm.a : sm.a + sm.size],
                "size": sm.size,
                "key": body,
            })
    return hits


def load_allowlist() -> dict[str, str]:
    """{「相对路径|归一化注释文本」: 理由}。理由行以 # 起头，不参与匹配。"""
    entries: dict[str, str] = {}
    if not ALLOWLIST.exists():
        return entries
    for raw in ALLOWLIST.read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        key, _, reason = line.partition("  # ")
        entries[key.strip()] = reason.strip() or "（未写理由）"
    return entries


def java_sources() -> list[Path]:
    return sorted(BACKEND.glob("*/src/main/java/**/*.java"))


def main(argv: list[str]) -> int:
    report = "--report" in argv
    emit = "--emit" in argv

    allow = load_allowlist()
    # 白名单里已失效的条目（注释被改过或删过）顺手报出来，避免它悄悄变成免死金牌
    alive: set[tuple[str, str]] = set()
    violations: list[tuple[str, dict]] = []
    total_hits = 0

    for path in java_sources():
        rel = str(path.relative_to(ROOT))
        for hit in scan_file(path):
            total_hits += 1
            key = f"{rel}|{hit['key']}"
            if key in allow:
                alive.add((rel, hit["key"]))
                if report:
                    print(f"  白名单 {rel}:{hit['line']}  「{hit['snippet']}」  {allow[key]}")
                continue
            violations.append((rel, hit))
            if emit:
                print(f"{key}  # 写理由")

    stale = [k for k in allow if k.split("|", 1)[0] + "|" + k.split("|", 1)[1] not in {f"{a}|{b}" for a, b in alive}]

    if report and not violations:
        print(f"[comment-dup] 全仓 {total_hits} 处重叠，白名单收 {len(alive)} 处，无豁免外的重复")

    if violations:
        print("[comment-dup] FAIL: 方法/行内注释在复述类注释：", file=sys.stderr)
        for rel, hit in violations:
            print(f"  {rel}:{hit['line']}  重复 {hit['size']} 字  「{hit['snippet']}」", file=sys.stderr)
        print(
            "\n修法（三选一）：① 剪到只剩类注释没有的增量（首选）；"
            "② 确认属「类注释讲契约、行内锚定字面量」等语义例外 → 加白名单：\n"
            f"    python3 {Path(__file__).relative_to(ROOT)} --emit   # 打印待粘贴行，补上理由后追加到\n"
            f"    {ALLOWLIST.relative_to(ROOT)}\n"
            "跳过本次校验：SKIP=git-hooks。",
            file=sys.stderr,
        )
        return 1

    print(f"[comment-dup] OK 跨层级重复注释 0 处（全仓重叠 {total_hits} 处，白名单收 {len(alive)} 处）")
    if stale:
        print(f"[comment-dup] WARN 白名单有 {len(stale)} 条已失效，可清理：{ALLOWLIST.relative_to(ROOT)}")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
