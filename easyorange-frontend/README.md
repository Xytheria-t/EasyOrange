# EasyOrange Frontend

> React + TypeScript + Vite 的 SPA，C 端 + 管理端（统一设计系统）双布局。
>
> **开发约定见 [AGENTS.md](./AGENTS.md)**（管理端 Portal / AdminTable 签名 / Zustand 写入规则等「不按这个写就出 bug」的坑）；全局硬约束见 [根 AGENTS.md](../AGENTS.md)。本文只放前端自身的入口地图与命令。

## 技术栈

TypeScript（`strict` 全开）· React · React Router · Vite · Tailwind CSS + Radix UI（shadcn 风格自建）· TanStack Query（服务端状态）· Zustand（客户端状态）· react-hook-form + Zod（表单）· react-helmet-async（路由级 meta）· react-markdown + remark-gfm（对话正文渲染）· @stomp/stompjs（WebSocket）· Lucide（图标）· Biome（lint/format，内置 a11y 规则）· Vitest + Testing Library + MSW（单元/组件）· Playwright（E2E）。

> 版本单一来源：`package.json`（依赖）+ 根 `compose.yaml`（中间件）；测试数见 [doc/工程指标.md](../doc/工程指标.md)（前端测试规模只在 §1.2 与「结构计数」区块维护）。

## 可用命令

| 命令 | 说明 |
|------|------|
| `npm run dev` | 开发服务器（:5173） |
| `npm run build` / `build:analyze` | 生产构建 / 构建 + Bundle 分析（`dist/stats.html`） |
| `npm run preview` | 预览构建结果 |
| `npm run typecheck` / `typecheck:test` / `typecheck:all` | 主 tsconfig / 测试文件 tsconfig / 串联两者（主 tsconfig 排除测试，CI 单列第三步） |
| `npm run lint` / `lint:check` | Biome 检查并修复 / 只检查（CI 门禁：0 errors） |
| `npm run test` / `test:watch` | Vitest 单跑 / watch |
| `npm run test:e2e` | Playwright E2E（全 `page.route` mock，无后端依赖） |

## 页面路由

C 端只列 AI 叙事面与共用壳（通知中心 / 独立搜索页 / 我的发布 / 编辑商品等纯业务面 2026-10 已裁）；管理端列全部页面——2026-10-21 加回业务管理面（数据总览 / 商品审核 / 用户管理 / 订单管理），后端 REST 端点始终由 Swagger 承载。

| 页面 | 路由 | 说明 | 需登录 |
|------|------|------|--------|
| 首页 | `/` | 首屏区块（懒加载） | 否 |
| 资产列表 | `/products` | 无限滚动 + 行虚拟化；检索入口收敛到 `?keyword=` | 否 |
| 资产详情 | `/products/:id` | 可点结果；后端 `product-paths` 放行，前端 `skipAuth` 走匿名读 | 否 |
| AI 助手 Playground | `/playground` | 多轮 Agent 对话 + 知识库/商品引用溯源（SSE 流式 + 步骤可视化） | **是** |
| 发布助手 | `/publish` | 拍照识别回填表单；`aiSuggestion` 原文留档供采纳率统计 | 是 |
| 消息中心 | `/messages` · `/messages/:targetUserId` | 站内信 + STOMP 实时沟通（选中会话走路由参数，可深链） | 是 |
| 订单 | `/orders` · `/orders/:id` | 订单列表 / 详情（支付动作在列表 / 详情内触发，无独立收银台页） | 是 |
| 个人中心 | `/profile` | 资料与密码 | 是 |
| 登录 / 找回密码 | `/login` · `/forgot-password` | 登录 + 注册 Tab 切换（无独立注册页） | 否 |
| 管理端 · 总览 | `/admin` · `/admin/overview` | 一屏看全用户 / 商品 / 订单 / 营收四个口径 + 分布与走势（手写 SVG 折线，不引图表库） | 是（ADMIN） |
| 管理端 · 业务 | `/admin/products` · `/admin/users` · `/admin/orders` | 商品审核（批量审核 + 审核日志）· 用户管理（封禁 / 解锁 / 强制下线 / 改角色）· 订单干预（取消 / 强制完成 / 退款） | 是（ADMIN） |
| 管理端 · AI 运维 | `/admin/knowledge` · `/admin/retrieval-quality` | RAG 文档摄入管线 + 检索质量回看（ADR-0012 兑现位） | 是（ADMIN） |

> 管理端路由必须在 `MinimalLayout` **外部**独立渲染（否则 C 端 Header 会出现在管理页）。
> `/playground` 需登录：AI 对话依赖登录身份（长期偏好加载、`remember_preference` 落库），且后端 `/api/ai/**` 本身要求 JWT。

## 性能优化

- 14 个路由全部 `React.lazy` + 统一 `LoadingFallback`；`withSuspense` HOF 把路由级 `title` / `description` 一起包进 Suspense（14 条路由 meta，react-helmet-async）
- `npm run build:analyze` 输出 treemap 定位大块
- 图片懒加载与压缩、骨架屏、TanStack Query 缓存、Tailwind 原子化样式
- 管理端列表重渲染成本见 [doc/工程指标.md §1.4](../doc/工程指标.md)；入口图体积同节实测

## 环境变量

前端**不读任何 `VITE_*` 变量**：WebSocket 地址从 `location.protocol` / `location.host` 推导（`useStompChat`），API 基址走 Vite dev/proxy 约定。全量键位见仓库根 `.env.example`。

## 许可

木兰宽松许可证第 2 版（[Mulan PSL v2](../LICENSE)）
