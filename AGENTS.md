# EasyOrange 项目指南

EasyOrange 是基于 Spring Boot 4 + React 的全栈二手交易平台。

## 技术栈

| 层 | 技术 |
|---|------|
| 后端 | Java 25, Spring Boot 4.0.3, MyBatis-Plus 3.5.16 |
| 前端 | TypeScript, React |
| 数据库 | MySQL 8.0, Redis 7 |
| 认证 | JWT (Access + Refresh Token) |
| 迁移 | Flyway 11.14.1 |
| 部署 | Docker, docker-compose |

## 项目结构

```
easy-orange/
├── easyorange-backend/          # Spring Boot 后端
│   ├── easyorange-common/       # 通用组件 (Result, PageResult, 注解, 异常)
│   ├── easyorange-framework/    # 框架基础设施 (Security, Redis, 事件, AOP)
│   ├── easyorange-user/         # 用户模块 (DDD)
│   ├── easyorange-product/      # 商品模块 (DDD + CQRS)
│   ├── easyorange-order/        # 订单模块 (DDD + CQRS + Saga)
│   ├── easyorange-payment/      # 支付模块 (DDD + CQRS + Outbox)
│   ├── easyorange-message/      # 消息模块 (混合架构 + WebSocket)
│   ├── easyorange-favorite/     # 收藏模块 (DDD + ACL)
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

## 模块依赖关系

```
application → framework, user, product, order, payment, message, favorite
framework → common
user → framework
product → framework, user (需通过 ACL 演进消除)
order → framework, product, user, payment (需通过 Port/ACL 演进消除)
payment → framework
message → framework, user (需通过 ACL 演进消除)
favorite → framework, product (已通过 ProductAclService 隔离)
```

## 开发规范

- 编码规则见 `.trae/rules/` 目录
- 架构守卫测试: `ArchitectureRulesTest.java` (ArchUnit)
- 数据库变更必须通过 Flyway 迁移脚本
- 所有 API 统一返回 `Result<T>`，分页返回 `PageResult<T>`
- 测试覆盖率目标 ≥ 80%

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
