#!/usr/bin/env python3
"""TD-020 四组对照 repro：kNN 相似度阈值 × 顶层 query 的组合失效（标准库直连 ES）。

根因结论（走查曾记为「无低分阈值」，实为组合失效）由本脚本输出支撑：
  1. BM25 词面路对乱码 0 命中（词面挡不住时才轮到向量兜底）；
  2. kNN 无阈值 -> ANN 把候选池凑满 k（「乱码返全量」的真实机制）；
  3. kNN 有阈值 + 顶层 query（match_all 旧形态）-> 阈值被架空，query 侧命中
     照常返回 = 组合失效证据；
  4. kNN 有阈值、不挂顶层 query（现行 knn.filter 形态）-> 乱码 0 命中 = 修复生效。

查询向量取索引内任一文档 embedding 的逐分量取反：cos(-v, u) = -cos(v,u)，
对全库余弦恒为负，低于 0.5 门槛等价于「乱码查询」而无需 embedding API key。

用法：
    python3 scripts/repro-td-020-knn-threshold.py
    ES_URL=http://localhost:9200 INDEX=products python3 scripts/repro-td-020-knn-threshold.py

退出码：四组全符合预期 0；任一不符 1（g4>0 即阈值回归）；索引无向量/不可达 2。
"""
import json
import os
import sys
import urllib.error
import urllib.request

ES = os.environ.get("ES_URL", "http://localhost:9200").rstrip("/")
INDEX = os.environ.get("INDEX", "products")
K = 10
NUM_CANDIDATES = 100
SIMILARITY = 0.5
# 与索引词面零重叠（全辅音 + 奇数数字），fuzziness AUTO 的编辑距离也够不着真实商品词
GARBAGE = "zzqwxplkjhgf13579"


def es(path, body):
    data = json.dumps(body).encode()
    req = urllib.request.Request(ES + path, data=data, headers={"Content-Type": "application/json"})
    try:
        with urllib.request.urlopen(req, timeout=10) as resp:
            return json.loads(resp.read())
    except (urllib.error.URLError, OSError) as e:
        print(f"ES 不可达（{ES}{path}）：{e}", file=sys.stderr)
        sys.exit(2)


def search(body):
    return es(f"/{INDEX}/_search", body)


def main():
    count = es(f"/{INDEX}/_count", {})["count"]
    print(f"index={INDEX}  corpus={count}  es={ES}")
    if count == 0:
        print("索引为空：先起 dev 栈并灌入商品（含 nameEmbedding）", file=sys.stderr)
        sys.exit(2)

    doc = search({"size": 1, "_source": ["nameEmbedding"]})
    source = (doc["hits"]["hits"] or [{}])[0].get("_source", {})
    vec = source.get("nameEmbedding")
    if not vec:
        print("索引文档无 nameEmbedding：先跑向量化重建（无 embedding 的对照无意义）", file=sys.stderr)
        sys.exit(2)
    low_vec = [-x for x in vec]  # 全库余弦为负的「乱码等价」查询向量

    knn_base = {"field": "nameEmbedding", "query_vector": low_vec, "k": K, "num_candidates": NUM_CANDIDATES}

    groups = [
        (
            "g1 BM25 词面 · 乱码",
            {
                "size": K,
                "query": {
                    "multi_match": {
                        "query": GARBAGE,
                        "type": "best_fields",
                        "fuzziness": "AUTO",
                        "fields": ["name^3", "description"],
                    }
                },
            },
        ),
        ("g2 kNN · 无阈值", {"size": K, "knn": knn_base}),
        (
            "g3 kNN · 阈值0.5 + 顶层query(旧形态)",
            {"size": K, "knn": {**knn_base, "similarity": SIMILARITY}, "query": {"match_all": {}}},
        ),
        ("g4 kNN · 阈值0.5 无顶层query(现行)", {"size": K, "knn": {**knn_base, "similarity": SIMILARITY}}),
    ]

    results = []
    for name, body in groups:
        resp = search(body)
        total = resp["hits"]["total"]["value"]
        top = resp["hits"]["hits"][0]["_score"] if resp["hits"]["hits"] else None
        results.append((name, total, top))

    expected_g2 = min(K, count)
    checks = [
        results[0][1] == 0,  # g1：词面挡乱码
        results[1][1] == expected_g2,  # g2：无阈值 -> 池子凑满
        results[2][1] > 0,  # g3：组合失效 -> 阈值被架空
        results[3][1] == 0,  # g4：现行形态 -> 乱码 0 命中
    ]

    print(f"{'组':<38} {'hits':>5} {'topScore':>9}  预期")
    expectations = [f"== 0", f"== {expected_g2}", "> 0", "== 0"]
    for (name, total, top), ok, exp in zip(results, checks, expectations):
        score = "-" if top is None else f"{top:.3f}"
        print(f"{name:<38} {total:>5} {score:>9}  {exp} {'✓' if ok else '✗'}")

    print()
    print("结论：g3>0 且 g4==0 说明阈值失效是「顶层 query × similarity」的组合问题，")
    print("单独加阈值不迁 filter 不会修好；当前 knn.filter 形态（553c842b）乱码 0 命中。")
    if all(checks):
        print("四组全部符合预期。")
        return 0
    print("存在不符组：g4>0 即相似度阈值回归，先查 knnQuery 是否又挂了顶层 query。", file=sys.stderr)
    return 1


if __name__ == "__main__":
    sys.exit(main())
