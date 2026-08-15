#!/usr/bin/env python3
"""搜索端点并发压测（ES 全文搜索 vs DB LIKE 回退对比用，标准库实现）

k6 的补充：k6 脚本见 product-list.js（混合流量）；本脚本只打搜索端点，
用于 ES/DB 两种检索模式的分项延迟对比（两种模式各启动一轮应用后跑同一脚本）。
k6 二进制下载受限（GitHub 大文件被网络劫持）时的等价替代，延迟口径一致。

用法：
    python3 search-bench.py <并发> <时长秒> [keyword]
    python3 search-bench.py 20 30 iPhone

输出：total / qps / p50 / p95 / p99 / max / fail_rate
"""
import json
import sys
import time
import urllib.request
from concurrent.futures import ThreadPoolExecutor

BASE = "http://localhost:8080"
CONCURRENCY = int(sys.argv[1]) if len(sys.argv) > 1 else 20
DURATION = int(sys.argv[2]) if len(sys.argv) > 2 else 30
KEYWORD = sys.argv[3] if len(sys.argv) > 3 else "iPhone"

def hit(_):
    url = f"{BASE}/api/products/search?keyword={urllib.parse.quote(KEYWORD)}&pageNum=1&pageSize=10"
    t0 = time.perf_counter()
    try:
        with urllib.request.urlopen(url, timeout=10) as r:
            body = r.read()
            ok = r.status == 200 and json.loads(body).get("code") == "A0000"
        return time.perf_counter() - t0, ok
    except Exception:
        return time.perf_counter() - t0, False

def main():
    latencies, ok_count, total = [], 0, 0
    deadline = time.perf_counter() + DURATION
    with ThreadPoolExecutor(max_workers=CONCURRENCY) as ex:
        while time.perf_counter() < deadline:
            futs = [ex.submit(hit, i) for i in range(CONCURRENCY)]
            for f in futs:
                lat, ok = f.result()
                total += 1
                ok_count += 1 if ok else 0
                latencies.append(lat)
    latencies.sort()
    n = len(latencies)
    p = lambda q: latencies[min(n - 1, int(n * q))] * 1000
    print(f"keyword={KEYWORD} concurrency={CONCURRENCY} duration={DURATION}s")
    print(f"total={total} qps={total / DURATION:.1f} fail_rate={(1 - ok_count / total) * 100:.2f}%")
    print(f"p50={p(0.50):.1f}ms p95={p(0.95):.1f}ms p99={p(0.99):.1f}ms max={latencies[-1] * 1000:.1f}ms")

if __name__ == "__main__":
    import urllib.parse
    main()
