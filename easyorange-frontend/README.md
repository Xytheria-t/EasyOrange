# EasyOrange Frontend (AI 资产管理)

> 基于 React 19 + TypeScript + Vite 构建的现代化 SPA 应用（**2025 年 11 月启动**）

## 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| **语言** | TypeScript 5.3+ | 类型安全 |
| **框架** | React 19 | UI 框架 |
| **路由** | React Router v7 | SPA 路由管理 |
| **数据获取** | TanStack React Query 5 | 服务端状态管理 |
| **状态管理** | Zustand 5 | 客户端状态管理 |
| **构建** | Vite 8 | 极速构建工具 |
| **样式** | Tailwind CSS 4 + shadcn/ui | 原子化 CSS + 可复用组件库 |
| **表单校验** | react-hook-form + Zod 4 | 类型安全表单验证 |
| **图标** | Lucide React | 图标库 |
| **测试** | Vitest + Testing Library | 单元/组件测试（100 文件, 952 测试） |
| | Playwright | E2E 测试 |
| **质量** | Biome + jsx-a11y | 统一 lint + format（替代 ESLint + Prettier） |

## 项目结构

```
easyorange-frontend/
├── src/
│   ├── api/                  # API 接口层
│   │   ├── core/             # 请求核心模块
│   │   │   ├── request.ts    # 主请求函数
│   │   │   ├── requestManager.ts  # 请求去重/取消
│   │   │   ├── cache.ts      # 响应缓存
│   │   │   ├── interceptors.ts    # 拦截器管理
│   │   │   └── index.ts      # 统一导出
│   │   ├── productApi.ts     # 资产 API
│   │   ├── orderApi.ts       # 订单 API
│   │   ├── aiApi.ts          # AI 功能 API（定价/审核/语义搜索/问答/拍照上架）
│   │   ├── creditApi.ts      # 信用评分 API
│   │   └── ...
│   ├── admin/                # 管理端模块（暖橙指挥中心设计系统）
│   │   ├── layout/           # AdminLayout, AdminSidebar, AdminHeader
│   │   ├── pages/            # dashboard / users / products / orders / categories / reviews / reports / stats
│   │   ├── components/       # AdminTable, AdminSelect(Portal), StatusBadge, ConfirmModal
│   │   └── styles/           # admin.css, admin-layout.css
│   ├── components/           # 可复用组件
│   │   ├── layout/           # 布局组件
│   │   ├── ai/               # AI 组件（AiPricingBadge, AiPhotoCapture, AiReviewSuggestion, SemanticSearchToggle, AiQaPanel, AiCopyGeneration, CreditScoreCard）
│   │   ├── sections/         # 页面区块组件
│   │   ├── profile/          # 个人中心组件
│   │   ├── products/         # 资产相关组件
│   │   └── ui/               # 基础 UI 组件
│   ├── features/             # 业务模块
│   │   └── auth/             # 认证模块
│   │       └── session.ts    # TokenRefreshManager
│   ├── hooks/                # 自定义 Hooks
│   │   ├── useAiPricing.ts   # AI 定价 Hook
│   │   ├── useAutoListing.ts # 拍照上架 Hook
│   │   ├── useSemanticSearch.ts # 语义搜索 Hook
│   │   ├── useAiQa.ts        # AI 问答 Hook
│   │   ├── useAiCopyGeneration.ts # AI 文案生成 Hook
│   │   ├── auth/             # 认证相关 Hooks
│   │   ├── order/            # 订单相关 Hooks
│   │   ├── product/          # 资产相关 Hooks
│   │   └── ui/               # UI 相关 Hooks (useColumnCount 等)
│   ├── lib/                  # 库配置
│   ├── pages/                # 页面组件
│   │   └── publish/          # 发布资产子模块
│   ├── routes/               # 路由配置
│   ├── store/                # Zustand 状态管理（认证、聊天、UI、Overlay）
│   ├── styles/               # 样式文件
│   ├── types/                # 类型定义
│   ├── utils/                # 工具函数
│   ├── App.tsx               # 应用入口
│   └── main.tsx              # 渲染入口
├── index.html                # HTML 入口
├── vite.config.ts            # Vite 配置
├── tsconfig.json             # TypeScript 配置
├── tailwind.config.js        # Tailwind 配置
├── playwright.config.ts      # E2E 测试配置
├── Dockerfile                # Docker 构建文件
├── nginx.conf                # Nginx 部署配置
└── security-headers.conf     # Nginx 安全响应头配置 (CSP / HSTS / XFO 等)
```

## 快速开始

### 环境要求

- Node.js >= 18.0.0
- npm >= 8.0.0

### 安装依赖

```bash
npm install
```

### 启动开发服务器

```bash
npm run dev
```

访问地址：http://localhost:5173/

### 生产构建

```bash
npm run build
npm run preview
```

## 可用命令

| 命令 | 说明 |
|------|------|
| `npm run dev` | 启动开发服务器 (:5173) |
| `npm run build` | 生产构建 |
| `npm run build:analyze` | 生产构建 + Bundle 分析（rollup-plugin-visualizer 输出 `dist/stats.html`） |
| `npm run preview` | 预览构建结果 |
| `npm run lint` | 代码检查并自动修复 |
| `npm run typecheck` | TypeScript 类型检查 |

## 页面路由

