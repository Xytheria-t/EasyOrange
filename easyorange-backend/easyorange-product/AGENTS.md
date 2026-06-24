# easyorange-product 模块指南

商品管理模块，DDD + CQRS 架构，支持商品 CRUD、搜索、库存、分类、举报、评价。

## 目录结构

```
product/
├── adapter/
│   ├── inbound/web/
│   │   ├── controller/
│   │   │   ├── ProductCommandController.java   # 商品写操作 (创建/更新/删除/状态)
│   │   │   ├── ProductQueryController.java     # 商品读操作 (详情/列表)
│   │   │   ├── ProductSearchController.java    # 搜索 (关键词/历史/热词)
│   │   │   ├── ProductReportController.java    # 举报管理
│   │   │   └── ProductReviewController.java    # 评价管理
│   │   ├── scheduler/
│   │   │   └── ProductPriceAdjustTask.java # 阶梯降价定时任务 (每天凌晨 2 点执行)
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
│   │   ├── OfferAppService.java          # AI 议价应用服务 (处理买家出价)
│   │   ├── OfferResult.java              # 议价结果 DTO
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
│       └── service/
│       ├── ProductViewCountService.java
│       └── SearchHistoryBufferService.java
├── domain/
│   ├── aggregate/
│   │   └── Product.java                 # 商品聚合根
│   ├── entity/
│   │   ├── ProductAuditLog.java
│   │   ├── ProductDetail.java
│   │   ├── ProductReport.java
│   │   └── ReportHandleHistory.java
│   ├── valueobject/
│   │   ├── ProductId.java, CategoryId.java, SellerId.java
│   │   ├── Money.java, StockQuantity.java, Version.java
│   │   ├── ProductTitle.java, ProductDescription.java
│   │   ├── ImageUrl.java, ImageSet.java, TagSet.java
│   │   ├── ContactMethod.java, TradeLocation.java
│   │   └── SellerInfo.java
│   ├── event/
│   │   ├── ProductCreatedEvent.java
│   │   ├── ProductUpdatedEvent.java
│   │   ├── ProductDeletedEvent.java
│   │   ├── ProductMarkedSoldEvent.java
│   │   ├── StockDecreasedEvent.java
│   │   ├── StockRestoredEvent.java
│   │   ├── PriceAdjustedEvent.java        # 阶梯降价事件
│   │   ├── OfferAcceptedEvent.java        # AI 接受出价事件
│   │   ├── OfferRejectedEvent.java        # AI 拒绝出价事件
│   │   └── CounterOfferMadeEvent.java     # AI 还价事件
│   ├── port/
│   │   ├── OutboundPort.java            # 标记接口 (跨模块出站端口)
│   │   ├── ProductCachePort.java        # 缓存端口 (domain 定义, application 实现)
│   │   ├── CategoryCachePort.java
│   │   ├── ProductSnapshotPort.java
│   │   ├── SellerInfoPort.java          # 卖家信息查询 (跨模块)
│   │   ├── ProductNotificationPort.java # 商品事件通知 (跨模块)
│   │   ├── ProductSearchIndexPort.java  # 搜索索引 (跨模块)
│   │   ├── OrderCreationPort.java       # 创建订单端口 (AI 接受出价后自动创建订单)
│   │   └── NegotiationMessagePort.java  # 议价消息生成端口 (LLM 生成话术)
│   ├── repository/
│   │   ├── ProductRepository.java       # 写仓储
│   │   ├── ProductReportRepository.java
│   │   └── query/
│   │       ├── ProductQueryRepository.java  # 读仓储
│   │       └── CategoryQueryRepository.java
│   ├── service/
│   │   ├── ProductReportDomainService.java
│   │   └── OfferRuleEngine.java           # AI 议价规则引擎 (决策接受/还价/拒绝)
│   ├── enums/
│   │   ├── ProductStatus.java, ConditionLevel.java
│   │   ├── ProductReportStatus.java
│   │   ├── ProductResultCode.java
│   │   ├── ConsignmentMode.java          # 寄售模式枚举 (MANUAL/AI_MANAGED)
│   │   └── OfferDecision.java            # 议价决策枚举 (ACCEPT/COUNTER_OFFER/REJECT)
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

## AI 托管寄售

商品模块实现了 AI 全自动托管寄售工作流，核心组件：

- **ConsignmentMode**: 寄售模式枚举（MANUAL/AI_MANAGED），存储在 `eo_product.consignment_mode` 字段
- **OfferRuleEngine**: 议价规则引擎，基于 floorPrice 做决策（接受/还价/拒绝）
- **OfferAppService**: 议价应用服务，处理买家出价，协调规则引擎和消息端口
- **OfferDecision**: 议价决策枚举（ACCEPT/COUNTER_OFFER/REJECT）
- **ProductPriceAdjustTask**: 阶梯降价定时任务，每天凌晨 2 点自动降价
- **OrderCreationPort**: 出站端口，AI 接受出价后自动创建订单（由 order 模块实现）
- **NegotiationMessagePort**: 出站端口，LLM 生成议价话术（由 ai 模块实现）
- **offer 领域事件**: PriceAdjustedEvent / OfferAcceptedEvent / OfferRejectedEvent / CounterOfferMadeEvent

### 编辑商品修改寄售模式

`Product.update()` 新增 `consignmentMode` / `floorPrice` 参数（参见 `ProductUpdateRequest` + `UpdateProductCommand`）。**domain 层强制校验**：`consignmentMode == AI_MANAGED` 时 `floorPrice` 必须非 null 且 > 0；设置为 `MANUAL` 时可清空底价。不传 consignmentMode（null）则保持原值。

`Product.create()` 同样校验：AI_MANAGED 必须设底价。

### 常见开发任务：添加 AI 托管寄售新功能

1. 规则引擎 `OfferRuleEngine` 添加新决策逻辑
2. domain event 定义新事件类型
3. 应用服务 `OfferAppService` 编排新流程
4. 出站端口接口 + 适配器实现
5. 定时任务配置
6. 测试

## 跨模块交互

- **order 模块**: 通过 `ProductInventoryPort` 扣减/恢复库存；通过 `OrderCreationPort` 创建订单
- **favorite 模块**: 通过 `ProductInfoPort`（`FavoriteProductInfoAdapter` 在 application 模块实现）查询商品信息
- **ai 模块**: 通过 `NegotiationMessagePort` 生成议价话术
- order/favorite/ai 对 product 的 Maven 依赖均为 `<optional>true</optional>`，通过 Port 接口隔离领域模型
