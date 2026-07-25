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
│       ├── persistence/                 # 持久化
│       │   ├── converter/ProductConverter.java
│       │   ├── persistence/             # DO: ProductDO, ProductDetailDO, CategoryDO, etc.
│       │   ├── mapper/                  # MyBatis Mapper
│       │   ├── repository/              # Repository 实现
│       │   └── ProductSnapshotPortImpl.java
│       ├── scheduler/                  # 定时批处理
│       │   └── ViewCountFlushScheduler.java  # 浏览量 Redis→DB 定时刷入
│       └── cache/                       # 缓存适配器
│           ├── ProductCacheAdapter.java     # 实现 ProductCacheEvictionPort + ProductCachePort
│           ├── CategoryCacheAdapter.java    # 实现 CategoryCachePort
│           └── ProductCacheConstant.java
├── application/
│   ├── command/                         # 命令侧 (CQRS Write)
│   │   ├── ProductCommand.java               # 密封接口（所有命令实现此接口）
│   │   ├── CreateProductCommand.java         # 顶层 record
│   │   ├── UpdateProductCommand.java         # 顶层 record
│   │   ├── CreateProductRatingCommand.java   # 顶层 record
│   │   ├── ProductCommandService.java
│   │   ├── ProductReportCommandService.java
│   │   └── ProductRatingCommandService.java
│   ├── query/                           # 查询侧 (CQRS Read)
│   │   ├── ProductQueryService.java
│   │   ├── ProductReportQueryService.java
│   │   ├── ProductRatingQueryService.java
│   │   ├── ProductSearchHandler.java
│   │   ├── CategoryQueryService.java
│   │   ├── criteria/                    # 查询参数对象
│   │   │   └── ProductSearchCriteria.java
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
│   │   ├── ProductId.java, CategoryId.java, SellerId.java
│   │   ├── Money.java, StockQuantity.java, Version.java
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
│   │   ├── OutboundPort.java            # 标记接口 (跨模块出站端口)
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
`ProductController` → `ProductCommandService` → `Product` 聚合根 → `ProductRepository`

**Query 侧 (读)**:
`ProductController` → `ProductQueryService` → `ProductQueryRepository`（`application/port/query/`）→ `ProductReadModel`

读写使用不同的 Repository 接口和数据模型，查询侧使用 ReadModel 组装响应。查询 Repository 接口位于 `application/port/query/`（非 domain 层），因为查询 ReadModel 是 application 层的概念。

## 缓存端口模式

缓存端口按 DDD 双向依赖原则分拆：

- **domain 层**: `ProductCacheEvictionPort` — 仅 `evictProductCache(id)` / `evictProductListCache()`，领域服务只做驱逐
- **application 层**: `ProductCachePort` — 完整 CRUD 操作（get/put/evict），供查询服务使用
- **adapter 层**: `ProductCacheAdapter` 同时实现两个端口

```java
// domain/port/ProductCacheEvictionPort.java
public interface ProductCacheEvictionPort {
    void evictProductCache(String productId);
    void evictProductListCache();
}

// application/port/ProductCachePort.java
public interface ProductCachePort {
    Optional<ProductVO> getProduct(ProductId id);
    void putProduct(ProductId id, ProductVO product);
    void evictProductCache(String productId);
}

// adapter/outbound/cache/ProductCacheAdapter.java — Redis 实现，实现两个接口
```

`CategoryCachePort` 同理，已从 domain 层移至 application 层，移除泛型 `<T>` 参数，直接使用 `CategoryReadModel`。

## 库存并发控制

- `@Version` 乐观锁防止超卖
- `StockQuantity` 值对象封装库存操作
- `StockDecreasedEvent` / `StockRestoredEvent` 通知下游模块

## 常见开发任务

### 添加商品新字段

1. `Product` 聚合根 + 对应值对象
2. Flyway 迁移脚本
3. `ProductDO` + `ProductConverter`
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

- **order 模块**: 通过 `ProductInventoryPort` 扣减/恢复库存
- **favorite 模块**: 通过 `ProductInfoPort`（`FavoriteProductInfoAdapter` 在 application 模块实现）查询商品信息
- order/favorite 对 product 的 Maven 依赖均为 `<optional>true</optional>`，通过 Port 接口隔离领域模型
