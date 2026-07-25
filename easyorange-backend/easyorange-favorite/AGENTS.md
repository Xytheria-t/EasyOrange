# easyorange-favorite 模块指南

收藏模块，DDD + ACL 架构，处理用户商品收藏的增删查。

## 目录结构

```
favorite/
├── adapter/                           # 适配器层
│   ├── inbound/web/                   # 入站适配器
│   │   ├── controller/
│   │   │   └── FavoriteController.java
│   │   ├── assembler/
│   │   │   └── FavoriteAssembler.java
│   │   └── dto/
│   │       ├── request/
│   │       │   ├── AddFavoriteRequest.java
│   │       │   ├── FavoritePageRequest.java
│   │       │   ├── BatchCheckRequest.java
│   │       │   └── BatchRemoveRequest.java
│   │       └── response/
│   │           ├── FavoriteResponse.java
│   │           └── FavoriteDetailResponse.java
│   └── outbound/persistence/          # 出站适配器
│       ├── FavoriteDO.java
│       ├── FavoriteMapper.java
│       └── MybatisFavoriteRepository.java
├── application/                       # 应用层
│   └── service/
│       └── FavoriteService.java
└── domain/                            # 领域层
    ├── aggregate/
    │   ├── Favorite.java              # 收藏聚合根 (不可变)
    │   └── FavoriteCreateSpec.java    # record 收敛 create() 工厂参数
    ├── port/
    │   ├── OutboundPort.java
    │   └── ProductInfoPort.java       # 商品信息端口
    ├── repository/
    │   └── FavoriteRepository.java    # 仓储接口
    └── valueobject/
        ├── ProductDetailInfo.java
        ├── ProductInfo.java
        └── SellerInfo.java
```

## ACL 模式

通过 `ProductInfoPort` 端口接口隔离对 product 模块的依赖，实现在 application 模块的适配器中。

这是项目中 ACL 模式的最佳实践示例，其他模块的跨模块依赖也应参照此模式。

## Favorite 聚合根

```java
public class Favorite {
    private final String id;
    private final String userId;
    private final String productId;
    private final LocalDateTime createTime;

    public static Favorite create(FavoriteCreateSpec spec) { ... }
    public static Favorite reconstitute(String id, String userId, String productId, LocalDateTime createTime) { ... }
}
```

- 不可变设计，通过静态工厂方法创建
- `id`、`userId`、`productId` 均为 String (UUID v7)
- `create(FavoriteCreateSpec)` 用于新建（对齐 product/order/payment 的 Spec 模式，校验 null + 空字符串）
- `reconstitute()` 用于从持久化重建

## 事务规范

查询方法必须标注 `@Transactional(readOnly = true)`：
- `queryFavorites()` - 分页查询
- `isFavorited()` - 检查是否收藏
- `getFavoriteCount()` - 获取收藏数量
- `batchCheckFavorited()` - 批量检查收藏状态

## 常见开发任务

### 添加收藏新功能

1. `FavoriteController` 添加端点
2. `FavoriteAssembler` 添加转换（adapter/inbound/web/assembler/）
3. Request DTO 放在 `adapter/inbound/web/dto/request/`
4. Response DTO 放在 `adapter/inbound/web/dto/response/`
5. `FavoriteService` 添加业务方法（接受原始参数）
6. 添加测试
