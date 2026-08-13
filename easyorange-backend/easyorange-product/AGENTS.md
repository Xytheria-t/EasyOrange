# easyorange-product 模块指南

商品管理模块，DDD + CQRS 架构，支持商品 CRUD、搜索、库存、分类、举报、评价。

## 目录结构

```
product/
├── adapter/
│   ├── inbound/web/
│   │   ├── controller/
│   │   │   ├── ProductController.java          # 商品 CRUD + 命令 + 查询 (含库存/状态)
│   │   │   ├── ProductSearchController.java    # 搜索 (关键词/历史/热词)
│   │   │   ├── ProductReportController.java    # 举报管理
│   │   │   └── ProductRatingController.java    # 评价管理
│   │   ├── dto/request/
│   │   └── dto/response/
│   └── outbound/
│       ├── persistence/                 # 持久化 — 按领域聚合分组
│       │   ├── product/                 # 商品聚合：ProductDO + DetailDO + ImageDO + Mapper + RepositoryImpl + DataMapper + SnapshotPortImpl
│       │   ├── category/                # 分类：CategoryDO + CategoryProductCount + Mapper + QueryRepositoryImpl
│       │   ├── audit/                   # 审核日志：ProductAuditLogDO + Mapper + RepositoryImpl
│       │   ├── report/                  # 举报：ProductReportDO + ReportHandleHistoryDO + Mapper × 2 + RepositoryImpl × 3
│       │   ├── rating/                  # 评价：ProductRatingDO + Mapper + RepositoryImpl × 2
│       │   ├── search/                  # 搜索：HotKeywordDO + SearchHistoryDO + Mapper × 2
│       │   └── typehandler/             # MyBatis 枚举 TypeHandler
│       ├── scheduler/                  # 定时批处理
│       │   └── ViewCountFlushScheduler.java  # 浏览量 Redis→DB 定时刷入
│       └── cache/                       # 缓存适配器
│           ├── ProductCacheAdapter.java     # 实现 ProductCachePort（extends ProductCacheEvictionPort）
│           ├── CategoryCacheAdapter.java    # 实现 CategoryCachePort
│           └── ProductCacheConstant.java
├── application/
│   ├── command/                         # 命令侧 (CQRS Write)
│   │   ├── ProductCommand.java               # 密封接口（所有命令实现此接口）
│   │   ├── CreateProductCommand.java         # 顶层 record
│   │   ├── UpdateProductCommand.java         # 顶层 record
│   │   ├── CreateProductRatingCommand.java   # 顶层 record
│   │   ├── ProductCommandHandler.java
│   │   ├── ProductReportCommandHandler.java
│   │   └── ProductRatingCommandHandler.java
│   ├── query/                           # 查询侧 (CQRS Read)
│   │   ├── ProductQueryHandler.java
│   │   ├── ProductReportQueryHandler.java
│   │   ├── ProductRatingQueryHandler.java
│   │   ├── ProductSearchQueryHandler.java
│   │   ├── ProductSearchCriteria.java
│   │   ├── CategoryQueryHandler.java
│   │   ├── dto/                         # 应用层输出 VO
│   │   │   ├── ProductVO.java
│   │   │   ├── ProductRatingVO.java
│   │   │   └── RatingStatsVO.java
│   │   ├── readmodel/                   # 读模型（Repository 返回）
│   │   │   ├── ProductReadModel.java
│   │   │   ├── CategoryReadModel.java
│   │   │   ├── SellerReadModel.java
│   │   │   ├── HotKeywordReadModel.java
│   │   │   └── SearchHistoryReadModel.java
│   │   └── assembler/
│   │       └── ProductReadModelAssembler.java
│   ├── port/                            # 应用层端口
│   │   ├── ProductCachePort.java        # 缓存端口（含 ProductVO 类型）
│   │   ├── CategoryCachePort.java       # 分类缓存端口
│   │   └── query/                       # 查询仓储
│   │       ├── CategoryQueryRepository.java    # 分类查询（返回 CategoryReadModel）
│   │       └── ProductRatingQueryRepository.java  # 评价查询
│   ├── event/
│   │   └── ProductEventConsumer.java     # RabbitMQ 领域事件消费者
│   ├── service/
│   │   ├── ProductViewCountAppService.java    # 浏览量 Redis 增量（仅写Redis）
│   │   ├── ViewCountBatchProcessor.java       # 浏览量批量刷入DB（@Transactional）
│   │   └── SearchHistoryBufferAppService.java
├── domain/
│   ├── aggregate/
│   │   └── Product.java                 # 商品聚合根
│   ├── entity/
│   │   ├── ProductAuditLog.java
│   │   ├── ProductDetail.java
│   │   ├── ProductRating.java
│   │   ├── ProductReport.java
│   │   └── ReportHandleHistory.java
│   ├── valueobject/
│   │   ├── CategoryId.java, SellerId.java
│   │   ├── StockQuantity.java, Version.java
│   │   ├── ProductTitle.java, ProductDescription.java
│   │   ├── ImageUrl.java, ImageSet.java, TagSet.java
│   │   ├── ContactMethod.java, TradeLocation.java
│   │   ├── SellerInfo.java
│   │   ├── Rating.java, ReviewContent.java
│   ├── event/
│   │   ├── ProductEvent.java                 # 密封接口（消除 aggregateId() 模板）
│   │   ├── ProductCreatedEvent.java
│   │   ├── ProductUpdatedEvent.java
│   │   ├── ProductDeletedEvent.java
│   │   ├── ProductSubmittedForReviewEvent.java
│   │   ├── ProductPutOnlineEvent.java
│   │   ├── ProductTakeOfflineEvent.java
│   │   ├── ProductMarkedSoldEvent.java
│   │   ├── ProductAuditedEvent.java
│   │   ├── StockDecreasedEvent.java
│   │   ├── StockRestoredEvent.java
│   │   └── ReportProcessedEvent.java
│   ├── port/
│   │   ├── ProductCacheEvictionPort.java # 缓存驱逐端口（domain 层，仅 evict）
│   │   ├── ProductSnapshotPort.java
│   │   ├── SellerInfoPort.java          # 资产方信息查询 (跨模块)
│   │   ├── ProductNotificationPort.java # 商品事件通知 (跨模块)
│   │   └── ProductSearchIndexPort.java  # 搜索索引 (跨模块)
│   ├── repository/
│   │   ├── ProductRepository.java       # 写仓储
│   │   ├── ProductReportRepository.java
│   │   └── ProductRatingRepository.java
│   ├── service/
│   │   └── ProductReportDomainService.java
│   ├── enums/
│   │   ├── ProductStatus.java, ConditionLevel.java
│   │   ├── ProductReportStatus.java
│   │   └── ProductResultCode.java
│   ├── constant/
│   │   └── ProductConstant.java
│   └── exception/
│       ├── InsufficientStockException.java
│       ├── InvalidProductStatusException.java
│       └── ProductNotFoundException.java
└── config/
    └── ProductDomainConfig.java
```

