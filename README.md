# EasyOrange — AI 替卖家运营的 C2C 平台

> 设一个底价，AI 替你议价、改价、撮合。卖家全程不需要在线。
>
> **2025 年 11 月启动开发**

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-25-ED8B00)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.3-6DB33F)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-19-61DAFB)](https://react.dev/)

## 项目简介

面向 C2C 二手交易场景的 AI 替卖家运营平台，采用 DDD 模块化架构 + 前后端分离设计。

### 核心特性

- **AI 驱动** — AI 替卖家运营：智能定价、议价、改价、撮合
- **DDD 架构** — 后端 11 个 Maven 模块，高内聚低耦合
- **AI 赋能** — 智能定价、拍照上架、AI 审核、语义搜索、信用评分
- **事件驱动** — RabbitMQ 领域事件，10 个跨模块消费者，路由键自动派生
- **完整生态** — 商品审核工作流、举报处理、Saga 事务补偿
- **AI 替卖家运营** — 卖家设底价，AI 替你议价 / 改价 / 撮合 / 阶梯降价

## 技术栈

| 层 | 技术 |
|---|------|
| **后端** | Java 25, Spring Boot 4, MyBatis-Plus, Spring Security + JWT |
| **前端** | React 19, TypeScript, Vite 8, TanStack Query 5, Zustand 5, Tailwind CSS 4 |
| **数据库** | MySQL 8.4, Redis 7.4, Elasticsearch 8 (可选) |
| **消息队列** | RabbitMQ 3.13 (Spring AMQP) |
| **AI** | DeepSeek (文本), 通义千问 VL (视觉) |

## 快速开始

### 环境要求

- JDK 25+, Node.js 18+, Maven 3.8+
- Docker 20.10+ (可选)

### 三步启动

```bash
# 1. 克隆
git clone https://gitee.com/cartethyia_XLS/easy-orange.git && cd easy-orange

# 2. 启动基础设施 (MySQL + Redis)
docker-compose up -d

# 3. 安装依赖并启动
./mvnw install -DskipTests          # 后端
cd easyorange-frontend && npm install && npm run dev  # 前端 :5173
cd .. && ./mvnw spring-boot:run -pl easyorange-application  # 后端 :8080
```

> 项目支持**零配置启动**，MySQL/Redis 使用默认端口即可运行。详见 [CLAUDE.md](./CLAUDE.md) 和 [AGENTS.md](./AGENTS.md)。

## 核心功能

| 模块 | 功能 |
|------|------|
| **用户** | 注册/登录 (JWT)、个人资料、密码管理、信用评分 |
| **商品** | 发布 (多图上传)、编辑、搜索 (全文/语义)、审核工作流、举报 |
| **交易** | 订单 (Saga 补偿)、支付、评价 |
| **互动** | 收藏、站内消息、实时聊天 (WebSocket) |
| **AI** | 智能定价、拍照上架、AI 审核、语义搜索、智能问答、智能文案、**智能导购搜索** |
| **管理端** | 用户/商品/订单/分类/评价/举报/统计 CRUD |

详细 API 文档见 [AGENTS.md](./AGENTS.md)，数据库设计见 [DATABASE.md](./DATABASE.md)。

## AI 替卖家运营模式

设一个底价，AI 替你议价、改价、撮合，卖家全程不需要在线。

- **智能定价**: AI 分析商品信息给出建议价 + 底价
- **AI 议价**: 规则引擎毫秒级决策 + LLM 自然话术，7×24 自动响应买家出价
- **阶梯降价**: 上架时间越长价格自动阶梯下调，直到底价
- **自动成交**: AI 接受出价后自动创建订单，超时取消商品回池

## Docker 部署

```bash
docker-compose up -d           # 启动全部服务
docker compose up -d elasticsearch  # 可选: ES 搜索
```

前端容器化:

```bash
cd easyorange-frontend && docker build -t easyorange-frontend . && docker run -p 80:80 easyorange-frontend
```

## 项目结构

```
easy-orange/
├── easyorange-backend/     # Spring Boot (11 Maven 模块, DDD 六边形架构)
├── easyorange-frontend/    # React SPA (Vite + TypeScript)
├── easyorange-shared/      # 共享类型定义
├── easyorange-miniprogram/ # 微信小程序 (规划中)
├── doc/架构/               # 架构规范文档
├── CLAUDE.md               # AI 项目指南
├── AGENTS.md               # Agent 使用说明 (含完整 API 清单)
└── CHANGELOG.md            # 更新日志
```

## 开发规范

- Conventional Commits (`feat/fix/docs/refactor/chore`)
- 分支策略: `main` / `develop` / `feature/*` / `bugfix/*`
- 代码风格: Google Java Style + ESLint/Prettier
- 测试: 后端 JUnit 5 (2,692 用例) + 前端 Vitest/Playwright (945 用例)
- 架构守卫: ArchUnit (`ArchitectureRulesTest`)

## 贡献指南

1. Fork → 2. 创建分支 → 3. 提交 → 4. Push → 5. Pull Request

## 许可证

MIT License — 详见 [LICENSE](LICENSE) 文件

---

<div align="center">

**EasyOrange** · [Gitee](https://gitee.com/cartethyia_XLS/easy-orange) · [更新日志](./CHANGELOG.md)

</div>
