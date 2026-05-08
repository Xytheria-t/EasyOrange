# easyorange-favorite 模块指南

收藏模块，DDD + ACL 架构，处理用户商品收藏的增删查。

## 目录结构

```
favorite/
├── controller/                        # 控制器 (待迁移到 adapter/inbound/web/)
│   ├── FavoriteController.java        # 收藏端点
│   └── request/
│       └── BatchRemoveRequest.java
├── service/                           # 服务层 (待迁移到 application/)
│   ├── FavoriteService.java           # 收藏业务逻辑
│   └── dto/
│       ├── AddFavoriteDTO.java
│       ├── RemoveFavoriteDTO.java
│       ├── FavoritePageQuery.java
│       └── FavoriteVO.java
├── domain/
│   ├── aggregate/
│   │   └── Favorite.java              # 收藏聚合根 (不可变)
│   └── repository/
│       └── FavoriteRepository.java    # 仓储接口
├── infrastructure/
│   ├── acl/                           # 防腐层
│   │   ├── ProductAclService.java         # 商品信息 ACL 接口
│   │   └── ProductAclServiceImpl.java     # 商品信息 ACL 实现
│   └── persistence/                   # 持久化 (待迁移到 adapter/outbound/)
│       ├── FavoriteDO.java
│       ├── FavoriteMapper.java
│       └── MybatisFavoriteRepository.java
└── enums/ (无独立枚举，使用 common 枚举)
```

## ACL 模式

通过 `ProductAclService` 隔离对 product 模块的依赖：

```java
// infrastructure/acl/ProductAclService.java
public interface ProductAclService {
    ProductInfo getProductInfo(Long productId);
}

// infrastructure/acl/ProductAclServiceImpl.java
@Service
public class ProductAclServiceImpl implements ProductAclService {
    // 内部调用 product 模块服务，转换为目标模型
}
```

这是项目中 ACL 模式的最佳实践示例，其他模块的跨模块依赖也应参照此模式演进。

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

## 演进路线

当前为部分 DDD 结构，待完善：

1. `controller/` → `adapter/inbound/web/controller/`
2. `service/` → `application/service/`
3. `infrastructure/persistence/` → `adapter/outbound/persistence/`
4. `infrastructure/acl/` → `adapter/outbound/acl/`
5. 添加 `domain/port/output/` 端口接口

## 常见开发任务

### 添加收藏新功能

1. `FavoriteController` 添加端点
2. `FavoriteService` 添加业务方法
3. 如需新 DTO → `service/dto/`
4. 添加测试

### 修改 ACL 隔离

1. `ProductAclService` 接口添加方法
2. `ProductAclServiceImpl` 实现
3. 注意：ACL 层负责模型转换，不暴露 product 模块的内部类型
