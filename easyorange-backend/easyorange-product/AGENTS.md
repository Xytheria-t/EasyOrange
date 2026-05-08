# easyorange-product 模块指南

商品管理模块，DDD + CQRS 架构，支持商品 CRUD、搜索、库存、分类、举报、评价。

## 目录结构

```
product/
├── adapter/
│   ├── inbound/web/
│   │   ├── ProductController.java       # 商品写操作 (创建/更新/删除/状态)
│   │   ├── ProductQueryController.java  # 商品读操作 (详情/列表)
│   │   ├── SearchController.java        # 搜索 (关键词/历史/热词)
│   │   ├── ProductReportController.java # 举报管理
│   │   ├── ProductReviewController.java # 评价管理
│   │   ├── dto/request/
│   │   └── dto/response/
│   └── outbound/
│       ├── persistence/                 # 持久化
│       │   ├── converter/ProductConverter.java
│       │   ├── dataobject/              # DO: ProductDO, ProductDetailDO, CategoryDO, etc.
│       │   ├── mapper/                  # MyBatis Mapper
│       │   ├── repository/              # Repository 实现
│       │   └── ProductSnapshotPortImpl.java
│       └── cache/                       # 缓存适配器
│           ├── ProductCacheAdapter.java     # 实现 ProductCachePort
│           ├── CategoryCacheAdapter.java    # 实现 CategoryCachePort
│           └── ProductCacheConstant.java
├── application/
│   ├── command/                         # 命令侧 (CQRS Write)
│   │   ├── ProductCommandService.java
│   │   ├── ProductReviewCommandService.java
│   │   ├── dto/                         # Command DTOs
│   │   └── handler/                     # 命令处理器
│   ├── query/                           # 查询侧 (CQRS Read)
│   │   ├── ProductQueryService.java
│   │   ├── ProductReviewQueryService.java
│   │   ├── readmodel/                   # 读模型
│   │   │   ├── ProductReadModel.java
│   │   │   ├── CategoryReadModel.java
│   │   │   ├── SellerReadModel.java
│   │   │   └── ...
│   │   ├── assembler/
│   │   │   └── ProductReadModelAssembler.java
│   │   ├── dto/                         # Query DTOs + VOs
│   │   └── handler/                     # 查询处理器
│   ├── event/
│   │   └── ProductEventListener.java
│   └── service/
│       ├── ProductViewCountService.java
│       └── SearchHistoryService.java
├── domain/
│   ├── aggregate/
│   │   └── Product.java                 # 商品聚合根
│   ├── entity/
│   │   ├── ProductDetail.java
│   │   ├── ProductReport.java
│   │   ├── HotKeyword.java
│   │   └── SearchHistory.java
│   ├── valueobject/
│   │   ├── ProductId.java, CategoryId.java, SellerId.java
│   │   ├── Money.java, StockQuantity.java, Version.java
│   │   ├── ProductTitle.java, ProductDescription.java
│   │   ├── ImageUrl.java, ImageSet.java, TagSet.java
│   │   ├── ContactMethod.java, TradeLocation.java
│   │   └── ProductSnapshotPort.java (接口)
│   ├── event/
│   │   ├── ProductCreatedEvent.java
│   │   ├── ProductUpdatedEvent.java
│   │   ├── ProductDeletedEvent.java
│   │   ├── ProductMarkedSoldEvent.java
│   │   ├── StockDecreasedEvent.java
│   │   └── StockRestoredEvent.java
│   ├── port/
│   │   ├── ProductCachePort.java        # 缓存端口 (domain 定义, application 实现)
│   │   ├── CategoryCachePort.java
│   │   └── ProductSnapshotPort.java
│   ├── repository/
│   │   ├── ProductRepository.java       # 写仓储
│   │   ├── ProductReportRepository.java
│   │   └── query/
│   │       ├── ProductQueryRepository.java  # 读仓储
│   │       └── CategoryQueryRepository.java
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

## CQRS 架构

**Command 侧 (写)**:
`ProductController` → `ProductCommandService` → `Product` 聚合根 → `ProductRepository`

**Query 侧 (读)**:
`ProductQueryController` → `ProductQueryService` → `ProductQueryRepository` → `ProductReadModel`

读写使用不同的 Repository 接口和数据模型，查询侧使用 ReadModel 组装响应。

## 缓存端口模式

domain 层定义缓存端口接口，application 层实现：

```java
// domain/port/ProductCachePort.java
public interface ProductCachePort {
    Optional<ProductReadModel> getProduct(ProductId id);
    void putProduct(ProductId id, ProductReadModel product);
    void invalidateProduct(ProductId id);
}

// adapter/outbound/cache/ProductCacheAdapter.java — Redis 实现
```

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

1. `ProductSearchRequest` 添加字段
2. `ProductQueryRepository` 修改查询
3. `ProductReadModel` 添加字段
4. 缓存 Key 调整
5. 测试

## 跨模块交互

- **order 模块**: 通过 `ProductInventoryPort` 扣减/恢复库存
- **favorite 模块**: 通过 `ProductAclService` 查询商品信息
- 当前 order/favorite 直接依赖 product Maven 模块，通过 Port/ACL 接口隔离领域模型
