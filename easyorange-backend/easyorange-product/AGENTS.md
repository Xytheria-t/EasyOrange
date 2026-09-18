# easyorange-product 模块指南

商品管理模块，DDD + CQRS 架构，支持商品 CRUD、搜索、库存、分类、举报、评价。

## 目录结构

```
product/
├── adapter/
│   ├── inbound/
│   │   ├── web/
│   │   │   ├── controller/              # ProductController / ProductSearchController / ProductReportController / ProductRatingController
│   │   │   ├── assembler/
│   │   │   └── dto/request/ + dto/response/
│   │   └── messaging/ProductEventConsumer.java   # RabbitMQ 领域事件消费者（异步投影：CQRS 缓存、索引、通知）
│   └── outbound/
│       ├── persistence/                 # 持久化 — 按领域聚合分组
│       │   ├── product/                 # 商品聚合：ProductDO + ProductDetailDO + ProductImageDO + Mapper × 3
│       │   │                            #   + ProductRepositoryImpl + ProductQueryRepositoryImpl + ProductDataMapper + ProductSnapshotAdapter
│       │   ├── category/                # 分类：CategoryDO + CategoryProductCount + Mapper + QueryRepositoryImpl
│       │   ├── audit/                   # 审核日志：ProductAuditLogDO + Mapper + DataMapper + RepositoryImpl
│       │   ├── report/                  # 举报：ProductReportDO + ReportHandleHistoryDO + Mapper × 2 + RepositoryImpl × 3
│       │   ├── rating/                  # 评价：ProductRatingDO + Mapper + QueryRepositoryImpl + RepositoryImpl
│       │   ├── search/                  # 搜索：HotKeywordDO + SearchHistoryDO + Mapper × 2
│       │   └── stock/                   # 库存流水：StockLedgerDO + Mapper + RepositoryImpl
│       ├── scheduler/                   # 定时批处理
│       │   ├── StockReconcileScheduler.java   # 库存对账（余额 vs 最新流水 stock_after）
│       │   └── ViewCountFlushScheduler.java   # 浏览量 Redis→DB 定时刷入
│       └── cache/                       # 缓存适配器
│           ├── ProductCacheAdapter.java     # 实现 ProductCachePort + ProductCacheEvictionPort
│           ├── CategoryCacheAdapter.java    # 实现 CategoryCachePort
│           └── ProductCacheConstant.java
├── application/
│   ├── command/                         # 命令侧 (CQRS Write)
│   │   ├── ProductCommand.java               # 密封接口（所有命令实现此接口）
│   │   ├── CreateProductCommand.java / UpdateProductCommand.java / CreateProductRatingCommand.java（顶层 record）
│   │   ├── ProductCommandHandler.java
│   │   ├── ProductReportCommandHandler.java
│   │   └── ProductRatingCommandHandler.java
│   ├── query/                           # 查询侧 (CQRS Read)
│   │   ├── ProductQueryHandler.java / ProductReportQueryHandler.java / ProductRatingQueryHandler.java
│   │   ├── ProductSearchQueryHandler.java / ProductSearchCriteria.java / CategoryQueryHandler.java
│   │   ├── dto/                         # 应用层输出 VO：ProductVO / ProductRatingVO / RatingStatsVO
│   │   ├── readmodel/                   # 读模型：ProductReadModel / CategoryReadModel / SellerReadModel / HotKeywordReadModel / SearchHistoryReadModel
│   │   └── assembler/ProductReadModelAssembler.java
│   ├── port/                            # 应用层端口
│   │   ├── cache/                       # ProductCachePort / CategoryCachePort / SellerCachePort / ViewCountPort
│   │   └── query/                       # ProductQueryRepository / ProductReportQueryRepository / ProductRatingQueryRepository
│   │                                    #   / CategoryQueryRepository / ProductSearchQueryPort / AiSearchEnhancerPort
│   ├── event/ProductDomainEventListener.java  # 同步监听：缓存失效、审核日志（异步投影见 ProductEventConsumer）
│   ├── config/ProductDomainConfig.java
│   └── service/                         # ProductViewCountAppService（浏览量 Redis 增量）
│                                        #   / ViewCountBatchProcessor（批量刷入 DB）/ SearchHistoryBufferAppService
├── domain/
│   ├── aggregate/Product.java
│   ├── entity/                          # ProductAuditLog / ProductDetail / ProductRating / ProductReport / ReportHandleHistory
│   ├── valueobject/                     # CategoryId, SellerId / StockQuantity, Version / StockChange, StockDrift, ViewCountEntry
│   │                                    #   / ProductTitle, ProductDescription / ImageUrl, ImageSet, TagSet
│   │                                    #   / ContactMethod, TradeLocation / SellerInfo / Rating, ReviewContent
│   ├── event/                           # ProductEvent（sealed）+ Created/Updated/Deleted/SubmittedForReview/PutOnline
│   │                                    #   / TakeOffline/MarkedSold/Audited/StockDecreased/StockRestored/ReportProcessed 事件
│   ├── port/                            # ProductCacheEvictionPort（仅 evict）/ ProductSnapshotPort / SellerInfoPort（跨模块）
│   │                                    #   / CompletedOrderPort（跨模块，评价资格）/ ProductNotificationPort / ProductSearchIndexPort
│   ├── repository/                      # ProductRepository / ProductReportRepository / ProductRatingRepository
│   │                                    #   / ProductAuditLogRepository / ReportHandleHistoryRepository / StockLedgerRepository
│   ├── service/ProductReportDomainService.java
│   ├── enums/                           # ProductStatus, ConditionLevel, AuditAction, ReportReasonType, StockChangeType
│   │                                    #   / ProductReportStatus / ProductResultCode
│   ├── constant/ProductConstant.java
│   └── exception/ProductDomainException.java
```

