#!/usr/bin/env python3
"""校验跨模块依赖 optional 标记（TD-011 缓解措施）。

规则：除 easyorange-application（组合根，依赖全量领域模块）外，任意模块依赖
其他领域模块（framework/common 两个基础模块除外）必须声明 <optional>true</optional>，
防止领域依赖被下游模块意外传递。

用法：python3 .github/scripts/check-optional-deps.py [模块根目录，默认当前目录]
"""

import sys
import xml.etree.ElementTree as ET
from pathlib import Path

NS = {"m": "http://maven.apache.org/POM/4.0.0"}
BASE_MODULES = {"easyorange-framework", "easyorange-common"}


def check_module(pom: Path) -> list[str]:
    tree = ET.parse(pom)
    bad = []
    for dep in tree.findall(".//m:dependency", NS):
        aid = dep.findtext("m:artifactId", "", NS)
        if not aid.startswith("easyorange-") or aid in BASE_MODULES:
            continue
        if dep.findtext("m:optional", "false", NS) != "true":
            bad.append(f"{pom.parent.name} → {aid} 缺少 <optional>true</optional>")
    return bad


def main() -> int:
    root = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(".")
    bad = []
    for pom in sorted(root.glob("easyorange-*/pom.xml")):
        if pom.parent.name == "easyorange-application":
            continue
        bad += check_module(pom)
    if bad:
        print("跨模块依赖 optional 校验失败：")
        print("\n".join(f"  - {item}" for item in bad))
        return 1
    print("OK：所有跨模块领域依赖均已声明 <optional>true</optional>")
    return 0


if __name__ == "__main__":
    sys.exit(main())
