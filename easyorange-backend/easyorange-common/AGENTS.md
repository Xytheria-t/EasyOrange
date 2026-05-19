# easyorange-common 模块指南

通用基础组件，提供全项目共享的类型定义、工具和抽象。

## 目录结构

```
common/
├── annotation/          # 自定义注解
│   ├── Log.java             # 操作日志标记
│   ├── RateLimiter.java     # 限流 (Redis + Lua 滑动窗口)
│   └── RepeatSubmit.java    # 防重复提交
├── constant/
│   └── CommonConstant.java  # 全局常量
├── dto/
│   ├── AuthUser.java        # 认证用户信息
│   └── PageRequest.java     # 分页请求基类
├── enums/
│   ├── IResultCode.java     # 结果码接口
│   ├── ResultCode.java      # 通用结果码枚举
│   ├── BusinessType.java    # 操作业务类型
│   ├── FileResultCode.java  # 文件操作结果码
│   └── LimitType.java       # 限流类型
├── event/
│   ├── BaseDomainEvent.java     # 领域事件基类
│   ├── DomainEventPublisher.java # 领域事件发布接口
│   └── DomainEventSubscriber.java # 领域事件订阅接口
├── exception/
│   ├── BaseBusinessException.java       # 业务异常基类
│   ├── BusinessException.java           # 通用业务异常
│   ├── file/
│   │   ├── FileException.java               # 文件操作异常
│   │   ├── FileSizeLimitExceededException.java # 文件大小超限
│   │   └── InvalidExtensionException.java     # 非法扩展名
│   └── validation/
│       └── ParamValidationException.java     # 参数校验异常
├── notification/
│   └── NotificationService.java # 通知服务接口
├── result/
│   ├── Result.java          # 统一响应 Result<T>
│   └── PageResult.java      # 分页响应 PageResult<T>
└── util/
    ├── BizRequire.java          # 业务断言工具
    ├── MaskUtils.java           # 数据脱敏
    ├── FileSizeFormat.java      # 文件大小格式化
    └── SnowflakeIdGenerator.java # 雪花ID生成器
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
BizRequire.isTrue(condition, ResultCode.PARAM_VALIDATION_FAILED, "消息");
```

### BaseDomainEvent — 领域事件基类

```java
public class UserRegisteredEvent extends BaseDomainEvent {
    private final Long userId;
    private final String username;
}
```

### 自定义注解

- `@Log(title = "操作名称", businessType = BusinessType.INSERT)` — 自动记录操作日志
- `@RateLimiter(count = 10, time = 60)` — 接口限流
- `@RepeatSubmit(interval = 3000)` — 防重复提交

## 注意事项

- 本模块应保持轻量，禁止引入 Spring Boot Starter 或重量级框架依赖
- 异常类必须关联 `IResultCode`，确保错误码统一
- 新增通用类型前确认是否真的跨模块共享，避免 common 模块膨胀
