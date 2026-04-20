# easyorange-common

**Module:** Shared utilities, annotations, Result<T> wrapper

## OVERVIEW

Cross-cutting shared code: custom annotations (AOP-driven), unified API response wrapper, exception hierarchy, business enums, constants.

## STRUCTURE

```
easyorange-common/src/main/java/com/cartethyia/easyorange/common/
├── annotation/          # @Log, @RateLimiter, @RepeatSubmit (AOP targets)
├── constant/            # Shared constants (CacheConstants, Constants, etc.)
├── dto/                 # Shared DTOs (BaseEntity, etc.)
├── entity/              # Shared entity base classes
├── enums/               # BusinessType, LimitType, UserStatus, etc.
├── exception/           # ServiceException, base exception classes
├── result/              # Result<T> unified API response wrapper
└── util/                # Shared utilities
```

## WHERE TO LOOK

| Task | Location | Notes |
|------|----------|-------|
| Result wrapper | `result/Result.java` | `Result.success()`, `Result.error()`, all controllers return this |
| Custom annotations | `annotation/` | @Log (操作日志), @RateLimiter (限流), @RepeatSubmit (防重) |
| Exception hierarchy | `exception/` | ServiceException 为业务异常基类 |
| Business enums | `enums/` | BusinessType (CRUD/OTHER), LimitType (DEFAULT/IP/CLUSTER) |
| Constants | `constant/` | 缓存键前缀、通用常量 |

## CONVENTIONS

- 所有 Controller 返回 `Result<T>`，不使用原始类型
- 业务异常抛 `ServiceException`，不要抛 RuntimeException
- 逻辑删除值：0=未删除，2=已删除（MyBatis-Plus 全局配置）

## DEPENDENCIES

```
所有业务模块 → easyorange-common
easyorange-common → 无业务依赖（仅 Lombok、Jackson、Validation）
```

## COMMANDS

```bash
mvn clean install -pl easyorange-common
```
