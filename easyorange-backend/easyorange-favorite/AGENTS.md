# easyorange-favorite 模块指南

收藏模块，DDD + ACL 架构，处理用户商品收藏的增删查。

## 目录结构

```
favorite/
├── adapter/                           # 适配器层
│   ├── inbound/web/                   # 入站适配器
│   │   ├── controller/
│   │   │   └── FavoriteController.java
│   │   └── dto/request/
│   │       ├── BatchCheckRequest.java
│   │       └── BatchRemoveRequest.java
│   └── outbound/persistence/          # 出站适配器
│       ├── FavoriteDO.java
│       ├── FavoriteMapper.java
│       └── MybatisFavoriteRepository.java
├── application/                       # 应用层
│   ├── dto/
│   │   ├── AddFavoriteDTO.java
│   │   ├── FavoritePageQuery.java
│   │   ├── FavoriteResponse.java
│   │   └── RemoveFavoriteDTO.java
│   └── service/
│       └── FavoriteService.java
└── domain/                            # 领域层
    ├── aggregate/
    │   └── Favorite.java              # 收藏聚合根 (不可变)
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
    private final Long id;
    private final Long userId;
    private final Long productId;
    private final LocalDateTime createTime;

    public static Favorite create(Long userId, Long productId) { ... }
    public static Favorite reconstitute(Long id, Long userId, Long productId, LocalDateTime createTime) { ... }
}
```

- 不可变设计，通过静态工厂方法创建
- `create()` 用于新建，`reconstitute()` 用于从持久化重建

## 事务规范

查询方法必须标注 `@Transactional(readOnly = true)`：
- `queryFavorites()` - 分页查询
- `isFavorited()` - 检查是否收藏
- `getFavoriteCount()` - 获取收藏数量
- `batchCheckFavorited()` - 批量检查收藏状态

## 常见开发任务

### 添加收藏新功能

1. `FavoriteController` 添加端点
2. `FavoriteService` 添加业务方法
3. 如需新 DTO → `application/dto/`
4. 添加测试