> **共享值对象**：`ProductId` 与 `Money` 位于 `easyorange-common`（`common/domain/`，2026-08-08 将两模块重复的 `ProductId` 收敛为单一实现，带 `@JsonValue`/`@JsonCreator`），本模块 valueobject 包不重复定义。

## 领域事件模式

商品领域事件统一实现 `ProductEvent` 密封接口（extends `DomainEvent`）：
- 所有事件共享 `String productId()` 作为聚合根标识
- 密封接口消除 ~22 行重复的 `aggregateId()` 模板代码
- 新增产品事件只需 `implements ProductEvent` 并定义组件即可，无需手动实现 `aggregateId()`

```java
public sealed interface ProductEvent extends DomainEvent
    permits ProductCreatedEvent, ProductUpdatedEvent, ..., ReportProcessedEvent {
    String productId();
    @Override default String aggregateId() { return productId(); }
}
```

## CQRS 架构

**Command 侧 (写)**:
`ProductController` → `ProductCommandHandler` → `Product` 聚合根 → `ProductRepository`

**Query 侧 (读)**:
`ProductController` → `ProductQueryHandler` → `ProductQueryRepository`（`application/port/query/`）→ `ProductReadModel`

读写使用不同的 Repository 接口和数据模型，查询侧使用 ReadModel 组装响应。查询 Repository 接口位于 `application/port/query/`（非 domain 层），因为查询 ReadModel 是 application 层的概念。

## 缓存端口模式

缓存端口按 DDD 双向依赖原则分拆：

- **domain 层**: `ProductCacheEvictionPort` — 仅 `evictProductCache(id)` / `evictProductListCache(categoryId)`，领域服务只做驱逐
- **application 层**: `ProductCachePort extends ProductCacheEvictionPort` — 继承驱逐方法，补充 get/set，供查询服务使用
- **adapter 层**: `ProductCacheAdapter` 仅实现 `ProductCachePort`（驱逐方法由继承关系提供）

```java
// domain/port/ProductCacheEvictionPort.java
public interface ProductCacheEvictionPort {
    void evictProductCache(String productId);
    void evictProductListCache(String categoryId);
}

// application/port/ProductCachePort.java
public interface ProductCachePort {
    ProductVO getProductCache(String productId, Supplier<ProductVO> loader);  // null = 未命中或不存在
}

// adapter/outbound/cache/ProductCacheAdapter.java — Spring Cache 注解式实现（@Cacheable/@CacheEvict，纯 Redis 单层）
// 写侧失效：ProductCacheEvictionPort.evictProductCache(productId)
```

`CategoryCachePort` 同理，已从 domain 层移至 application 层，移除泛型 `<T>` 参数，直接使用 `CategoryReadModel`。缓存实现基于 Spring Cache（framework `RedisCacheConfig`），不再手写多级缓存（2026-08-13）。

## 库存并发控制

- `@Version` 乐观锁防止超卖
- `StockQuantity` 值对象封装库存操作
- `StockDecreasedEvent` / `StockRestoredEvent` 通知下游模块

## 常见开发任务

### 添加商品新字段

1. `Product` 聚合根 + 对应值对象
2. Flyway 迁移脚本
3. `ProductDO` + `ProductDataMapper`
4. `ProductReadModel` + `ProductReadModelAssembler`
5. Request/Response DTO
6. 缓存失效逻辑
7. 测试

### 添加新搜索维度

1. `ProductSearchRequest`（adapter DTO）和 `ProductSearchCriteria`（application criteria）添加字段
2. `ProductSearchController` 补充 DTO→Criteria 转换
3. `ProductQueryRepository` 修改查询
4. `ProductReadModel` 添加字段
5. 缓存 Key 调整
6. 测试

## 跨模块交互

- **order 模块**: 通过 `ProductInventoryPort` 操作产品生命周期（快照、库存、售出）
- **favorite 模块**: 通过 `ProductInfoPort`（`FavoriteProductInfoAdapter` 在 application 模块实现）查询商品信息
- order/favorite 对 product 的 Maven 依赖均为 `<optional>true</optional>`，通过 Port 接口隔离领域模型
