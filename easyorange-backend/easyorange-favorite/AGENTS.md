# easyorange-favorite 模块指南

收藏模块，DDD + ACL 架构，处理用户商品收藏的增删查与降价提醒。

## 目录结构

```
favorite/
├── adapter/
│   ├── inbound/web/
│   │   ├── controller/FavoriteController.java
│   │   ├── assembler/FavoriteAssembler.java
│   │   └── dto/
│   │       ├── request/                  # BatchCheckRequest / BatchRemoveRequest
│   │       └── response/                 # FavoriteResponse
│   └── outbound/persistence/
│       ├── FavoriteDO.java, FavoriteMapper.java, FavoriteRepositoryImpl.java
├── application/
│   └── service/FavoriteService.java
└── domain/
    ├── aggregate/Favorite.java           # 收藏聚合根（record）
    ├── port/
    │   ├── ProductInfoPort.java          # 商品价格/归属查询（ACL）
    │   └── PriceDropNotificationPort.java # 降价通知（实现在 easyorange-application）
    ├── repository/FavoriteRepository.java
    └── valueobject/                      # ProductDetailInfo / ProductInfo / SellerInfo
```

## ACL 模式

通过 `ProductInfoPort` 端口接口隔离对 product 模块的依赖，实现 `FavoriteProductInfoAdapter` 在 `easyorange-application/adapter/outbound/product/`。

这是项目中 ACL 模式的最佳实践示例，其他模块的跨模块依赖也应参照此模式。

## Favorite 聚合根

```java
public record Favorite(String id, String userId, String productId, BigDecimal priceSnapshot, LocalDateTime createTime) {
    public static Favorite create(String userId, String productId, BigDecimal price);
    public static Favorite reconstitute(String id, String userId, String productId, BigDecimal priceSnapshot, LocalDateTime createTime);
    public void validateOwnership(String userId);
    public boolean isPriceDrop(BigDecimal newPrice);
}
```

- `create()` 校验 userId / productId 非空并记录收藏时价格快照（价格缺失拒绝收藏）
- `reconstitute()` 仅从持久化重建，不做校验
- `isPriceDrop(newPrice)` 要求新价低于快照价；快照为空视为未知，不判定降价
- 快照更新走仓储 CAS（`WHERE price_snapshot = 旧值`），重复事件不重复通知（只提醒「再创新低」）

## 常见开发任务

### 添加收藏新功能

1. `FavoriteController` 添加端点
2. `FavoriteAssembler` 添加转换（adapter/inbound/web/assembler/）
3. Request DTO 放在 `adapter/inbound/web/dto/request/`
4. Response DTO 放在 `adapter/inbound/web/dto/response/`
5. `FavoriteService` 添加业务方法（接受原始参数）
6. 添加测试
