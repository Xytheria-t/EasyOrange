# EasyOrange Frontend

> React 19 + TypeScript + Vite 的 SPA，C 端 + 管理端（暖橙指挥中心设计系统）双布局。
>
> **开发约定（管理端 Portal / AdminTable 签名 / Zustand 写入规则等 15 条坑）见 [doc/agents/开发规范.md §前端约定](../doc/agents/开发规范.md)**；全局硬约束见 [根 AGENTS.md](../AGENTS.md)。本文只放前端自身的入口地图与命令。

## 技术栈

TypeScript · React 19 · React Router v7 · Vite · Tailwind CSS 4 + shadcn/ui · TanStack Query 5（服务端状态）· Zustand 5（客户端状态）· react-hook-form + Zod（表单）· react-helmet-async（路由级 meta）· Lucide（图标）· Biome + jsx-a11y（lint/format）· Vitest + Testing Library（单元/组件）· Playwright（E2E）。

> 版本与测试数的单一来源：[doc/架构/架构-技术栈.md](../doc/架构/架构-技术栈.md)、[doc/工程指标.md §1.4](../doc/工程指标.md)。

## 可用命令

| 命令 | 说明 |
|------|------|
| `npm run dev` | 开发服务器（:5173） |
| `npm run build` / `build:analyze` | 生产构建 / 构建 + Bundle 分析（`dist/stats.html`） |
| `npm run preview` | 预览构建结果 |
| `npm run typecheck` | TypeScript 类型检查 |
| `npm run lint` / `lint:check` | Biome 检查并修复 / 只检查（CI 门禁：0 errors） |
| `npm run test` / `test:watch` | Vitest 单跑 / watch |
| `npm run test:e2e` | Playwright E2E（全 `page.route` mock，无后端依赖） |

## 页面路由

| 页面 | 路由 | 说明 | 需登录 |
|------|------|------|--------|
| 首页 / 资产列表 / 资产详情 | `/` · `/products` · `/products/:id` | 推荐、分类筛选排序、详情 + 评价 + 收藏 | 否 |
| 搜索 | `/search` | 关键词搜索 + 筛选 | 否 |
| 发布 / 编辑资产 | `/publish` · `/products/:id/edit` | 发布表单（含 AI 拍照识别）、编辑已发布资产 | 是 |
| 订单 | `/orders` · `/orders/:id` | 订单列表 / 详情 | 是 |
| 收银台 / 支付结果 | `/payment` · `/payment/result` | 在线支付与结果展示 | 是 |
| 个人中心 / 收藏 / 信用评分 | `/profile` · `/favorites` · `/credit` | 资料与密码、收藏管理、信用分与变更记录 | 是 |
| 消息中心 / 通知中心 | `/messages` · `/notifications` | 站内信（含 WebSocket 实时聊天）、系统通知 | 是 |
| 登录 / 找回密码 | `/login` · `/forgot-password` | 登录 + 注册 Tab 切换（无独立注册页） | 否 |
| 管理端 | `/admin/**` | `dashboard` · `users` · `products`（审核）· `orders` · `categories` · `reviews` · `reports` · `stats` | 是（ADMIN） |

> 管理端路由必须在 `MinimalLayout` **外部**独立渲染（否则 C 端 Header 会出现在管理页）。

## 性能优化

- 按路由 `React.lazy` 代码分割；重依赖（recharts / dayjs / monaco-editor / xlsx / @tiptap/*）在 `vite.config.ts` 的 `manualChunks` 分独立 `vendor-*` 块，组件用 `React.lazy` 包装
- `npm run build:analyze` 输出 treemap 定位大块
- 图片懒加载与压缩、骨架屏、TanStack Query 缓存、Tailwind 原子化样式
- 21 个路由独立 `title` + `og:title` / `description`（react-helmet-async）

## 环境变量

前端只读 `VITE_*`：`VITE_WS_URL`（WebSocket 地址，未设时从 `location.host` 推导）。全量键位见仓库根 `.env.example`。

## 许可

MIT License
