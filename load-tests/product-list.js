// EasyOrange 商品只读 API 压测（k6）
//
// 用法（默认单实例 localhost:8080）：
//   k6 run load-tests/product-list.js
//   k6 run --vus 50 --duration 30s load-tests/product-list.js        # 覆盖并发/时长
//   BASE_URL=http://localhost k6 run load-tests/product-list.js      # 多实例走 nginx LB
//   PRODUCT_ID=<真实ID> k6 run load-tests/product-list.js            # 附加详情接口流量
//
// Docker 方式（免装 k6，--network host 直接访问宿主机端口）：
//   docker run --rm -i --network host grafana/k6 run - < load-tests/product-list.js
//
// ⚠️ 压测前关闭限流（本地限流按 IP 计数，单机压测会被 1000 次/分 截断）：
//   RATE_LIMIT_FILTER_ENABLED=false docker compose up -d --scale easyorange-app=2
//
// 内置断言（k6 thresholds）：
//   http_req_duration p95 < 500ms（对齐 doc/工程指标.md §2.3 目标）
//   http_req_duration p99 < 1000ms
//   http_req_failed   < 1%

import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const DETAIL_ID = __ENV.PRODUCT_ID;

export const options = {
  vus: __ENV.VUS ? Number(__ENV.VUS) : 50,
  duration: __ENV.DURATION || '30s',
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<500', 'p(99)<1000'],
  },
};

export default function () {
  // 混合只读流量：商品列表(60%) + 分类(20%) + 搜索(20%)，列表轮换 5 页
  const n = (__ITER + __VU) % 5;
  const list = http.get(`${BASE_URL}/api/products?page=${1 + (n % 5)}&size=12`);
  check(list, { '商品列表 200': (r) => r.status === 200 });

  if (n % 5 === 3) {
    const categories = http.get(`${BASE_URL}/api/products/categories`);
    check(categories, { '分类 200': (r) => r.status === 200 });
  } else if (n % 5 === 4) {
    const search = http.get(`${BASE_URL}/api/products/search?keyword=${encodeURIComponent('手机')}`);
    check(search, { '搜索 200': (r) => r.status === 200 });
  }

  if (DETAIL_ID) {
    const detail = http.get(`${BASE_URL}/api/products/${DETAIL_ID}`);
    check(detail, { '商品详情 200': (r) => r.status === 200 });
  }

  sleep(0.1);
}
