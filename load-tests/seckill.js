// EasyOrange 秒杀式抢购压测（k6）— 验证「瞬时洪峰下不超卖」这条不变式
//
// 与 order-create.js 的分工：
//   order-create.js : 稳态写路径 —— 单账号、每 VU 轮询下单、看 p95/p99 与业务成功率
//   本脚本          : 洪峰抢购   —— 多账号、同一商品、库存远小于抢购人数、t=0 同时开抢
//                     产出的是「成交数 == min(库存, 请求数)」这种可断言的不变式，而不是延迟指标
//
// 前置条件：
//   1. docker compose up -d（MySQL/Redis/RabbitMQ），应用 + 前端栈已启动
//   2. 压测商品 stock 设成远小于抢购人数（如 50），状态 ONLINE
//   3. 限流两种口径各跑一遍（本地限流按 IP 计数，单机压测必被截断）：
//        关限流：RATE_LIMIT_FILTER_ENABLED=false docker compose up -d --scale easyorange-app=2
//        留限流：默认即可 —— 此时 429 占比本身就是「保护路径生效」的数字
//
// 用法：
//   PRODUCT_ID=<商品ID> VUS=300 BUYERS=50 k6 run load-tests/seckill.js     # 默认：300 人同时开抢，各抢 1 次
//   PRODUCT_ID=<id> VUS=200 ITERS=5 k6 run load-tests/seckill.js           # 每人连抢 5 次（持续压力）
//   PRODUCT_ID=<id> MODE=rate RATE=500 BURST=10s k6 run load-tests/seckill.js  # 到货率口径（恒定 500 req/s 打 10s）
//   docker run --rm -i --network host -e PRODUCT_ID=<id> grafana/k6 run - < load-tests/seckill.js
//
// 跑完必须以 DB 复核（压测器自报不算数，见脚本末尾打印的 SQL）：
//   eo_product.stock 应等于 初始库存 − 成交单数 且 ≥ 0；
//   eo_order_item 行数 == eo_stock_ledger 中 DECREASE 流水条数 == 响应里的 A0000 数。
//
// 预期会看到的两个现象（不是 bug，是这条链路的固有代价，面试里正是要讲的）：
//   · 下单锁按 productId 串行（key=eo:order:lock:product:*），单商品砸 300 并发时请求在锁上排队，
//     p99 随排队长度线性上升；
//   · 排队超过 10s 锁等待的请求拿不到锁，抛 LockAcquisitionException → 映射成 B3009 丢单，
//     所以「成交数 == min(库存, 请求数)」这条不变式要盯 B3009 的数量而不是只盯超卖。