| 页面 | 路由 | 功能 | 需登录 |
|------|------|------|--------|
| 首页 | `/` | 推荐资产、轮播图、平台统计 | 否 |
| 资产列表 | `/products` | 资产浏览、分类筛选、排序 | 否 |
| 资产详情 | `/products/:id` | 资产详情、评价、收藏 | 否 |
| 搜索 | `/search` | 关键词搜索、筛选 | 否 |
| 提交资产 | `/publish` | 资产发布表单 | 是 |
| 编辑资产 | `/products/:id/edit` | 编辑已发布资产 | 是 |
| 个人中心 | `/profile` | 个人信息、密码修改 | 是 |
| 我的收藏 | `/favorites` | 收藏资产管理 | 是 |
| 消息中心 | `/messages` | 站内消息列表 | 是 |
| 信用评分 | `/credit` | 信用评分展示、变更记录 | 是 |
| 通知中心 | `/notifications` | 系统通知列表、查看详情 | 是 |
| 我的订单 | `/orders` | 订单列表 | 是 |
| 订单详情 | `/orders/:id` | 订单详情 | 是 |
| 收银台 | `/payment` | 在线支付 | 是 |
| 支付结果 | `/payment/result` | 支付结果展示 | 是 |
| 登录/注册 | `/login` | 用户登录+注册（Tab 切换，无独立注册页） | 否 |
| 找回密码 | `/forgot-password` | 密码找回 | 否 |
| 管理后台 | `/admin` | 管理端仪表盘（重定向到 /admin/dashboard） | 是 |
| 管理仪表盘 | `/admin/dashboard` | 数据概览、待办事项、最近动态 | 是 |
| 用户管理 | `/admin/users` | 用户列表、搜索、详情弹窗、状态变更 | 是 |
| 资产管理 | `/admin/products` | 资产审核列表、详情抽屉、通过/拒绝 | 是 |
| 订单管理 | `/admin/orders` | 订单列表、筛选、状态追踪 | 是 |
| 分类管理 | `/admin/categories` | 分类树、CRUD 操作、启用禁用 | 是 |
| 评价管理 | `/admin/reviews` | 评价列表、详情、删除 | 是 |
| 举报管理 | `/admin/reports` | 举报处理、处理/驳回操作 | 是 |
| 数据统计 | `/admin/stats` | 统计卡片、图表、活动时间线 | 是 |

## 核心功能

### 用户系统
- 注册/登录（用户名密码）
- JWT 令牌认证 + 自动刷新
- 个人资料管理
- 密码修改与找回

### 资产系统
- 资产发布（多图上传、分类选择、草稿保存）
- 资产编辑/删除
- 资产搜索与筛选
- 相似资产推荐

### 交易系统
- 订单创建
- 订单状态管理
- 在线支付集成
- 评价系统

### 互动系统
- 资产收藏
- 站内消息
- 系统通知

### AI 系统
- **智能定价** - AI 分析市场行情给出定价建议（AiPricingBadge + useAiPricing）
- **拍照上架** - 上传图片自动生成资产信息（AiPhotoCapture + useAutoListing）
- **AI 审核** - AI 分析资产信息给出审核建议（AiReviewSuggestion + useAdminProductAudit）
- **语义搜索** - 基于语义向量搜索相似资产（SemanticSearchToggle + useSemanticSearch）
- **智能问答** - 基于资产上下文回答认领方问题（AiQaPanel + useAiQa）
- **智能文案** - 基于资产信息自动生成资产描述和标题（AiCopyGeneration + useAiCopyGeneration）
- **信用评分** - 基于交易数据的信用评分体系（CreditScoreCard + creditApi）

## 环境变量

### 开发环境 (`.env.development`)
```env
VITE_API_BASE_URL=/api
VITE_APP_TITLE=EasyOrange — AI 资产管理
```

### 生产环境 (`.env.production`)
```env
VITE_API_BASE_URL=https://api.easyorange.com
VITE_APP_TITLE=EasyOrange — AI 资产管理
```

## 开发规范

### 代码风格
- 使用 ESLint + Prettier
- 所有函数和变量必须有类型注解
- 优先使用 `const` 和不可变数据模式
- 使用 async/await 处理异步逻辑

### 管理端开发约定
- **样式**: 所有 `src/admin/` 页面/组件必须使用内联 `style={{}}`，禁止依赖外部CSS（`admin.css` 仅用于侧边栏/头部布局）
- **下拉菜单**: 必须使用 `AdminSelect` 组件（`src/admin/components/AdminSelect.tsx`），禁止使用原生 `<select>`
- **设计系统**: 暖橙指挥中心 — 主色 Orange(#F97316) / Rose(#FB7185)，毛玻璃侧边栏，渐变强调色

### Git 提交规范
```
feat: 新功能
fix: 修复 bug
docs: 文档更新
style: 代码格式调整
refactor: 重构代码
test: 测试相关
chore: 构建/工具链相关
```

## 性能优化

- 代码分割（React.lazy 按路由懒加载）
- 重依赖独立 chunk（`vite.config.ts` `manualChunks` 中为 recharts 等大库分配 `vendor-*` 块，组件用 `React.lazy` 包装；非相关路由不下载）
- Bundle 分析（`npm run build:analyze` 输出 `dist/stats.html`，treemap 可视化定位大块）
- 图片懒加载与压缩
- 骨架屏加载动画
- TanStack Query 数据缓存
- Tailwind CSS 原子化样式

## 许可证

MIT License

---

**EasyOrange** — AI 资产管理
