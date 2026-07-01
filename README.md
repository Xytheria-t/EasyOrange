# EasyOrange — 砍业务,撑架构

> **副标：业务做减法,架构做加法**
>
> 选 C2C 资产流转作为业务容器,在真实场景中完整落地 DDD 战术模式、CQRS 命令/查询分离、Saga 分布式事务、RabbitMQ 领域事件、Spring AMQP、Elasticsearch、Redis 多级缓存,以及 DeepSeek + 通义千问 VL 的多模态 AI 集成。
>
> **2025 年 11 月启动开发**

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-25-ED8B00)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.3-6DB33F)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-19-61DAFB)](https://react.dev/)

## 项目定位

**EasyOrange 是一个面向 AI 工程实践的领域驱动设计与事件驱动架构 demo。**

> 副标：**砍业务,撑架构。**

项目选 C2C 资产流转作为业务容器,是因为这个场景具备:

- **复杂领域模型** — 商品 / 订单 / 支付 / 消息 / 收藏 / 信用,聚合根、值对象、领域事件丰富
- **跨模块事务** — 订单创建 → 库存锁定 → 支付 → 发货,Saga 模式天然落地
- **实时通信需求** — 聊天 / 通知 / 信用变化,WebSocket + STOMP 协议验证
- **AI 落地空间** — 智能估值 / 营销文案 / 智能找货 / 物品评估 / 信用画像,多模态集成充分
- **CQRS 适用面** — 商品读多写少 + 搜索聚合,命令/查询分离收益明显

业务场景本身被刻意简化:**资产方按固定价格发布商品,认领方浏览下单,平台不碰货、不囤货、不经手资金,C2C 直发**。这样能确保把全部精力放在架构与工程上。

> 业务是容器,架构才是主角。

## 技术亮点

| 维度 | 落地内容 |
|---|---|
| **架构模式** | DDD 战术模式 (聚合根 / 值对象 / 领域事件 / 端口-适配器) + 六边形架构 |
| **CQRS** | 命令端 MyBatis-Plus 写库,查询端独立 ReadModel + 全文搜索聚合 |
| **Saga** | 订单创建 / 取消 / 完成 / 退款 全链路分布式事务补偿 |
| **事件驱动** | RabbitMQ Topic Exchange + 9 个消费者队列 + DLQ + 指数退避重试 |
| **AI 集成** | DeepSeek 文本 + 通义千问 VL 多模态 + L1/L2 缓存装饰器 + 限流 + 异常降级 |
| **缓存** | Redis 多级缓存 (Caffeine + Redis) + 一致性哈希 + 布隆过滤器 |
| **搜索** | Elasticsearch 8 + IK 中文分词器 + 索引管理 + 全量重建 |
| **实时通信** | STOMP over WebSocket + JWT 认证 + 离线消息重推 |
| **数据库** | MySQL 8.4 + Flyway 迁移 + 16 张业务表 + 完整种子数据 |
| **测试** | JUnit 5 + Mockito + AssertJ + ArchUnit 架构守卫 |
| **前端** | React 19 + TypeScript + Vite 8 + Zustand 5 + React Query 5 + shadcn/ui + 管理端暖橙指挥中心设计系统 |

## 技术栈

| 层 | 技术 |
|---|------|
| **后端** | Java 25, Spring Boot 4.0.3, MyBatis-Plus 3.5.16, Spring Security + JWT |
| **前端** | React 19, TypeScript 5, Vite 8, TanStack Query 5, Zustand 5, Tailwind CSS 4, shadcn/ui |
| **数据库** | MySQL 8.4, Redis 7.4, Elasticsearch 8 (可选) |
| **消息队列** | RabbitMQ 3.13 (Spring AMQP 4.0.x) |
| **AI** | DeepSeek (文本), 通义千问 VL (视觉) |
| **认证** | JWT (Access + Refresh Token) |
| **迁移** | Flyway 11.15.0 |
| **部署** | Docker, docker-compose, compose.yaml (@ServiceConnection) |

## 快速开始

### 环境要求

- JDK 25+, Node.js 18+, Maven 3.8+
- Docker 20.10+ (可选)
- MySQL 8.0+ / Redis 7.0+ (本地或 Docker)

### 三步启动

```bash
# 1. 克隆
git clone https://gitee.com/cartethyia_XLS/easy-orange.git && cd easy-orange

# 2. 启动基础设施 (MySQL 8.4 + Redis 7.4 + RabbitMQ 3.13)
docker compose -f compose.yaml up -d

# 3. 安装依赖并启动
./mvnw install -DskipTests          # 后端
cd easyorange-frontend && npm install && npm run dev  # 前端 :5173
cd .. && ./mvnw spring-boot:run -pl easyorange-application  # 后端 :8080
```

> 项目支持**零配置启动**,MySQL/Redis 使用默认端口即可运行。详见 [CLAUDE.md](./CLAUDE.md) 和 [AGENTS.md](./AGENTS.md)。

## 后端模块划分 (11 个 Maven 模块)

| 模块 | 职责 |
|------|------|
| `easyorange-common` | 通用组件 (Result, PageResult, 注解, 异常, Money 值对象) |
| `easyorange-framework` | 框架基础设施 (Security, Redis, 多级缓存, Bloom 过滤器, AOP, 事件, 文件, 分布式 ID, 一致性哈希, **RabbitMQ 消息队列**) |
| `easyorange-user` | 用户模块 (DDD: 认证/注册/密码管理/个人资料/信用) |
| `easyorange-product` | 商品模块 (DDD + CQRS + 审核工作流 + 举报 + 全文搜索) |
| `easyorange-order` | 订单模块 (DDD + CQRS + Saga 补偿) |
| `easyorange-payment` | 支付模块 (DDD + CQRS) |
| `easyorange-message` | 消息模块 (DDD + WebSocket + Repository 模式) |
| `easyorange-favorite` | 收藏模块 (DDD 六边形架构) |
| `easyorange-ai` | AI 模块 (Port/Adapter + LLM + Embedding + Vision + 限流 + 缓存) |
| `easyorange-admin` | 管理端模块 (用户/商品/订单/分类/举报管理 API) |
| `easyorange-application` | 应用启动入口 + Flyway + 架构测试 + ES 搜索适配器 |

## AI 能力清单

业务侧:资产方侧 3 个 AI 能力 (智能估值 / AI 营销文案 / 信用画像) + 认领方侧 3 个 AI 能力 (AI 智能找货 / AI 物品评估 / 信用画像)。**架构侧关注点**是 LLM/Vision 的端口抽象、多级缓存、限流降级、缓存装饰器,详见 [doc/集成/AI-资产管理.md](doc/集成/AI-资产管理.md)。

## Docker 部署

```bash
docker compose -f compose.yaml up -d       # 启动全部服务 (MySQL/Redis/RabbitMQ)
docker compose up -d elasticsearch        # 可选: ES 搜索 (需先构建镜像)
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
├── doc/架构/               # 架构规范文档
├── doc/集成/               # 业务专题 + API 速查
├── PRODUCT_DIRECTION.md    # 业务场景说明 (非商业模式)
├── CLAUDE.md               # AI 项目指南
├── AGENTS.md               # Agent 使用说明 (含完整 API 清单)
└── CHANGELOG.md            # 更新日志
```

## 开发规范

- Conventional Commits (`feat/fix/docs/refactor/chore`)
- 分支策略: `main` / `develop` / `feature/*` / `bugfix/*`
- 代码风格: Google Java Style + Biome (前端, 替代 ESLint + Prettier)
- 测试: 后端 JUnit 5 (1,269 用例) + 前端 Vitest/Playwright (953 用例)
- 架构守卫: ArchUnit (`ArchitectureRulesTest`)

## 贡献指南

1. Fork → 2. 创建分支 → 3. 提交 → 4. Push → 5. Pull Request

## 许可证

MIT License — 详见 [LICENSE](LICENSE) 文件

---

<div align="center">

**EasyOrange** · DDD + CQRS + Saga + 事件驱动 + AI 多模态 全栈架构 demo · [Gitee](https://gitee.com/cartethyia_XLS/easy-orange) · [更新日志](./CHANGELOG.md)

</div>
