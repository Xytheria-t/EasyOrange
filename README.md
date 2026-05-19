# EasyOrange 易橙坊

> 让闲置流转，让价值延续 —— 安全、便捷、环保的校园交易体验

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-25-orange.svg)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.3-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-19-61DAFB.svg)](https://react.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.3+-blue.svg)](https://www.typescriptlang.org/)
[![Vite](https://img.shields.io/badge/Vite-8-purple.svg)](https://vitejs.dev/)

## 项目简介

EasyOrange 是一个面向校园用户的二手交易平台，采用现代化的前后端分离架构，致力于为学生提供安全、便捷的闲置物品交易服务。平台支持商品发布、浏览、搜索、收藏、订单管理、在线支付、站内消息等完整交易流程。

### 核心特性

- **校园专属** - 针对校园场景优化，安全可信
- **响应式设计** - 完美适配移动端、平板和桌面端
- **安全可靠** - JWT 认证、BCrypt 加密、输入验证、SQL 注入防护
- **现代化前端** - React 19 + TypeScript + Vite 8，极速开发体验
- **DDD 架构** - 后端采用领域驱动设计，模块化高内聚低耦合
- **AI 赋能** - 智能定价、拍照上架、AI 审核、语义搜索、智能问答、信用评分
- **完整生态** - 商品、订单、支付、收藏、消息、AI 全链路覆盖

## 系统架构

```
EasyOrange Platform
  Frontend (React 19 SPA)           Backend (Spring Boot 4.0)
  ┌──────────────────────┐          ┌──────────────────────┐
  │  React 19            │          │  DDD Modular          │
  │  React Router v7     │  REST    │  11 Maven Modules     │
  │  TanStack Query 5    │◄───────►│  MyBatis-Plus         │
  │  Zustand 5           │  API     │  Spring Security      │
  │  Tailwind CSS 4      │          │  Saga Pattern         │
  └──────────────────────┘          └──────────────────────┘
         │                                  │
         ▼                                  ▼
  Vite 8 Dev Server                  MySQL 8.4 + Redis 7.4
  Port 5173                          Port 3306 + 6379
```

## 技术栈

### 后端技术

| 技术 | 版本 | 说明 |
|------|------|------|
| **语言** | Java 25 | 最新 LTS 版本 |
| **框架** | Spring Boot 4.0.3 | 现代化微服务框架 |
| **ORM** | MyBatis-Plus 3.5.16 | 高效数据访问层 |
| **安全** | Spring Security + JWT (jjwt 0.13.0) | 认证授权 |
| **数据库** | MySQL 8.4 | 关系型数据库 |
| **迁移** | Flyway | 数据库版本管理 |
| **缓存** | Redis 7.4 | 高性能缓存 |
| **序列化** | Jackson 3.1.2 | JSON 处理 |
| **映射** | MapStruct 1.6.3 | 对象映射 |
| **构建** | Maven | 依赖管理和构建 |

### 前端技术

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
| **质量** | ESLint + Prettier | 代码规范 |

## 项目结构

```
easyorange/
├── easyorange-backend/               # 后端服务 (10 Maven 模块)
│   ├── easyorange-application/       # 应用层（启动类、健康检查、平台统计）
│   ├── easyorange-admin/             # 管理端（用户/商品/订单/分类/举报/审核 API）
│   ├── easyorange-common/            # 公共模块（Result, PageResult, 注解, 异常, 领域事件基类）
│   ├── easyorange-framework/         # 框架层（安全、配置、Outbox、事件发布）
│   ├── easyorange-user/              # 用户域模块
│   ├── easyorange-product/           # 商品域模块
│   ├── easyorange-order/             # 订单域模块（含 Saga 编排）
│   ├── easyorange-payment/           # 支付域模块
│   ├── easyorange-message/           # 消息域模块
│   ├── easyorange-favorite/          # 收藏域模块
│   ├── pom.xml                       # Maven 父工程
│
├── easyorange-frontend/              # 前端应用 (React SPA)
│   ├── src/
│   │   ├── api/                      # API 接口层
│   │   ├── app/                      # 应用配置（路由、认证会话）
│   │   ├── components/               # 可复用组件
│   │   ├── layout/               # 布局组件（Header, Footer）
│   │   ├── chat/                 # 聊天组件（ChatHeader, MessageBubble, MessageList, ChatInputBar, TypingIndicator）
│   │   ├── sections/             # 页面区块组件
│   │   │   ├── profile/              # 个人中心组件
│   │   │   ├── products/             # 商品相关组件
│   │   │   └── ui/                   # 基础 UI 组件
│   │   ├── features/                 # 业务模块（auth 等）
│   │   ├── hooks/                    # 自定义 Hooks
│   │   └── chat/                 # 聊天 Hooks（useStompChat, useChatMessages, useMessageRecall, useChatNotification, useOfflineQueue）
│   ├── lib/                      # 库配置（queryClient, motion）
│   │   ├── pages/                    # 页面组件
│   │   │   ├── messages/             # 消息页面（MessagesPage, ChatWindowPage）
│   │   │   └── publish/              # 发布商品子模块
│   │   ├── routes/                   # 路由配置
│   │   ├── store/                    # Zustand 状态管理
│   │   └── chatStore.ts          # 聊天全局状态（messages, typingUsers, connectionStatus）
│   ├── styles/                   # 样式文件（Tailwind + CSS）
│   │   ├── types/                    # 类型定义
│   │   ├── utils/                    # 工具函数
│   │   └── main.tsx                  # 入口文件
│   ├── index.html                    # HTML 入口
│   ├── vite.config.ts                # Vite 配置
│   ├── tsconfig.json                 # TypeScript 配置
│   ├── tailwind.config.js            # Tailwind 配置
│   ├── postcss.config.js             # PostCSS 配置
│   ├── playwright.config.ts          # E2E 测试配置
│   ├── Dockerfile                    # Docker 构建文件
│   └── nginx.conf                    # Nginx 部署配置
│
├── docker-compose.yml                # Docker Compose 配置
├── .env.example                      # 环境变量示例
├── doc/                              # 项目文档
│   └── 架构/                         # 架构规范文档（已切分为多个子文档）
├── AGENTS.md                         # Agent 使用说明
├── CLAUDE.md                         # Claude 配置
├── DATABASE.md                       # 数据库设计文档
└── README.md                         # 项目文档
```

## 快速开始

### 环境要求

- **JDK**: 25+
- **Node.js**: 18+
- **Maven**: 3.8+
- **Docker**: 20.10+ (可选，用于数据库容器化)

### 1. 克隆项目

```bash
git clone https://gitee.com/cartethyia_XLS/easy-orange.git
cd easy-orange
```

### 2. 启动基础设施

```bash
# 使用 Docker Compose 启动 MySQL、Redis 和可选 ES
docker-compose up -d
docker compose up -d elasticsearch   # 可选：启用 Elasticsearch 搜索
```

### 3. 配置环境变量

#### 后端配置

```bash
cd easyorange-backend/easyorange-application
cp .env.example .env
```

编辑 `.env` 文件：

```env
# 数据库配置
DB_HOST=localhost
DB_PORT=3306
DB_USERNAME=root
DB_PASSWORD=your_password_here

# Redis 配置
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# CORS 配置
CORS_ALLOWED_ORIGINS=http://localhost:5173,http://localhost:3000

# JWT 密钥（生产环境必须修改，长度至少 32 字符）
JWT_SECRET_KEY=your-secret-key-must-be-at-least-32-characters-long
```

#### 前端配置

```bash
cd easyorange-frontend
# 开发环境配置已内置于 .env.development，可直接使用
```

### 4. 启动后端服务

```bash
cd easyorange-backend

# 编译并启动
./mvnw spring-boot:run -pl easyorange-application
```

后端服务将在 `http://localhost:8080` 启动

### 5. 启动前端开发服务器

```bash
cd easyorange-frontend

# 安装依赖
npm install

# 启动开发服务器
npm run dev
```

前端服务将在 `http://localhost:5173` 启动，API 请求自动代理到 `http://localhost:8080`

### 核心 API

| 模块 | 端点 | 说明 |
|------|------|------|
| **认证** | `POST /api/auth/login` | 用户登录 |
| | `POST /api/auth/logout` | 用户登出 |
| | `POST /api/auth/refresh` | 刷新令牌 |
| **用户** | `POST /api/users/register` | 用户注册 |
| | `GET /api/users/profile` | 获取个人资料 |
| | `PUT /api/users/profile` | 更新个人资料 |
| | `PUT /api/users/password` | 修改密码 |
| | `POST /api/users/forgot-password` | 忘记密码 |
| **商品** | `GET /api/products` | 商品列表（分页） |
| | `GET /api/products/{id}` | 商品详情 |
| | `POST /api/products` | 发布商品 |
| | `PUT /api/products/{id}` | 更新商品 |
| | `DELETE /api/products/{id}` | 删除商品 |
| | `GET /api/products/search` | 搜索商品 |
| **收藏** | `GET /api/favorites` | 收藏列表 |
| | `POST /api/favorites` | 添加收藏 |
| | `DELETE /api/favorites/{id}` | 取消收藏 |
| | `DELETE /api/favorites/batch` | 批量取消收藏 |
| **订单** | `GET /api/orders` | 订单列表 |
| | `POST /api/orders` | 创建订单 |
| | `GET /api/orders/{id}` | 订单详情 |
| **支付** | `POST /api/payment/pay` | 发起支付 |
| | `GET /api/payment/status` | 支付状态 |
| **消息** | `GET /api/messages` | 消息列表 |
| | `POST /api/messages` | 发送消息 |
| | `PUT /api/messages/{id}/recall` | 撤回消息（2分钟内） |
| | `POST /api/messages/typing` | 上报正在输入 |
| | `PUT /api/messages/read` | 批量标记已读 |
| **评价** | `POST /api/reviews` | 创建评价 |
| | `GET /api/reviews/product/{id}` | 商品评价列表 |
| **统计** | `GET /api/stats` | 平台统计数据 |
| **管理端-仪表板** | `GET /api/admin/dashboard/*` | 统计概览、待处理事项、最近动态 |
| **管理端-用户** | `GET/PUT /api/admin/users/*` | 用户列表、详情、状态、解锁、重置密码、角色 |
| **管理端-商品** | `GET/PUT /api/admin/products/*` | 商品列表、详情、状态、审核(带原因)、批量审核 |
| **管理端-订单** | `GET/PUT /api/admin/orders/*` | 订单列表、详情、取消、强制完成、退款、统计 |
| **管理端-分类** | CRUD `/api/admin/categories` | 分类 CRUD、树形结构、启用禁用 |
| **管理端-评价** | `GET/DELETE /api/admin/reviews/*` | 评价列表、详情、删除 |
| **管理端-举报** | `GET/PUT /api/admin/reports/*` | 举报列表、详情、处理(6种动作)、统计 |

## 功能模块

### 页面路由

| 页面 | 路由 | 功能 | 需登录 |
|------|------|------|--------|
| 首页 | `/` | 推荐商品、轮播图、平台统计 | 否 |
| 商品列表 | `/products` | 商品浏览、分类筛选、排序 | 否 |
| 商品详情 | `/products/:id` | 商品详情、评价、收藏 | 否 |
| 搜索 | `/search` | 关键词搜索、筛选 | 否 |
| 发布商品 | `/publish` | 商品发布表单（多图上传、草稿保存） | 是 |
| 编辑商品 | `/products/:id/edit` | 编辑已发布商品 | 是 |
| 个人中心 | `/profile` | 个人信息、密码修改、偏好设置 | 是 |
| 我的收藏 | `/favorites` | 收藏商品管理 | 是 |
| 消息中心 | `/messages` | 站内消息列表 | 是 |
| 通知中心 | `/notifications` | 系统通知列表、查看详情 | 是 |
| 聊天窗口 | `/messages/:targetUserId` | 实时聊天（STOMP WebSocket） | 是 |
| 我的订单 | `/orders` | 订单列表 | 是 |
| 订单详情 | `/orders/:id` | 订单详情 | 是 |
| 收银台 | `/payment` | 在线支付 | 是 |
| 支付结果 | `/payment/result` | 支付结果展示 | 是 |
| 登录/注册 | `/login` | 用户登录+注册（Tab 切换，无独立注册页） | 否 |
| 找回密码 | `/forgot-password` | 密码找回 | 否 |

### 核心功能

#### 1. 用户系统
- 注册/登录（用户名密码）
- JWT 令牌认证 + 自动刷新
- 个人资料管理（头像上传、昵称、联系方式）
- 密码修改与找回

#### 2. 商品系统
- 商品发布（多图上传、分类选择、草稿保存）
- 商品编辑/删除
- 商品搜索（关键词、分类）
- 商品筛选（价格、成色、排序）
- 商品详情展示
- 相似商品推荐

#### 3. 交易系统
- 订单创建（含 Saga 事务补偿）
- 订单状态管理（待付款、待发货、已完成）
- 订单列表/详情
- 在线支付集成
- 评价系统

#### 4. 互动系统
- 商品收藏（批量管理）
- 站内消息
- 系统通知

## Docker 部署

### 使用 Docker Compose

```bash
# 启动所有服务
docker-compose up -d

# 查看日志
docker-compose logs -f

# 停止服务
docker-compose down
```

### 数据库问题排查

如果遇到 "表不存在" 或 Flyway 迁移未执行的问题，可能是数据卷处于不一致状态。

**方案一：完全重置（删除所有数据）**

```bash
# 停止并删除数据卷
docker compose down -v

# 重新启动
docker compose up -d

# 等待数据库就绪后启动应用
```

**方案二：创建 Flyway 历史表（保留数据）**

```bash
# 进入 MySQL 容器
docker exec -it easyorange-mysql mysql -uroot -proot123456 easyorange

# 执行 SQL
CREATE TABLE IF NOT EXISTS flyway_schema_history (
    installed_rank INT NOT NULL,
    version VARCHAR(50),
    description VARCHAR(200) NOT NULL,
    type VARCHAR(20) NOT NULL,
    script VARCHAR(1000) NOT NULL,
    checksum INT,
    installed_by VARCHAR(100) NOT NULL,
    installed_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    execution_time INT NOT NULL,
    success TINYINT(1) NOT NULL,
    PRIMARY KEY (installed_rank)
);

# 退出
exit
```

### 环境变量配置

创建 `.env` 文件在项目根目录：

```env
# MySQL 配置
MYSQL_ROOT_PASSWORD=root123456
EASYORANGE_DB_NAME=easyorange
EASYORANGE_DB_USERNAME=easyorange_app
EASYORANGE_DB_PASSWORD=easyorange_app_dev

# Redis 配置（docker-compose 内置）
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# CORS 配置
CORS_ALLOWED_ORIGINS=http://localhost:5173,http://localhost:3000
```

### 前端容器化部署

```bash
cd easyorange-frontend
docker build -t easyorange-frontend .
docker run -p 80:80 easyorange-frontend
```

## 开发规范

### Git 提交规范

采用 [Conventional Commits](https://www.conventionalcommits.org/) 规范：

```
<type>: <description>

[optional body]
```

**Type 类型：**
- `feat`: 新功能
- `fix`: Bug 修复
- `docs`: 文档更新
- `style`: 代码格式调整
- `refactor`: 重构
- `test`: 测试相关
- `chore`: 构建/工具链

**示例：**
```bash
feat: add product search functionality
fix: resolve JWT token validation issue
docs: update API documentation
```

### 代码风格

#### 后端 (Java)
- 遵循 Google Java Style
- 使用 Lombok 简化代码
- 构造函数注入依赖
- 使用 Optional 处理空值
- 异常统一处理
- DDD 分层架构

#### 前端 (TypeScript/React)
- 使用 ESLint + Prettier
- 所有函数和变量必须有类型注解
- 优先使用 `const` 和不可变数据模式
- 使用 async/await 处理异步操作
- 错误边界处理（ErrorBoundary）
- 组件按功能组织，高内聚低耦合

### 分支策略

```
main          - 生产分支
develop       - 开发分支
feature/*     - 功能分支
bugfix/*      - Bug 修复分支
release/*     - 发布分支
hotfix/*      - 紧急修复分支
```

## 测试

### 后端测试

```bash
cd easyorange-backend

# 运行所有测试
./mvnw test

# 运行特定模块测试
./mvnw test -pl easyorange-user

# 生成测试覆盖率报告
./mvnw clean test jacoco:report
```

### 前端测试

```bash
cd easyorange-frontend

# 运行所有单元/组件/Hook 测试（81 个文件, 734 个用例）
npm test

# 监听模式
npm run test:watch

# 覆盖率报告（当前 ~42%）
npm run test:coverage

# E2E 测试（42 个用例，需先启动 dev server）
npm run test:e2e

# 类型检查
npm run typecheck

# 代码检查
npm run lint
```

## 性能优化

### 后端优化
- HikariCP 连接池配置优化
- Redis 缓存热点数据（订单缓存服务）
- 数据库索引优化
- 异步线程池处理耗时操作
- 日志异步输出
- Saga 事务补偿模式保证数据一致性

### 前端优化
- 代码分割（React.lazy 按路由懒加载）
- 图片懒加载与压缩
- 骨架屏加载动画
- 防抖节流优化
- TanStack Query 数据缓存
- Tailwind CSS 原子化样式，按需生成

## 安全特性

- JWT 无状态认证 + Token 自动刷新
- 密码 BCrypt 加密存储
- SQL 注入防护（MyBatis-Plus 参数化查询）
- XSS 防护（输入验证）
- CORS 跨域配置
- 请求频率限制（RateLimiter 注解）
- 防重复提交（RepeatSubmit 注解）
- 敏感数据脱敏（MaskUtils）
- Spring Security 安全过滤链

## 贡献指南

欢迎贡献代码！请遵循以下步骤：

1. Fork 本项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'feat: add some amazing feature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 提交 Pull Request

## 许可证

MIT License - 详见 [LICENSE](LICENSE) 文件

## 团队

- **开发者**: cartethyia
- **项目地址**: https://gitee.com/cartethyia_XLS/easy-orange

## 更新日志

### v0.1.0 (2025)
- 重构前端为 React 19 SPA 架构
- 引入 TanStack Query + Zustand 状态管理
- 后端采用 DDD 模块化架构
- 新增收藏模块（easyorange-favorite）
- 新增 Saga 事务补偿模式
- 引入 Tailwind CSS 4 样式系统
- 升级 Vite 8 构建工具
- 添加 Playwright E2E 测试

### v0.0.1 (2024-03)
- 初始版本发布
- 用户注册/登录功能
- 商品发布和浏览
- 订单管理系统
- 基础支付集成

## 致谢

感谢以下开源项目：

- [Spring Boot](https://spring.io/projects/spring-boot)
- [MyBatis-Plus](https://baomidou.com/)
- [React](https://react.dev/)
- [TanStack Query](https://tanstack.com/query)
- [Zustand](https://github.com/pmndrs/zustand)
- [Vite](https://vitejs.dev/)
- [Tailwind CSS](https://tailwindcss.com/)
- [TypeScript](https://www.typescriptlang.org/)

---

<div align="center">

**EasyOrange** - 让闲置流转，让价值延续

[返回顶部](#easyorange-校园二手交易平台)

</div>
