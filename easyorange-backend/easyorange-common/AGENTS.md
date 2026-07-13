# easyorange-common 模块指南

通用基础组件，提供全项目共享的类型定义、工具和抽象。

## 目录结构

```
common/
├── annotation/          # 自定义注解
│   ├── SkipRateLimit.java      # 跳过限流（配合 RateLimitFilter 使用）
│   ├── SkipRepeatSubmit.java   # 跳过防重提交（配合 RateLimitFilter 使用）
│   └── Idempotent.java         # Idempotency-Key 协议级幂等（@Aspect 实现）
├── constant/
│   └── CommonConstant.java  # 全局常量
├── dto/
│   └── PageRequest.java     # 分页请求基类
├── security/
│   └── AuthUser.java        # 认证用户信息 (Security Principal)
├── enums/
│   ├── IResultCode.java     # 结果码接口
│   ├── ResultCode.java      # 通用结果码枚举
│   ├── BusinessType.java    # 操作业务类型
│   ├── FileResultCode.java  # 文件操作结果码
│   └── LimitType.java       # 限流类型
├── entity/
│   └── BaseDO.java          # 数据对象基类 (id, createTime, updateTime, delFlag, version)
├── repository/
│   └── BaseRepository.java  # 仓储基类 (lambdaQuery/lambdaUpdate + 常见查询模式)
├── idgen/
│   └── IdGenerator.java     # 分布式 ID 生成器接口 (@FunctionalInterface)
├── event/
│   ├── DomainEvent.java          # 领域事件接口（事件类应为此接口的 record 实现）
│   ├── DomainEventPublisher.java # 领域事件发布接口
├── exception/
│   ├── BaseBusinessException.java       # 业务异常基类
│   ├── BusinessException.java           # 通用业务异常
│   ├── ConcurrentUpdateException.java   # 并发更新冲突异常
│   ├── file/
│   │   ├── FileException.java               # 文件操作异常
│   │   ├── FileSizeLimitExceededException.java # 文件大小超限
│   │   └── InvalidExtensionException.java     # 非法扩展名
│   └── validation/
│       └── ParamValidationException.java     # 参数校验异常
├── result/
│   ├── Result.java          # 统一响应 Result<T>
│   └── PageResult.java      # 分页响应 PageResult<T>
└── util/
    ├── BizRequire.java          # 业务断言工具
    ├── MaskUtils.java           # 数据脱敏
    └── FileSizeFormat.java      # 文件大小格式化
```

## 核心类型使用方式

### Result — 统一 API 响应

```java
Result.success(data)
Result.fail(ResultCode.PARAM_VALIDATION_FAILED)
Result.fail("CUSTOM_CODE", "自定义消息")
```

### PageResult — 分页响应

```java
PageResult.of(records, total, page, size)
```

### BizRequire — 业务断言

```java
BizRequire.notNull(user, UserResultCode.USER_NOT_FOUND);
BizRequire.notBlank(name, "用户名已存在");
BizRequire.notEmpty(items, "订单资产不能为空");
BizRequire.requireTrue(condition, ResultCode.PARAM_VALIDATION_FAILED);
BizRequire.requireTrue(condition, "条件不满足");
```

### DomainEvent — 领域事件接口

### 自定义注解

- `@SkipRateLimit` — 跳过当前方法/类的限流（`RateLimitFilter` 命中规则时检查）
- `@SkipRepeatSubmit` — 跳过当前方法/类的防重提交（`RateLimitFilter` 写方法时自动检查）
- `@Idempotent(headerName, ttlSeconds)` — Idempotency-Key 协议级幂等。客户端提供幂等 key 头（默认 `Idempotency-Key`），服务端缓存成功响应，相同 key 的重复请求直接返回缓存结果。`@Idempotent` 与 `@SkipRepeatSubmit` 互补：前者是 24h 协议级幂等（含响应缓存），后者是 3s 短时间防连点

> 审计日志（AuditLog）为约定式自动记录，无需注解。所有非查询类 RestController 方法自动记录。

## 注意事项

- 本模块应保持轻量，禁止引入 Spring Boot Starter 或重量级框架依赖
- 异常类必须关联 `IResultCode`，确保错误码统一
- `FileException` 构造器为 `protected`，统一使用 `FileException.of(...)` 工厂方法（与 `BusinessException` 一致）
- 新增通用类型前确认是否真的跨模块共享，避免 common 模块膨胀
