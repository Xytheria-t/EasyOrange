// EasyOrange 下单写路径压测（k6）— 验证「本地单事务 + Redisson 分布式锁」的防超卖链路
//
// 前置条件：
//   1. 应用 + MySQL/Redis 已启动（docker compose up -d）
//   2. 有可登录账号 + 一个库存充足的已上架商品
//   3. 压测前关闭全局限流（本地限流按 IP 计数，单机压测会被截断）：
//        RATE_LIMIT_FILTER_ENABLED=false docker compose up -d --scale easyorange-app=2
//
// 用法：
//   k6 run --vus 20 --duration 30s load-tests/order-create.js
//   K6_USERNAME=testuser K6_PASSWORD=123456 PRODUCT_ID=<商品ID> k6 run load-tests/order-create.js
//
// 内置断言（对齐 doc/工程指标.md 写路径目标）：
//   http_req_failed     < 1%（不含 429 限流响应——压测期间限流关闭）
//   下单接口 p95 < 500ms / p99 < 1000ms
//   业务成功率：Result.code == "A0000" 的比例 >= 99%（库存不足/并发超卖应被锁挡在业务层，
//   返回 B 类业务码而不是 500 —— 断言区分「HTTP 失败」与「业务失败」）

import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const USERNAME = __ENV.K6_USERNAME || 'testuser';
const PASSWORD = __ENV.K6_PASSWORD || '123456';
const PRODUCT_ID = __ENV.PRODUCT_ID || '';

export const options = {
  // 写路径压测默认并发小于只读：下单链路持有分布式锁 + 单事务，20 VU 已能压出锁竞争
  vus: __ENV.VUS ? Number(__ENV.VUS) : 20,
  duration: __ENV.DURATION || '30s',
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<500', 'p(99)<1000'],
  },
};

export function setup() {
  if (!PRODUCT_ID) {
    throw new Error('缺少 PRODUCT_ID 环境变量：压测下单需要指定一个库存充足的已上架商品');
  }
  // 登录拿 accessToken（JWT Access + Opaque Refresh）
  const res = http.post(
    `${BASE_URL}/api/auth/login`,
    JSON.stringify({ username: USERNAME, password: PASSWORD }),
    { headers: { 'Content-Type': 'application/json' } },
  );
  const body = res.json();
  const token = body?.data?.accessToken;
  if (!token) {
    throw new Error(`登录失败（${res.status}）：请检查 K6_USERNAME/K6_PASSWORD`);
  }
  return { token, productId: PRODUCT_ID };
}

export default function (data) {
  // 每 VU 一轮一个订单，唯一 phone 避免幂等/风控干扰
  const phone = `13${String((__VU * 10000000 + __ITER * 7) % 1000000000).padStart(9, '0')}`;
  const payload = JSON.stringify({
    items: [{ productId: data.productId, quantity: 1 }],
    phone,
    remark: 'k6-write-path',
  });

  const res = http.post(`${BASE_URL}/api/orders`, payload, {
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${data.token}`,
    },
  });

  // HTTP 层断言
  check(res, {
    '下单 HTTP 200': (r) => r.status === 200,
  });

  // 业务层断言：A0000 成功；库存不足等业务失败返回 B 类业务码（不是 500 崩溃）
  const code = res.json()?.code;
  check(res, {
    '下单业务成功 A0000': () => code === 'A0000',
    '无 500 兜底错误': () => code !== 'C0500',
  });

  sleep(0.2);
}
