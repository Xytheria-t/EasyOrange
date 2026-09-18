# easyorange-common 模块指南

通用基础组件，提供全项目共享的类型定义、工具和抽象。

## 目录结构

```
common/
├── annotation/          # 自定义注解
│   ├── SkipRateLimit.java      # 跳过限流（配合 RateLimitFilter 使用）
│   └── SkipRepeatSubmit.java   # 跳过防重提交（配合 RateLimitFilter 使用）
├── constant/CommonConstant.java
├── domain/
│   ├── Money.java           # 金额值对象（record，精度 2 HALF_UP，不可变四则运算）
│   └── ProductId.java       # 商品 ID 值对象（record，@JsonValue/@JsonCreator）
├── dto/
│   ├── PageRequest.java     # 分页请求基类
│   └── AiEnhancement.java   # 搜索增强结果（intentExplanation / productTags / marketAnalysis / suggestedQuestions）
├── entity/BaseDO.java       # 数据对象基类 (id, createTime, updateTime, delFlag)
├── enums/
│   ├── BaseCodeEnum.java    # code 枚举公共接口（统一 fromCode 查找，未匹配 fail-fast）
│   ├── IResultCode.java     # 结果码接口
│   ├── ResultCode.java      # 通用结果码枚举
│   ├── BusinessType.java    # 操作业务类型
│   ├── FileResultCode.java  # 文件操作结果码
│   └── LimitType.java       # 限流类型
├── event/
│   ├── DomainEvent.java          # 领域事件接口（事件类应为此接口的 record 实现）
│   ├── DomainEventPublisher.java # 领域事件发布接口
│   └── Transition.java           # record<T, E extends DomainEvent>（聚合根新实例 + 领域事件）
├── exception/
│   ├── BaseBusinessException.java / BusinessException.java / ConcurrentUpdateException.java
│   ├── file/（FileException / FileSizeLimitExceededException / InvalidExtensionException）
│   └── validation/ParamValidationException.java
├── idgen/
│   ├── IdGenerator.java     # 分布式 ID 生成器接口 (@FunctionalInterface)
│   └── UuidV7.java          # UUID v7 生成（generateId()）
├── repository/BaseRepository.java   # 仓储基类 (lambdaQuery/lambdaUpdate + 常见查询模式)
├── result/Result.java, PageResult.java
├── security/AuthUser.java   # 认证用户信息 (Security Principal)
└── util/
    ├── BizRequire.java          # 业务断言工具
    ├── MaskUtils.java           # 数据脱敏
    └── FileSizeFormat.java      # 文件大小格式化
```

## BizRequire — 业务断言

```java
BizRequire.notNull(user, UserResultCode.USER_NOT_FOUND);
BizRequire.notBlank(name, "用户名已存在");
BizRequire.notEmpty(items, "订单资产不能为空");
BizRequire.requireTrue(condition, ResultCode.PARAM_VALIDATION_FAILED);
BizRequire.requireTrue(condition, "条件不满足");
```

### 自定义注解

- `@SkipRateLimit` — 跳过当前方法/类的限流（`RateLimitFilter` 命中规则时检查）
- `@SkipRepeatSubmit` — 跳过当前方法/类的防重提交（`RateLimitFilter` 写方法时自动检查）

> 协议级幂等（Idempotency-Key）由 framework 的 `IdempotencyKeyFilter` 约定式处理，common 不提供幂等注解。

## 注意事项

- 本模块应保持轻量，禁止引入 Spring Boot Starter 或重量级框架依赖
- `FileException` 构造器为 `protected`，统一使用 `FileException.of(...)` 工厂方法
- 新增通用类型前确认是否真的跨模块共享，避免 common 模块膨胀