import http from 'k6/http';
import { check } from 'k6';
import { Counter, Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const PRODUCT_ID = __ENV.PRODUCT_ID || '';
const PASSWORD = __ENV.K6_PASSWORD || 'Password123';
const USER_PREFIX = __ENV.K6_USER_PREFIX || 'k6sec_';
const BUYERS = __ENV.BUYERS ? Number(__ENV.BUYERS) : 50; // 抢购账号数（setup 里注册，登录拿 token）
const MODE = __ENV.MODE || 'stampede'; // stampede=同时开抢 | rate=恒定到货率
const VUS = __ENV.VUS ? Number(__ENV.VUS) : 300;
const ITERS = __ENV.ITERS ? Number(__ENV.ITERS) : 1;
const RATE = __ENV.RATE ? Number(__ENV.RATE) : 500;
const BURST = __ENV.BURST || '10s';

// 自定义指标：秒杀的口径应该按「业务结果」分桶，而不是只看 http_req_failed
const attempts = new Counter('seckill_attempts'); // 有效下单请求数
const success = new Counter('seckill_success'); // A0000 成交
const outOfStock = new Counter('seckill_out_of_stock'); // B2003 库存已被抢光（预期内）
const orderError = new Counter('seckill_order_error'); // B3009 订单业务异常（锁等待超时等，关注项）
const businessReject = new Counter('seckill_business_reject'); // 其余 B 类（校验/状态等）
const throttled = new Counter('seckill_rate_limited'); // HTTP 429（保护路径生效）
const serverError = new Counter('seckill_server_error'); // HTTP 5xx / C0500（必须为 0）
const otherFail = new Counter('seckill_other_fail'); // 兜底：无法归类的失败
const buyLatency = new Trend('seckill_buy_duration', true);

const scenario =
  MODE === 'rate'
    ? {
        burst: {
          executor: 'constant-arrival-rate',
          rate: RATE,
          timeUnit: '1s',
          duration: BURST,
          preAllocatedVUs: Math.max(50, Math.ceil(RATE / 2)),
          maxVUs: Math.min(1000, RATE * 2),
        },
      }
    : {
        // 默认口径：所有 VU 在 t≈0 同时发一个请求 —— 最接近「同一秒开抢」
        stampede: {
          executor: 'per-vu-iterations',
          vus: VUS,
          iterations: ITERS,
          maxDuration: '120s',
        },
      };

export const options = {
  scenarios: scenario,
  thresholds: {
    // 洪峰下业务拒绝是预期的，不设业务阈值；只把「服务端崩了」钉死
    seckill_server_error: ['count==0'],
  },
};

export function setup() {
  if (!PRODUCT_ID) {
    throw new Error('缺少 PRODUCT_ID：秒杀压测需要指定一个库存有限的 ONLINE 商品');
  }

  // 1) 读压测前库存 —— 不变式的基准值，必须在任何下单发生前取
  const detail = http.get(`${BASE_URL}/api/products/${PRODUCT_ID}`);
  const product = detail.json()?.data;
  if (!product) {
    throw new Error(`商品详情读取失败（HTTP ${detail.status}）：检查 PRODUCT_ID 与服务是否已启动`);
  }
  if (product.status !== 'ONLINE') {
    throw new Error(`商品状态为 ${product.status}（${product.statusDesc}），秒杀需要 ONLINE 状态`);
  }
  if (!(product.stock > 0)) {
    throw new Error(`商品库存为 ${product.stock}，先补库存再压测`);
  }

  // 2) 注册并登录抢购账号池（注册接口只需 username+password，无验证码）
  const tokens = [];
  for (let i = 0; i < BUYERS; i++) {
    const username = `${USER_PREFIX}${i}`;
    const creds = JSON.stringify({ username, password: PASSWORD });
    const headers = { 'Content-Type': 'application/json' };
    // 已注册过就跳过注册直接登录，脚本可重复跑
    http.post(`${BASE_URL}/api/auth/register`, creds, { headers });
    const login = http.post(`${BASE_URL}/api/auth/login`, creds, { headers });
    const token = login.json()?.data?.accessToken;
    if (!token) {
      throw new Error(`账号 ${username} 登录失败（HTTP ${login.status}）：改用 K6_PASSWORD=<真实密码>`);
    }
    tokens.push(token);
  }

  return { tokens, productId: PRODUCT_ID, initialStock: product.stock, title: product.title };
}

export default function (data) {
  const token = data.tokens[(__VU + __ITER) % data.tokens.length];

  // 手机号对齐 ^1[3-9]\d{9}$，每请求唯一
  const phone = `13${String((__VU * 10000000 + __ITER * 7) % 1000000000).padStart(9, '0')}`;
  const payload = JSON.stringify({
    items: [{ productId: data.productId, quantity: 1 }],
    phone,
    remark: 'k6-seckill',
  });

  const res = http.post(`${BASE_URL}/api/orders`, payload, {
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
  });

  attempts.add(1);
  buyLatency.add(res.timings.duration);

  const code = res.json()?.code;
  if (code === 'A0000') {
    success.add(1);
  } else if (res.status === 429) {
    throttled.add(1);
  } else if (res.status >= 500 || code === 'C0500') {
    serverError.add(1);
  } else if (code === 'B2003') {
    outOfStock.add(1);
  } else if (code === 'B3009') {
    orderError.add(1);
  } else if (typeof code === 'string' && code.startsWith('B')) {
    businessReject.add(1);
  } else {
    otherFail.add(1);
  }

  check(res, { '无 500 兜底错误': () => res.status < 500 && code !== 'C0500' });
  // 秒杀场景不能 sleep：VU 一空闲洪峰就漏气
}

const count = (data, name) => data.metrics[name]?.values?.count ?? 0;

export function handleSummary(data) {
  const bought = count(data, 'seckill_success');
  const tried = count(data, 'seckill_attempts');
  const stock = data.setup_data?.initialStock ?? 0;
  const ceiling = Math.min(stock, tried); // 理论上限：库存和请求数取小

  const noOversell = bought <= stock;
  const no5xx = count(data, 'seckill_server_error') === 0;
  const verdict = noOversell && no5xx && bought === ceiling ? 'PASS' : 'FAIL';

  const p95 = data.metrics.http_req_duration?.values['p(95)']?.toFixed(0);
  const p99 = data.metrics.http_req_duration?.values['p(99)']?.toFixed(0);
  const lines = [
    '',
    '==================== 秒杀压测结果 ====================',
    `压测商品          : ${PRODUCT_ID}`,
    `压测前库存        : ${stock}`,
    `有效下单请求      : ${tried}`,
    `成交 (A0000)      : ${bought}`,
    `库存已被抢光 B2003: ${count(data, 'seckill_out_of_stock')}`,
    `锁等待超时 B3009  : ${count(data, 'seckill_order_error')}`,
    `其他业务拒绝 B*   : ${count(data, 'seckill_business_reject')}`,
    `被限流 (429)      : ${count(data, 'seckill_rate_limited')}`,
    `服务端错误 (5xx)  : ${count(data, 'seckill_server_error')}`,
    `其他失败          : ${count(data, 'seckill_other_fail')}`,
    `下单延迟 p95/p99  : ${p95}ms / ${p99}ms`,
    `理论成交上限      : min(库存 ${stock}, 请求 ${tried}) = ${ceiling}`,
    `判定              : ${verdict}`,
    `  · 未超卖        : ${bought} <= ${stock} -> ${noOversell ? 'PASS' : 'FAIL'}`,
    `  · 无 5xx        : ${no5xx ? 'PASS' : 'FAIL'}`,
    `  · 库存吃尽      : ${bought} == ${ceiling} -> ${bought === ceiling ? 'PASS' : `FAIL（丢单 ${ceiling - bought} 单，看 B3009 与 429）`}`,
    '',
    'DB 复核（口径以这里为准）：',
    `  docker exec -it easyorange-mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" easyorange -e "`,
    `    SELECT id, title, stock, status FROM eo_product WHERE id='${PRODUCT_ID}';`,
    `    SELECT COUNT(*) AS sold FROM eo_order_item WHERE product_id='${PRODUCT_ID}';`,
    `    SELECT COUNT(*) AS ledger, SUM(delta) AS delta_sum FROM eo_stock_ledger`,
    `      WHERE product_id='${PRODUCT_ID}' AND change_type='DECREASE';"`,
    `  期望：stock = ${stock} - 成交数 且 >= 0；sold 与 DECREASE 流水条数均等于成交数`,
    '=====================================================',
    '',
  ];

  return { stdout: lines.join('\n') };
}
