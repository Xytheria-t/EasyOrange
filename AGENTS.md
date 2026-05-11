# EasyOrange 项目指南

EasyOrange 是基于 Spring Boot 4 + React 的全栈二手交易平台。

## 技术栈

| 层 | 技术 |
|---|------|
| **后端** | Java 25, Spring Boot 4.0.3, MyBatis-Plus 3.5.16 |
| **前端** | TypeScript, React |
| **数据库** | MySQL 8.0, Redis 7 |
| **认证** | JWT (Access + Refresh Token) |
| **迁移** | Flyway 11.14.1 |
| **部署** | Docker, docker-compose |

## 项目结构

```
easy-orange/
├── easyorange-backend/          # Spring Boot 后端 (12 Maven 模块)
│   ├── easyorange-common/       # 通用组件 (Result, PageResult, 注解, 异常)
│   ├── easyorange-framework/    # 框架基础设施 (Security, Redis, 事件, AOP)
│   ├── easyorange-user/         # 用户模块 (DDD)
│   ├── easyorange-product/      # 商品模块 (DDD + CQRS)
│   ├── easyorange-order/        # 订单模块 (DDD + CQRS + Saga)
│   ├── easyorange-payment/      # 支付模块 (DDD + CQRS + Outbox)
│   ├── easyorange-message/      # 消息模块 (DDD + WebSocket)
│   ├── easyorange-favorite/     # 收藏模块 (DDD 六边形架构)
│   ├── easyorange-admin/        # 管理端模块 (独立模块，24 个管理 API)
│   └── easyorange-application/  # 应用启动入口 + Flyway + 架构测试
├── easyorange-frontend/         # React 前端
├── doc/                         # 项目文档
└── .trae/rules/                 # AI 编码规则
```

## 后端架构核心原则

1. **DDD 分层**: domain → application → adapter，依赖方向单向向内
2. **CQRS**: 命令与查询分离 (product, order, payment 模块)
3. **六边形架构**: domain 层通过 port 接口与外部解耦
4. **不可变性**: 聚合根用 `@Builder(toBuilder = true)`，值对象用 `record`
5. **领域事件**: `@PublishEvent` 注解 + AOP 切面发布
6. **ACL 隔离**: 跨模块通过 ACL/Port 适配，禁止直接依赖领域模型
7. **异常继承**: 领域异常必须继承 `BaseBusinessException`（common 模块），`GlobalExceptionHandler` 已有统一处理器返回 400 + 业务错误码；**禁止直接抛出非 `BaseBusinessException` 子类的 RuntimeException**，否则会落入 500 兜底

## 模块依赖关系

```
application → framework, user, product, order, payment, message, favorite
framework → common
user → framework
product → framework, user (通过 SellerInfoPort 隔离，optional)
order → framework, product, user, payment (通过 Port 接口隔离，optional)
payment → framework
message → framework, user (通过 UserInfoPort 隔离，optional)
favorite → framework, product (通过 ProductInfoPort 隔离，optional)
```

> **状态 (2026-05-09)**：所有跨模块依赖已通过端口接口 + 适配器模式隔离，Maven 依赖标记为 `<optional>true</optional>`。写操作通过事件驱动解耦，查询操作保留同步端口调用。

## 已知问题

（暂无）

## 错误码规范

错误码采用 **A/B/C/D 前缀格式**：

| 前缀 | 类型 | HTTP 状态 | 示例 |
|------|------|----------|------|
| A | 成功/客户端语义 | 200/4xx | A0000=成功, A0401=未登录, A0403=禁止访问 |
| B | 业务错误 | 400 | B0001=操作失败, B0002=业务异常 |
| C | 系统错误 | 500 | C0500=服务器内部错误 |
| D | 第三方错误 | 502 | D0502=上游服务不可用 |

判断成功：`"A0000".equals(code)`

## 架构文档

详细架构规范见 `doc/规范/` 目录：

| 文档 | 内容 |
|------|------|
| [架构.md](doc/架构/架构.md) | 主索引文档 |
| [架构-技术栈.md](doc/架构/架构-技术栈.md) | 技术选型 |
| [架构-系统架构.md](doc/架构/架构-系统架构.md) | 整体架构、模块划分 |
| [架构-模块结构.md](doc/架构/架构-模块结构.md) | 包结构规范 |
| [架构-DDD规范.md](doc/架构/架构-DDD规范.md) | DDD 设计规范 |
| [架构-安全认证.md](doc/架构/架构-安全认证.md) | JWT 认证 |
| [架构-数据库迁移.md](doc/架构/架构-数据库迁移.md) | Flyway 规范 |
| [架构-部署演进.md](doc/架构/架构-部署演进.md) | 部署与演进 |

## 开发规范

- 编码规则见 `.trae/rules/` 目录
- 架构守卫测试: `ArchitectureRulesTest.java` (ArchUnit)
- 数据库变更必须通过 Flyway 迁移脚本
- 所有 API 统一返回 `Result<T>`，分页返回 `PageResult<T>`
- 测试覆盖率目标 ≥ 80%
- **Snowflake ID**: 后端 Long 主键通过 Jackson 2.x `ObjectMapper` 和 Jackson 3.x `JsonMapper` 的 `ToStringSerializer` 序列化为字符串；前端所有实体 ID 字段类型为 `string`，禁止使用 `number`（防止 JS 精度丢失）
- **React Query 缓存**: mutation 后 `invalidateQueries` 必须使用 `ORDER_KEYS.all` 前缀匹配，确保 myOrders/soldOrders/detail 等所有查询都能被正确失效

## 常用命令

```bash
# 后端构建
cd easyorange-backend && ./mvnw clean package -DskipTests

# 运行测试
./mvnw test

# 启动开发环境 (MySQL + Redis)
docker-compose up -d

# 启动后端
./mvnw spring-boot:run -pl easyorange-application
```
