# EasyOrange 校园二手交易平台

> 让闲置流转，让价值延续 —— 安全、便捷、环保的校园交易体验

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-25-orange.svg)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.3-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.3-blue.svg)](https://www.typescriptlang.org/)
[![Vite](https://img.shields.io/badge/Vite-5.0-purple.svg)](https://vitejs.dev/)

## 📖 项目简介

EasyOrange 是一个面向校园用户的二手交易平台，采用现代化的前后端分离架构，致力于为学生提供安全、便捷的闲置物品交易服务。平台支持商品发布、浏览、搜索、收藏、订单管理、在线支付等完整交易流程。

### ✨ 核心特性

- 🎯 **校园专属** - 针对校园场景优化，安全可信
- 📱 **响应式设计** - 完美适配移动端、平板和桌面端
- 🔒 **安全可靠** - JWT 认证、输入验证、SQL 注入防护
- ⚡ **高性能** - 原生 TypeScript、零框架依赖、极速加载
- 🎨 **现代 UI** - 清晰的视觉层次、流畅的交互动效
- 📊 **完整生态** - 商品、订单、支付、消息全链路覆盖

## 🏗️ 系统架构

```
┌─────────────────────────────────────────────────────────────┐
│                      EasyOrange Platform                     │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌──────────────────┐          ┌──────────────────┐        │
│  │   Frontend       │          │     Backend       │        │
│  │   (TypeScript)   │◄────────►│   (Spring Boot)   │        │
│  │   Vite 5.0       │   REST   │   Java 25         │        │
│  │   Zero Framework │   API    │   Modular DDD     │        │
│  └──────────────────┘          └──────────────────┘        │
│           │                              │                  │
│           │                              │                  │
│           ▼                              ▼                  │
│  ┌──────────────────┐          ┌──────────────────┐        │
│  │   HTML Pages     │          │    MySQL 8.0     │        │
│  │   Components     │          │    Redis 7       │        │
│  │   CSS Modules    │          │    Flyway Migrations│     │
│  └──────────────────┘          └──────────────────┘        │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

## 🛠️ 技术栈

### 后端技术

| 技术 | 版本 | 说明 |
|------|------|------|
| **语言** | Java 25 | 最新 LTS 版本 |
| **框架** | Spring Boot 4.0.3 | 现代化微服务框架 |
| **ORM** | MyBatis-Plus 3.5.16 | 高效数据访问层 |
| **数据库** | MySQL 8.0 | 关系型数据库 |
| **缓存** | Redis 7 | 高性能缓存 |
| **迁移** | Flyway | 数据库版本管理 |
| **认证** | JWT (jjwt 0.13.0) | 无状态认证 |
| **文档** | SpringDoc OpenAPI 3.0 | API 文档生成 |
| **构建** | Maven | 依赖管理和构建 |

### 前端技术

| 技术 | 版本 | 说明 |
|------|------|------|
| **语言** | TypeScript 5.3+ | 类型安全 |
| **构建** | Vite 5.0+ | 极速构建工具 |
| **框架** | 无 | 原生开发，零依赖 |
| **样式** | CSS3 + CSS Variables | 响应式设计 |
| **网络** | Fetch API | 原生 HTTP 客户端 |
| **存储** | LocalStorage | 本地数据持久化 |
| **质量** | ESLint + Prettier | 代码规范 |

## 📁 项目结构

```
easyorange/
├── easyorange-backend/              # 后端服务
│   ├── easyorange-application/      # 应用层（启动类、控制器）
│   ├── easyorange-common/           # 公共模块
│   ├── easyorange-framework/        # 框架层
│   ├── easyorange-user/             # 用户域模块
│   ├── easyorange-product/          # 商品域模块
│   ├── easyorange-order/            # 订单域模块
│   ├── easyorange-payment/          # 支付域模块
│   ├── easyorange-message/          # 消息域模块
│   ├── pom.xml                      # Maven 父工程
│   └── init.sql                     # 数据库初始化脚本
│
├── easyorange-frontend/             # 前端应用
│   ├── src/
│   │   ├── api/                     # API 接口层
│   │   ├── app/                     # 应用配置
│   │   ├── components/              # 可复用组件
│   │   ├── pages/                   # 页面逻辑
│   │   ├── styles/                  # 样式文件
│   │   ├── types/                   # 类型定义
│   │   ├── utils/                   # 工具函数
│   │   └── main.ts                  # 入口文件
│   ├── index.html                   # 首页
│   ├── products.html                # 商品列表页
│   ├── profile.html                 # 个人中心页
│   ├── publish.html                 # 发布商品页
│   ├── favorites.html               # 收藏夹页
│   ├── messages.html                # 消息中心页
│   ├── orders.html                  # 订单管理页
│   ├── package.json
│   ├── vite.config.ts
│   └── tsconfig.json
│
├── docker-compose.yml               # Docker Compose 配置
└── README.md                        # 项目文档
```

## 🚀 快速开始

### 环境要求

- **JDK**: 25+
- **Node.js**: 16+
- **Maven**: 3.8+
- **Docker**: 20.10+ (可选，用于数据库容器化)

### 1. 克隆项目

```bash
git clone https://gitee.com/cartethyia_XLS/easy-orange.git
cd easy-orange
```

### 2. 启动基础设施

```bash
# 使用 Docker Compose 启动 MySQL 和 Redis
docker-compose up -d
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
EASYORANGE_DB_HOST=localhost
EASYORANGE_DB_PORT=3306
EASYORANGE_DB_NAME=easyorange
EASYORANGE_DB_USERNAME=easyorange_app
EASYORANGE_DB_PASSWORD=easyorange_app_dev

# Redis 配置
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# CORS 配置
CORS_ALLOWED_ORIGINS=http://localhost:5173,http://localhost:3000
```

#### 前端配置

```bash
cd easyorange-frontend
cp .env.development .env.local
```

### 4. 启动后端服务

```bash
cd easyorange-backend

# 编译并启动
./mvnw spring-boot:run

# 或者先编译再运行
./mvnw clean install
java -jar easyorange-application/target/easyorange-application-0.0.1-SNAPSHOT.jar
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

前端服务将在 `http://localhost:3000` 启动

## 📋 API 文档

启动后端服务后，访问以下地址查看 API 文档：

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs

### 核心 API

| 模块 | 端点 | 说明 |
|------|------|------|
| **认证** | `POST /api/auth/login` | 用户登录 |
| | `POST /api/auth/logout` | 用户登出 |
| | `POST /api/auth/refresh` | 刷新令牌 |
| **用户** | `POST /api/users/register` | 用户注册 |
| | `GET /api/users/profile` | 获取个人资料 |
| | `PUT /api/users/profile` | 更新个人资料 |
| **商品** | `GET /api/products` | 商品列表 |
| | `GET /api/products/{id}` | 商品详情 |
| | `POST /api/products` | 发布商品 |
| | `PUT /api/products/{id}` | 更新商品 |
| | `DELETE /api/products/{id}` | 删除商品 |
| | `GET /api/products/search` | 搜索商品 |
| **订单** | `GET /api/orders` | 订单列表 |
| | `POST /api/orders` | 创建订单 |
| | `GET /api/orders/{id}` | 订单详情 |
| **支付** | `POST /api/payment/pay` | 发起支付 |
| | `GET /api/payment/status` | 支付状态 |
| **消息** | `GET /api/messages` | 消息列表 |
| | `POST /api/messages` | 发送消息 |

## 🎯 功能模块

### 核心功能

#### 1. 用户系统
- ✅ 注册/登录（用户名密码）
- 🔜 手机号注册/登录（规划中）
- 🔜 邮箱注册/登录（规划中）
- ✅ JWT 令牌认证
- ✅ 个人资料管理
- ✅ 头像上传
- ✅ 密码找回

#### 2. 商品系统
- ✅ 商品发布（多图上传）
- ✅ 商品编辑/删除
- ✅ 商品搜索（关键词、分类）
- ✅ 商品筛选（价格、成色、排序）
- ✅ 商品详情展示
- ✅ 相似商品推荐
- ✅ 浏览历史记录

#### 3. 交易系统
- ✅ 订单创建
- ✅ 订单状态管理（待付款、待发货、已完成）
- ✅ 订单列表/详情
- ✅ 在线支付集成
- ✅ 评价系统

#### 4. 互动系统
- ✅ 商品收藏
- ✅ 站内消息
- ✅ 系统通知
- ✅ 活动页面

### 页面列表

| 页面 | 路由 | 功能 |
|------|------|------|
| 首页 | `/` | 推荐商品、轮播图、搜索入口 |
| 商品列表 | `/products.html` | 商品浏览、筛选、搜索 |
| 个人中心 | `/profile.html` | 个人信息、账户设置 |
| 发布商品 | `/publish.html` | 商品发布表单 |
| 收藏夹 | `/favorites.html` | 收藏商品管理 |
| 消息中心 | `/messages.html` | 站内消息列表 |
| 订单管理 | `/orders.html` | 订单列表和详情 |

## 🐳 Docker 部署

### 使用 Docker Compose

```bash
# 启动所有服务
docker-compose up -d

# 查看日志
docker-compose logs -f

# 停止服务
docker-compose down
```

### 环境变量配置

创建 `.env` 文件在项目根目录：

```env
# MySQL 配置
MYSQL_ROOT_PASSWORD=root123456
EASYORANGE_DB_NAME=easyorange
EASYORANGE_DB_USERNAME=easyorange_app
EASYORANGE_DB_PASSWORD=easyorange_app_dev

# Redis 配置
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# CORS 配置
CORS_ALLOWED_ORIGINS=http://localhost:5173,http://localhost:3000
```

## 📊 开发规范

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

#### 前端 (TypeScript)
- 使用 ESLint + Prettier
- 所有函数和变量必须有类型注解
- 优先使用 `const`
- 使用异步/等待处理异步操作
- 错误边界处理

### 分支策略

```
main          - 生产分支
develop       - 开发分支
feature/*     - 功能分支
bugfix/*      - Bug 修复分支
release/*     - 发布分支
hotfix/*      - 紧急修复分支
```

## 🧪 测试

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

# 运行 E2E 测试
npm run test:e2e

# 类型检查
npm run typecheck

# 代码检查
npm run lint
```

## 📈 性能优化

### 后端优化
- HikariCP 连接池配置优化
- Redis 缓存热点数据
- 数据库索引优化
- 异步线程池处理耗时操作
- 日志异步输出

### 前端优化
- 代码分割（按页面）
- 图片懒加载（IntersectionObserver）
- 骨架屏加载动画
- 防抖节流优化
- DocumentFragment 批量 DOM 操作
- CSS 变量复用

## 🔒 安全特性

- ✅ JWT 无状态认证
- ✅ 密码 BCrypt 加密存储
- ✅ SQL 注入防护（参数化查询）
- ✅ XSS 防护（输入验证）
- ✅ CSRF 防护
- ✅ CORS 跨域配置
- ✅ 请求频率限制
- ✅ 敏感数据脱敏

## 🤝 贡献指南

欢迎贡献代码！请遵循以下步骤：

1. Fork 本项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 提交 Pull Request

### 开发环境搭建

详见 [DEVELOPMENT.md](./DEVELOPMENT.md)

## 📄 许可证

MIT License - 详见 [LICENSE](LICENSE) 文件

## 👥 团队

- **开发者**: cartethyia
- **联系方式**: support@easyorange.com
- **项目地址**: https://gitee.com/cartethyia_XLS/easy-orange

## 📝 更新日志

### v0.0.1 (2024-03)
- ✨ 初始版本发布
- ✨ 用户注册/登录功能
- ✨ 商品发布和浏览
- ✨ 订单管理系统
- ✨ 基础支付集成

## 🙏 致谢

感谢以下开源项目：

- [Spring Boot](https://spring.io/projects/spring-boot)
- [MyBatis-Plus](https://baomidou.com/)
- [Vite](https://vitejs.dev/)
- [TypeScript](https://www.typescriptlang.org/)

---

<div align="center">

**EasyOrange** - 让闲置流转，让价值延续

[🔝 返回顶部](#easyorange-校园二手交易平台)

</div>
