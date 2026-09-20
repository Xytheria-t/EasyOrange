#!/usr/bin/env python3
"""版本漂移校验 — 文档声明的版本 vs 权威来源（pom / compose / 镜像 tag）。

背景：技术栈版本曾同时写在 25 个文件里，代码升级后文档没人改，出现 Redis 7.4 / RabbitMQ 3.13 /
Elasticsearch 8 / Flyway 11 一类过期声明（2026-09-15 修正）。约定收敛为：

  1. 大版本（Spring Boot 4 / Java 25）可以写在 README / AGENTS 等技术栈表里，它承载技术取舍；
  2. **精确版本只在 `doc/技术栈.md` 的版本表里写一处**，其余文档引用它；
  3. 本脚本校验那张表 —— 表与 pom/compose 不一致即视为漂移，提交时挡下。

**当前状态（2026-09-20）**：已从 pre-commit 摘除 —— 文档不再复刻版本号（`doc/技术栈.md` 只记选型与说明），
权威源直接是下面两处；等版本表回填时再挂回本脚本。

权威来源：`easyorange-backend/pom.xml`（依赖）+ `compose.yaml` / `infra/elasticsearch/Dockerfile`（运行时镜像）。

用法：
    python3 .githooks/check-version-drift.py          # 校验，退出码 0=一致 / 1=漂移

表中「版本」列为 `—` 或含 `x`（如 `7.0.x`、`4.0.x`）的行跳过校验（表示不锁精确版本）。
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
TABLE_DOC = ROOT / "doc/技术栈.md"
POM = ROOT / "easyorange-backend/pom.xml"
COMPOSE = ROOT / "compose.yaml"
ES_DOCKERFILE = ROOT / "infra/elasticsearch/Dockerfile"


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def parse_table() -> dict[str, str]:
    """技术栈.md 的版本表 → {技术: 版本}（只取 4 列表格行）。"""
    rows: dict[str, str] = {}
    for line in read(TABLE_DOC).splitlines():
        cells = [c.strip() for c in line.split("|")]
        # ['', 类别, 技术, 版本, 说明, ''] —— 至少 6 段才是完整表格行
        if len(cells) < 6 or not line.startswith("|"):
            continue
        _, _, tech, version = cells[0], cells[1], cells[2], cells[3]
        if not tech or tech in {"技术", "------"} or set(tech) <= {"-"}:
            continue
        rows[tech] = version
    return rows


def pom_property(name: str) -> str | None:
    m = re.search(rf"<{re.escape(name)}>([^<]+)</{re.escape(name)}>", read(POM))
    return m.group(1).strip() if m else None


def pom_parent_version() -> str | None:
    block = re.search(r"<parent>(.*?)</parent>", read(POM), re.S)
    if not block:
        return None
    m = re.search(r"<version>([^<]+)</version>", block.group(1))
    return m.group(1).strip() if m else None


def compose_image_version(service_image: str, strip_suffix: str = "") -> str | None:
    m = re.search(rf"image:\s*{re.escape(service_image)}:([^\s]+)", read(COMPOSE))
    if not m:
        return None
    tag = m.group(1)
    if strip_suffix and tag.endswith(strip_suffix):
        tag = tag[: -len(strip_suffix)]
    return tag


def es_version() -> str | None:
    m = re.search(r"^FROM\s+\S*elasticsearch:([0-9][0-9.]*)", read(ES_DOCKERFILE), re.M)
    return m.group(1) if m else None


def expected_versions() -> dict[str, tuple[str | None, str]]:
    """表里的技术名 → (权威版本, 权威来源描述)。"""
    return {
        "Java": (pom_property("java.version"), "pom <java.version>"),
        "Spring Boot": (pom_parent_version(), "pom <parent>"),
        "MyBatis-Plus": (pom_property("mybatis-plus.version"), "pom <mybatis-plus.version>"),
        "MySQL": (compose_image_version("mysql"), "compose mysql 镜像 tag"),
        "Redis": (compose_image_version("redis", "-alpine"), "compose redis 镜像 tag"),
        "Langfuse": (compose_image_version("langfuse/langfuse"), "compose langfuse-web 镜像 tag"),
        "RabbitMQ": (compose_image_version("rabbitmq", "-management"), "compose rabbitmq 镜像 tag"),
        "Elasticsearch": (es_version(), "infra/elasticsearch/Dockerfile FROM"),
        "Flyway": (pom_property("flyway.version"), "pom <flyway.version>"),
        "MapStruct": (pom_property("mapstruct.version"), "pom <mapstruct.version>"),
    }


def main() -> int:
    for path in (TABLE_DOC, POM, COMPOSE, ES_DOCKERFILE):
        if not path.exists():
            print(f"[version-drift] 缺少文件，跳过校验：{path.relative_to(ROOT)}")
            return 0

    table = parse_table()
    drift: list[str] = []
    checked = 0

    for tech, (authoritative, source) in expected_versions().items():
        declared = table.get(tech)
        if declared is None:
            continue  # 表里没有这一行，不校验
        if declared in {"—", "-", ""} or "x" in declared:
            continue  # 显式不锁精确版本
        if authoritative is None:
            drift.append(f"  {tech}: 表声明 {declared}，但未能从 {source} 读到版本（来源被改动？）")
            continue
        checked += 1
        if declared != authoritative:
            drift.append(f"  {tech}: 文档 {declared} ≠ {source} 的 {authoritative}")

    if drift:
        print("[version-drift] FAIL: 文档版本与权威来源不一致：", file=sys.stderr)
        for item in drift:
            print(item, file=sys.stderr)
        print(
            "\n修法：改 doc/技术栈.md 的版本表（唯一权威落点），或确认 pom/compose 是否漏改。\n"
            "跳过本次校验：SKIP=git-hooks。",
            file=sys.stderr,
        )
        return 1

    print(f"[version-drift] OK 已校验 {checked} 项，文档版本与 pom/compose 一致")
    return 0


if __name__ == "__main__":
    sys.exit(main())
