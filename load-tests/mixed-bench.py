#!/usr/bin/env python3
"""混合只读流量压测（列表 60% / 分类 20% / 搜索 20%，对齐 load-tests/product-list.js 配比）

k6 的补充：k6 二进制下载受限（GitHub 大文件被网络劫持）时的等价替代；
延迟口径与 k6 一致（p50/p95/p99/QPS），用于 ES / DB 两种检索模式整体对比。

用法：
    python3 mixed-bench.py <并发> <时长秒>
    python3 mixed-bench.py 50 30
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

def hit(i):
    t0 = time.perf_counter()
    try:
        n = i % 5
        if n < 3:
            url = f"{BASE}/api/products?page={1 + (i % 5)}&size=12"
        elif n == 3:
            url = f"{BASE}/api/products/categories"
        else:
            url = f"{BASE}/api/products/search?keyword={urllib.parse.quote('iPhone')}&pageNum=1&pageSize=10"
        with urllib.request.urlopen(url, timeout=10) as r:
            body = r.read()
            ok = r.status == 200 and json.loads(body).get("code") == "A0000"
        return time.perf_counter() - t0, ok
    except Exception:
        return time.perf_counter() - t0, False

def main():
    latencies, ok_count, total = [], 0, 0
    deadline = time.perf_counter() + DURATION
    i = 0
    with ThreadPoolExecutor(max_workers=CONCURRENCY) as ex:
        while time.perf_counter() < deadline:
            futs = [ex.submit(hit, i + j) for j in range(CONCURRENCY)]
            i += CONCURRENCY
            for f in futs:
                lat, ok = f.result()
                total += 1
                ok_count += 1 if ok else 0
                latencies.append(lat)
    latencies.sort()
    n = len(latencies)
    p = lambda q: latencies[min(n - 1, int(n * q))] * 1000
    print(f"mixed traffic concurrency={CONCURRENCY} duration={DURATION}s")
    print(f"total={total} qps={total / DURATION:.1f} fail_rate={(1 - ok_count / total) * 100:.2f}%")
    print(f"p50={p(0.50):.1f}ms p95={p(0.95):.1f}ms p99={p(0.99):.1f}ms max={latencies[-1] * 1000:.1f}ms")

if __name__ == "__main__":
    main()
