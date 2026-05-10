# 易橙坊前端 - EasyOrange Frontend

> 基于 React 19 + TypeScript + Vite 构建的现代化 SPA 应用

## 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| **语言** | TypeScript 5.3+ | 类型安全 |
| **框架** | React 19 | UI 框架 |
| **路由** | React Router v7 | SPA 路由管理 |
| **数据获取** | TanStack React Query 5 | 服务端状态管理 |
| **状态管理** | Zustand 5 | 客户端状态管理 |
| **构建** | Vite 8 | 极速构建工具 |
| **样式** | Tailwind CSS 4 | 原子化 CSS |
| **图标** | Lucide React | 图标库 |
| **测试** | Playwright | E2E 测试 |
| **质量** | ESLint + Prettier + jsx-a11y | 代码规范与可访问性检查 |

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
│   │   ├── productApi.ts     # 商品 API
│   │   ├── orderApi.ts       # 订单 API
│   │   └── ...
│   ├── components/           # 可复用组件
│   │   ├── layout/           # 布局组件
│   │   ├── sections/         # 页面区块组件
│   │   ├── profile/          # 个人中心组件
│   │   ├── products/         # 商品相关组件
│   │   └── ui/               # 基础 UI 组件
│   ├── features/             # 业务模块
│   │   └── auth/             # 认证模块
│   │       └── session.ts    # TokenRefreshManager
│   ├── hooks/                # 自定义 Hooks
│   │   ├── auth/             # 认证相关 Hooks
│   │   ├── order/            # 订单相关 Hooks
│   │   ├── product/          # 商品相关 Hooks
│   │   └── ui/               # UI 相关 Hooks (useColumnCount 等)
│   ├── lib/                  # 库配置
│   ├── pages/                # 页面组件
│   │   └── publish/          # 发布商品子模块
│   ├── routes/               # 路由配置
│   ├── store/                # Zustand 状态管理
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
└── nginx.conf                # Nginx 部署配置
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
| `npm run preview` | 预览构建结果 |
| `npm run lint` | 代码检查并自动修复 |
| `npm run typecheck` | TypeScript 类型检查 |

## 页面路由

| 页面 | 路由 | 功能 | 需登录 |
|------|------|------|--------|
| 首页 | `/` | 推荐商品、轮播图、平台统计 | 否 |
| 商品列表 | `/products` | 商品浏览、分类筛选、排序 | 否 |
| 商品详情 | `/products/:id` | 商品详情、评价、收藏 | 否 |
| 搜索 | `/search` | 关键词搜索、筛选 | 否 |
| 发布商品 | `/publish` | 商品发布表单 | 是 |
| 编辑商品 | `/products/:id/edit` | 编辑已发布商品 | 是 |
| 个人中心 | `/profile` | 个人信息、密码修改 | 是 |
| 我的收藏 | `/favorites` | 收藏商品管理 | 是 |
| 消息中心 | `/messages` | 站内消息列表 | 是 |
| 我的订单 | `/orders` | 订单列表 | 是 |
| 订单详情 | `/orders/:id` | 订单详情 | 是 |
| 收银台 | `/payment` | 在线支付 | 是 |
| 支付结果 | `/payment/result` | 支付结果展示 | 是 |
| 登录 | `/login` | 用户登录 | 否 |
| 找回密码 | `/forgot-password` | 密码找回 | 否 |

## 核心功能

### 用户系统
- 注册/登录（用户名密码）
- JWT 令牌认证 + 自动刷新
- 个人资料管理
- 密码修改与找回

### 商品系统
- 商品发布（多图上传、分类选择、草稿保存）
- 商品编辑/删除
- 商品搜索与筛选
- 相似商品推荐

### 交易系统
- 订单创建
- 订单状态管理
- 在线支付集成
- 评价系统

### 互动系统
- 商品收藏
- 站内消息
- 系统通知

## 环境变量

### 开发环境 (`.env.development`)
```env
VITE_API_BASE_URL=/api
VITE_APP_TITLE=易橙坊 - 让闲置流转，让价值延续
```

### 生产环境 (`.env.production`)
```env
VITE_API_BASE_URL=https://api.easyorange.com
VITE_APP_TITLE=易橙坊 - 让闲置流转，让价值延续
```

## 开发规范

### 代码风格
- 使用 ESLint + Prettier
- 所有函数和变量必须有类型注解
- 优先使用 `const` 和不可变数据模式
- 使用 async/await 处理异步操作

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
- 图片懒加载与压缩
- 骨架屏加载动画
- TanStack Query 数据缓存
- Tailwind CSS 原子化样式

## 许可证

MIT License

---

**EasyOrange** - 让闲置流转，让价值延续
