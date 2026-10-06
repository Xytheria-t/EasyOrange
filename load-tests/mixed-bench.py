#!/usr/bin/env python3
"""混合只读流量并发压测（标准库实现）

k6 的等价替代：k6 脚本见 product-list.js（canonical），本脚本按同一流量配比逐项对齐——
商品列表(60%) + 分类(20%) + 搜索(20%)、列表轮换 5 页、每轮迭代后 sleep 0.1s。
k6 二进制下载受限（GitHub 大文件被网络劫持）时用它拿同口径数字，理由与延迟口径同
search-bench.py。

用法：
    python3 mixed-bench.py [并发] [时长秒]
    python3 mixed-bench.py 50 30

输出：total / qps / fail_rate / p50 / p95 / p99 / max，并分端点列出；
      末尾按 product-list.js 的门禁判达标（p95<500ms、p99<1000ms、失败率<1%）。
"""
import json
import sys
import time
import urllib.parse
import urllib.request
from concurrent.futures import ThreadPoolExecutor

BASE = "http://localhost:8080"
CONCURRENCY = int(sys.argv[1]) if len(sys.argv) > 1 else 50
DURATION = int(sys.argv[2]) if len(sys.argv) > 2 else 30
SEARCH_KEYWORD = "手机"

# 失败判据比 k6 的 http_req_failed（仅看 HTTP 状态）更严：状态 200 且业务码 A0000 才算成功
def hit(url):
    t0 = time.perf_counter()
    try:
        with urllib.request.urlopen(url, timeout=10) as r:
            body = r.read()
            ok = r.status == 200 and json.loads(body).get("code") == "A0000"
    except Exception:
        ok = False
    return time.perf_counter() - t0, ok


def worker(vu, deadline, acc):
    """单 VU 循环：与 k6 的默认函数体同构（n = (iteration + vu) % 5）"""
    it = 0
    while time.perf_counter() < deadline:
        n = (it + vu) % 5
        legs = [("list", f"{BASE}/api/products?page={1 + (n % 5)}&size=12")]
        if n % 5 == 3:
            legs.append(("categories", f"{BASE}/api/products/categories"))
        elif n % 5 == 4:
            legs.append(("search", f"{BASE}/api/products/search?keyword={urllib.parse.quote(SEARCH_KEYWORD)}"))
        for name, url in legs:
            lat, ok = hit(url)
            acc[name].append((lat, ok))
        it += 1
        time.sleep(0.1)


def pct(vals, q):
    if not vals:
        return 0.0
    vals = sorted(vals)
    return vals[min(len(vals) - 1, int(len(vals) * q))] * 1000


def report(name, samples):
    total = len(samples)
    fails = sum(1 for _, ok in samples if not ok)
    lats = [lat for lat, _ in samples]
    fail_rate = fails / total * 100 if total else 0.0
    print(f"  {name:<11} n={total:<6} fail={fail_rate:5.2f}%  "
          f"p50={pct(lats, 0.50):6.1f}ms p95={pct(lats, 0.95):6.1f}ms "
          f"p99={pct(lats, 0.99):6.1f}ms max={max(lats) * 1000 if lats else 0:7.1f}ms")
    return fail_rate, pct(lats, 0.95), pct(lats, 0.99)


def main():
    acc = {"list": [], "categories": [], "search": []}
    deadline = time.perf_counter() + DURATION
    t0 = time.perf_counter()
    with ThreadPoolExecutor(max_workers=CONCURRENCY) as ex:
        list(ex.map(lambda vu: worker(vu, deadline, acc), range(CONCURRENCY)))
    elapsed = time.perf_counter() - t0

    all_samples = [s for v in acc.values() for s in v]
    total = len(all_samples)
    print(f"concurrency={CONCURRENCY} duration={DURATION}s "
          f"mix=每轮必打列表 + 20% 迭代带分类 + 20% 迭代带搜索（对齐 product-list.js）")
    print(f"total={total} qps={total / elapsed:.1f}")
    overall = report("全部", all_samples)
    print("  分端点：")
    for name in ("list", "categories", "search"):
        report(name, acc[name])

    fail_rate, p95, p99 = overall
    verdict = "达标" if fail_rate < 1 and p95 < 500 and p99 < 1000 else "未达标"
    print(f"  门禁（对齐 product-list.js）：失败率<1% / p95<500ms / p99<1000ms -> {verdict}")


if __name__ == "__main__":
    main()
