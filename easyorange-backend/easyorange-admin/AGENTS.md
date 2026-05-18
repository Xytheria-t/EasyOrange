# easyorange-admin 模块指南

管理端（Admin）模块，提供后台管理系统的 RESTful API。

## 职责

管理端是独立的后台管理边界，包含以下功能域：

| 功能域 | Controller | 说明 |
|--------|-----------|------|
| 仪表板 | AdminDashboardController | 统计概览、待处理事项、趋势分析、最近动态、用户活跃热力图、Top 浏览量商品 |
| 用户管理 | AdminUserController | 用户列表、详情、状态、解锁、重置密码、强制下线、角色 |
| 商品管理 | AdminProductController | 商品列表、详情、审核(带原因)、批量审核 |
| 订单管理 | AdminOrderController | 订单列表、详情、取消、强制完成、退款、统计 |
| 分类管理 | AdminCategoryController | 分类 CRUD、树形结构、启用禁用 |
| 举报管理 | AdminReportController | 举报列表、详情、处理、统计 |
| 评价管理 | AdminReviewController | 评价列表、详情、删除 |

## 目录结构

```
easyorange-admin/
├── pom.xml
├── AGENTS.md
└── src/main/java/com/cartethyia/easyorange/admin/
    ├── controller/           # REST 控制器
    ├── service/              # 业务服务层
    └── dto/                  # 数据传输对象
        ├── request/          # 请求 DTO
        └── response/         # 响应 Response
```

## 设计原则

1. **只读优先**: 查询方法标注 `@Transactional(readOnly = true)`
2. **操作审计**: 所有写操作记录 reason + 操作人信息
3. **权限控制**: 所有接口依赖 SecurityConfig 的管理员鉴权
4. **依赖隔离**: 通过 `<optional>true</optional>` 实现编译期隔离

## 模块依赖

```
easyorange-admin ──optional──> easyorange-common   (Result, PageResult, BusinessException)
                 ──optional──> easyorange-user     (UserMapper, UserEntity, UserStatus)
                 ──optional──> easyorange-product  (ProductMapper, ProductDO, ProductReviewDO, ProductReviewMapper, CategoryDO, ProductReportRepository)
                 ──optional──> easyorange-order     (OrderReadRepository, OrderDO, OrderStatus)
                 ──optional──> easyorange-payment  (支付信息查询)
```

## 常见开发任务

### 添加新的管理端接口

1. 在 `dto/request/` 添加请求 DTO
2. 在 `dto/response/` 添加响应 Response
3. 在 `service/` 中编写业务逻辑
4. 在 `controller/` 中添加端点
5. 更新本 AGENTS.md 中的功能域表