## 领域事件模式

商品领域事件统一实现 `ProductEvent` 密封接口（extends `DomainEvent`）：

- 所有事件共享 `String productId()` 作为聚合根标识，密封接口的 `aggregateId()` 默认实现由它派生
- 新增产品事件只需 `implements ProductEvent` 并定义组件即可，无需手动实现 `aggregateId()`
- 同步副作用（缓存失效、审核日志）走 `ProductDomainEventListener`（`@EventListener`，同事务同线程）；异步投影走 `ProductEventConsumer`（`@RabbitListener`，队列 `eo.product.cqrs`）

## CQRS 架构

**Command 侧 (写)**:
`ProductController` → `ProductCommandHandler` → `Product` 聚合根 → `ProductRepository`

**Query 侧 (读)**:
`ProductController` → `ProductQueryHandler` → `ProductQueryRepository`（`application/port/query/`）→ `ProductReadModel`

读写使用不同的 Repository 接口和数据模型，查询侧使用 ReadModel 组装响应。

## 缓存端口模式

缓存端口按读写职责分拆：

- **domain 层**: `ProductCacheEvictionPort` — 仅 `evictProductCache(productId)`，领域服务只做驱逐
- **application 层**: `ProductCachePort` — 仅 `getProductCache(productId, loader)`（未命中回源，null 不落缓存）
- **adapter 层**: `ProductCacheAdapter` 同时实现两个端口（`@Cacheable` 读 + `@CacheEvict` 失效，纯 Redis 单层 + 短 TTL）

```java
// domain/port/ProductCacheEvictionPort.java
public interface ProductCacheEvictionPort {
    void evictProductCache(String productId);
}

// application/port/cache/ProductCachePort.java
public interface ProductCachePort {
    ProductVO getProductCache(String productId, Supplier<ProductVO> loader);  // null = 未命中或不存在
}
```

`CategoryCachePort`（application 层）直接用 `CategoryReadModel`，读走 `@Cacheable`、驱逐走 `@CacheEvict`。另有两个 application 层端口：`SellerCachePort`（批量卖家信息缓存）、`ViewCountPort`（浏览量 Redis hash 缓冲，`ViewCountBatchProcessor` 定时落库）。

## 库存并发控制

三层各管一件事，别互相替代：

- **分布式锁（order 侧）**：按 `productId` 串行化下单，压掉热点行并发——这一层只管「同时写」。
- **`@Version` 乐观锁**：整行写回的乐观并发控制，兜住不同锁键路径（如取消/退款恢复走 MQ 消费者、无商品锁）之间的丢失更新，冲突抛 `ConcurrentUpdateException`（B0006）。
- **库存流水 `eo_stock_ledger`**：管「写了几次、写了多少」——锁和版本号都管不了重复执行与数量错误。任何库存变更必须同事务落一条流水（`StockChange`），唯一索引 `(change_type, biz_id, product_id)` 承载幂等，重复投递落账失败即跳过本次变更（`ProductCommandHandler.claimStockChange`）。`ProductInventoryPort.decreaseStock/restoreStock` 必须带订单 ID，恢复数量必须取自订单事件明细。
- **对账**：`StockReconcileScheduler`（`adapter/outbound/scheduler/`）每日比对余额与最近一条流水的 `stock_after`，漂移告警打点，不自动改余额。
- `StockQuantity` 值对象封装库存操作（扣减为负即抛，是超卖的最后一道）
- `StockDecreasedEvent` / `StockRestoredEvent` 通知下游模块

新增库存写路径时的检查项：是否落流水、幂等键是否唯一、对账能否覆盖、`restoreStock` 数量是否与扣减对称。绕开聚合根的单列 SQL 更新会直接触发对账漂移告警。

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
- **favorite 模块**: 通过 `ProductInfoPort`（`FavoriteProductInfoAdapter` 在 `easyorange-application/adapter/outbound/product/` 实现）查询商品信息
- **ai 模块**: 通过 `ProductSearchQueryPort` / `AiSearchEnhancerPort` 接入检索与搜索增强（本模块作为端口定义方）
