# 管理端（Admin）功能补全设计文档

> 日期: 2026-05-11
> 状态: **已完成** ✅
> 范围: 新建 easyorange-admin 模块 + 迁移已有管理端代码 + 新增 24 个 API 接口

## 1. 背景与目标

### 现状

EasyOrange 是一个校园二手交易平台，后端采用 Spring Boot + DDD 六边形架构，多模块组织。当前管理端功能**部分实现在 `easyorange-application/controller/admin/` 下**，仅包含：

- **仪表板**: 统计概览、待处理事项、最近用户/商品
- **用户管理**: 列表(分页筛选)、详情、修改状态
- **商品管理**: 列表(分页筛选)、详情、修改状态

### 缺失功能（本设计覆盖范围）

| 模块 | 缺失内容 | 优先级 |
|------|---------|--------|
| 用户管理 | 解锁账号、重置密码、强制下线、角色切换 | 高 |
| 商品审核 | 带原因审核、批量审核、举报联动 | 高 |
| 订单管理 | 列表/详情/取消/强制完成/退款/统计（**完全缺失**） | 高 |
| 分类管理 | CRUD/树形/排序/启用禁用（**完全缺失**） | 高 |
| 举报管理 | 列表/详情/处理/统计（**完全缺失**） | 中 |

### 目标

1. 新建独立 `easyorange-admin` 模块，迁移已有管理端代码
2. 补齐上述 5 大模块共 **24 个新 API 接口**
3. 保持与项目 DDD 六边形架构风格一致
4. 前端管理页面已存在（`easyorange-frontend/src/admin/`），需对齐接口

---

## 2. 架构决策：独立 Admin 模块

### 决策：新建 `easyorange-admin` 模块

**理由：**

- 当前管理端代码放在 `application` 模块导致职责混乱（启动模块承载业务逻辑）
- 新增 24 个接口后代码量从 ~15 文件膨胀到 ~35+ 文件
- 独立模块可实现测试隔离、构建并行、边界清晰
- 与项目中 user/product/order 等模块的组织方式一致

### 模块结构

```
easyorange-admin/
├── pom.xml
├── AGENTS.md
└── src/main/java/com/cartethyia/easyorange/admin/
    ├── controller/
    │   ├── AdminDashboardController.java      # 仪表板 (迁移)
    │   ├── AdminUserController.java           # 用户管理 (迁移+扩展)
    │   ├── AdminProductController.java        # 商品管理 (迁移+扩展)
    │   ├── AdminOrderController.java          # 订单管理 (新增)
    │   ├── AdminCategoryController.java       # 分类管理 (新增)
    │   └── AdminReportController.java         # 举报管理 (新增)
    ├── service/
    │   ├── AdminDashboardService.java         # (迁移+扩展)
    │   ├── AdminUserService.java              # (迁移+扩展)
    │   ├── AdminProductService.java           # (迁移+扩展)
    │   ├── AdminOrderService.java             # (新增)
    │   ├── AdminCategoryService.java          # (新增)
    │   └── AdminReportService.java            # (新增)
    └── dto/
        ├── request/
        │   ├── AdminUserQueryRequest.java          # 已有 → 迁移
        │   ├── AdminProductQueryRequest.java       # 已有 → 迁移
        │   ├── UpdateStatusRequest.java            # 已有 → 迁移 + 复用
        │   ├── AdminOrderQueryRequest.java         # 新增
        │   ├── OrderInterventionRequest.java       # 新增
        │   ├── CategoryCreateRequest.java          # 新增
        │   ├── CategoryUpdateRequest.java          # 新增
        │   ├── ProductAuditRequest.java            # 新增
        │   ├── ReportHandleRequest.java            # 新增
        │   ├── ResetPasswordRequest.java           # 新增
        │   └── UserUnlockRequest.java              # 新增
        └── response/
            ├── AdminUserVO.java                    # 已有 → 迁移
            ├── AdminProductVO.java                 # 已有 → 迁移
            ├── DashboardStatsVO.java               # 已有 → 迁移
            ├── PendingItemsVO.java                 # 已有 → 迁移
            ├── RecentUserVO.java                   # 已有 → 迁移
            ├── RecentProductVO.java                # 已有 → 迁移
            ├── AdminOrderVO.java                   # 新增
            ├── AdminOrderDetailVO.java             # 新增
            ├── CategoryVO.java                     # 新增
            ├── CategoryTreeVO.java                 # 新增
            ├── AdminReportVO.java                  # 新增
            └── OrderStatsVO.java                   # 新增
```

### Maven 依赖关系

```xml
<dependencies>
    <!-- 内部依赖（optional，编译期隔离） -->
    <dependency>
        <groupId>com.cartethyia.easyorange</groupId>
        <artifactId>easyorange-common</artifactId>
        <optional>true</optional>
    </dependency>
    <dependency>
        <groupId>com.cartethyia.easyorange</groupId>
        <artifactId>easyorange-user</artifactId>
        <optional>true</optional>
    </dependency>
    <dependency>
        <groupId>com.cartethyia.easyorange</groupId>
        <artifactId>easyorange-product</artifactId>
        <optional>true</optional>
    </dependency>
    <dependency>
        <groupId>com.cartethyia.easyorange</groupId>
        <artifactId>easyorange-order</artifactId>
        <optional>true</optional>
    </dependency>
    <dependency>
        <groupId>com.cartethyia.easyorange</groupId>
        <artifactId>easyorange-payment</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- 外部依赖 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
    </dependency>
</dependencies>
```

### 父 POM 更新

在 `easyorange-backend/pom.xml` 的 `<modules>` 中添加 `<module>easyorange-admin</module>`。

在 `easyorange-application/pom.xml` 中添加对 `easyorange-admin` 的依赖。

---

## 3. 接口详细设计

### 3.1 订单管理 (`/api/admin/orders`)

订单管理为全新模块，数据库表 `eo_order` / `eo_payment` 和领域模型 `OrderAggregate` / `OrderStatus` 已就绪。

#### 接口列表

| 方法 | 路径 | 功能 | 认证 |
|------|------|------|------|
| GET | `/api/admin/orders` | 订单列表（多条件筛选+分页） | Admin |
| GET | `/api/admin/orders/{id}` | 订单详情 | Admin |
| PUT | `/api/admin/orders/{id}/cancel` | 管理员取消订单 | Admin |
| PUT | `/api/admin/orders/{id}/force-complete` | 强制完成订单 | Admin |
| PUT | `/api/admin/orders/{id}/refund` | 管理员退款 | Admin |
| GET | `/api/admin/orders/stats` | 订单统计数据 | Admin |

#### 请求 DTO: AdminOrderQueryRequest

```java
public record AdminOrderQueryRequest(
    String orderNo,          // 订单号（模糊匹配）
    Long buyerId,            // 买家 ID
    Long sellerId,           // 卖家 ID
    Integer status,          // 订单状态（0-5）
    Integer paymentStatus,   // 支付状态（0-2）
    String startTime,        // 开始时间 yyyy-MM-dd
    String endTime,          // 结束时间 yyyy-MM-dd
    Integer pageNum,         // 页码（默认 1）
    Integer pageSize         // 每页条数（默认 20）
) {}
```

#### 响应 VO: AdminOrderVO

```java
public record AdminOrderVO(
    Long orderId,
    String orderNo,
    Long buyerId,
    String buyerName,
    String buyerAvatar,
    Long sellerId,
    String sellerName,
    Long productId,
    String productName,
    String productImage,
    BigDecimal amount,
    Integer status,
    String statusDesc,
    Integer paymentStatus,
    String paymentStatusDesc,
    LocalDateTime createTime
) {}
```

#### 响应 VO: AdminOrderDetailVO

```java
public record AdminOrderDetailVO(
    Long orderId,
    String orderNo,
    BuyerInfo buyer,
    SellerInfo seller,
    ProductInfo product,
    BigDecimal amount,
    Integer status,
    String statusDesc,
    Integer paymentStatus,
    String paymentNo,
    BigDecimal paidAmount,
    BigDecimal refundedAmount,
    Address shippingAddress,
    String remark,
    LocalDateTime createTime,
    LocalDateTime payTime,
    LocalDateTime updateTime
) {
    public record BuyerInfo(Long userId, String nickname, String avatar, String phone) {}
    public record SellerInfo(Long userId, String nickname, String avatar, String phone) {}
    public record ProductInfo(Long productId, String name, String mainImage, BigDecimal price) {}
    public record Address(String receiverName, String phone, String detailAddress) {}
}
```

#### 请求 DTO: OrderInterventionRequest

```java
public record OrderInterventionRequest(
    String reason  // 管理员操作原因（必填，用于审计）
) {}
```

#### 响应 VO: OrderStatsVO

```java
public record OrderStatsVO(
    long totalOrders,
    long todayOrders,
    long pendingPayment,
    long toShip,
    long toReceive,
    long completed,
    long cancelled,
    long refunded,
    BigDecimal totalRevenue,
    BigDecimal todayRevenue
) {}
```

#### 关键业务逻辑

**取消订单：**
1. 校验订单状态（仅待付款/待发货可取消）
2. 更新订单状态为 CANCELLED
3. 如已支付，触发退款流程（调用 Payment 模块退款能力）
4. 如已扣库存，触发库存回退（通过 StockReservationRequestedEvent 补偿或直接调用 ProductInventoryPort）
5. 发布 OrderCancelledEvent
6. 记录操作原因

**强制完成：**
1. 校验订单状态（仅待收货可强制完成）
2. 更新订单状态为 COMPLETED
3. 发布 OrderCompletedEvent（触发后续评价提醒等链路）

**强制退款：**
1. 校验订单状态（非已退款状态方可操作）
2. 调用 Payment 模块执行退款
3. 更新订单状态为 REFUNDED
4. 发布 OrderRefundedEvent
5. 库存不回退（买家已确认收货的商品退款不回退库存）

---

### 3.2 分类管理 (`/api/admin/categories`)

分类管理为全新模块。数据库表 `eo_category` 已建好（含 parent_id/level/sort_order/status/del_flag），product 模块中 CategoryDO / CategoryQueryRepository 已存在。

#### 接口列表

| 方法 | 路径 | 功能 | 认证 |
|------|------|------|------|
| GET | `/api/admin/categories` | 分类列表（扁平，支持 parentId 筛选） | Admin |
| GET | `/api/admin/categories/tree` | 分类树结构 | Admin |
| POST | `/api/admin/categories` | 创建分类 | Admin |
| PUT | `/api/admin/categories/{id}` | 更新分类 | Admin |
| PUT | `/api/admin/categories/{id}/status` | 启用/禁用分类 | Admin |
| DELETE | `/api/admin/categories/{id}` | 删除分类（软删除） | Admin |

#### 请求 DTO: CategoryCreateRequest

```java
public record CategoryCreateRequest(
    @NotBlank String name,
    Long parentId,          // 父分类 ID（null 表示一级分类）
    Integer sortOrder       // 排序值（默认 0）
) {}
```

#### 请求 DTO: CategoryUpdateRequest

```java
public record CategoryUpdateRequest(
    @NotBlank String name,
    Long parentId,
    Integer sortOrder
) {}
```

#### 响应 VO: CategoryVO

```java
public record CategoryVO(
    Long categoryId,
    String name,
    Long parentId,
    String parentName,
    Integer level,
    Integer sortOrder,
    Integer status,
    Integer productCount,
    LocalDateTime createTime,
    LocalDateTime updateTime
) {}
```

#### 响应 VO: CategoryTreeVO

```java
public record CategoryTreeVO(
    Long categoryId,
    String name,
    Integer level,
    Integer sortOrder,
    Integer status,
    List<CategoryTreeVO> children
) {
    public static CategoryTreeVO from(CategoryVO vo, List<CategoryTreeVO> children) {
        return new CategoryTreeVO(vo.categoryId(), vo.name(), vo.level(),
            vo.sortOrder(), vo.status(), children != null ? children : List.of());
    }
}
```

#### 关键业务逻辑

**创建分类：**
1. 校验同名同级分类不存在
2. 根据 parentId 计算 level（无父级 = 1，否则 = 父级 level + 1）
3. 限制最大层级为 3 级
4. 插入记录

**删除分类：**
1. 检查是否存在子分类 → 有则拒绝删除
2. 检查是否有关联商品（eo_product.category_id）→ 有则拒绝删除
3. 执行软删除（del_flag = 2）

**树形查询：**
1. 一次查询所有 status=1 且 del_flag=0 的分类
2. 按 parentId 在内存中递归构建树
3. 按 sortOrder 升序排列

---

### 3.3 用户管理补充 (`/api/admin/users`)

在现有 AdminUserController 基础上扩展 4 个接口。

#### 新增接口

| 方法 | 路径 | 功能 | 认证 |
|------|------|------|------|
| PUT | `/api/admin/users/{id}/unlock` | 解锁用户账号 | Admin |
| PUT | `/api/admin/users/{id}/reset-password` | 重置密码 | Admin |
| PUT | `/api/admin/users/{id}/force-logout` | 强制下线 | Admin |
| PUT | `/api/admin/users/{id}/role` | 修改角色 | Admin |

#### 请求 DTO: ResetPasswordRequest

```java
public record ResetPasswordRequest(
    @NotBlank String reason  // 重置原因（用于审计日志）
) {}
```

**返回值：** 包含新生成的随机密码（仅此一次展示）

#### 请求 DTO: UserUnlockRequest

```java
public record UserUnlockRequest(
    @NotBlank String reason  // 解锁原因
) {}
```

#### 关键业务逻辑

**解锁账号：**
1. 查询用户，校验存在
2. 清除 Redis 中的登录失败计数（key 格式参考 LoginCacheConstants）
3. 如用户状态为 LOCKED，恢复为 NORMAL
4. 记录操作日志

**重置密码：**
1. 生成 12 位随机密码（字母+数字+特殊字符）
2. BCrypt 加密
3. 更新用户密码字段
4. **清除该用户所有 Redis Token**（强制重新登录）
5. 返回新密码（明文仅在此响应中出现一次）

**强制下线：**
1. 根据用户 ID 查找并清除 Redis 中所有关联的 Token
2. 后续该用户的任何请求将返回 401

**角色切换：**
1. 修改 user_type 字段（01 ↔ 02）
2. 记录操作日志（谁、什么时候、把谁的角色的什么值改成了什么）
3. 注意：不能修改最后一个超级管理员的角色

---

### 3.4 商品审核补充 (`/api/admin/products`)

在现有 AdminProductController 基础上扩展 3 个接口。

#### 新增接口

| 方法 | 路径 | 功能 | 认证 |
|------|------|------|------|
| PUT | `/api/admin/products/{id}/audit` | 审核（带原因） | Admin |
| POST | `/api/admin/products/batch-audit` | 批量审核 | Admin |
| GET | `/api/admin/products/reports` | 举报列表（可合并到举报模块） | Admin |

#### 请求 DTO: ProductAuditRequest

```java
public record ProductAuditRequest(
    @NotNull Integer status,     // 目标状态（1 通过 / -1 拒绝）
    String reason                // 审核原因（拒绝时必填）
) {}
```

#### 批量审核请求

```java
public record BatchAuditRequest(
    @NotEmpty List<AuditItem> items
) {
    public record AuditItem(
        @NotNull Long productId,
        @NotNull Integer status,
        String reason
    ) {}
}
```

**返回值：** 汇总结果 `{ total: N, success: N, failed: N, errors: [...] }`

#### 关键业务逻辑

**带原因审核：**
1. 在现有 updateProductStatus 基础上增加 audit_reason 字段
2. 审核通过：DRAFT → APPROVED
3. 审核拒绝：DRAFT → REJECTED（需填写原因）
4. 审核通过时发送站内消息通知卖家
5. 审核记录可考虑写入 eo_sys_oper_log 或独立的审核记录表

**批量审核：**
1. 遍历 items 列表，逐个执行单条审核逻辑
2. 汇总成功/失败数量
3. 单条失败不影响其他条的执行
4. 返回详细错误信息

---

### 3.5 举报管理 (`/api/admin/reports`)

举报管理为全新模块。`eo_product_report` 表和 `ProductReportRepository` 已存在于 product 模块。

#### 接口列表

| 方法 | 路径 | 功能 | 认证 |
|------|------|------|------|
| GET | `/api/admin/reports` | 举报列表（分页+筛选） | Admin |
| GET | `/api/admin/reports/{id}` | 举报详情 | Admin |
| PUT | `/api/admin/reports/{id}/handle` | 处理举报 | Admin |
| GET | `/api/admin/reports/stats` | 举报统计 | Admin |

#### 请求 DTO: ReportHandleRequest

```java
public record ReportHandleRequest(
    @NotNull String action,    // 处理动作: IGNORE / PRODUCT_OFFLINE / WARN_SENDER / BAN_PRODUCT
    String remark              // 处理备注
) {}
```

#### 响应 VO: AdminReportVO

```java
public record AdminReportVO(
    Long reportId,
    Long productId,
    String productName,
    String productImage,
    Long reporterId,
    String reporterName,
    String reason,
    Integer status,
    String statusDesc,
    String handleResult,
    String handleRemark,
    LocalDateTime createTime,
    LocalDateTime handleTime
) {}
```

#### 处理动作说明

| 动作 | 含义 | 副作用 |
|------|------|--------|
| `IGNORE` | 忽略举报（无效举报） | 仅更新举报状态为已处理 |
| `PRODUCT_OFFLINE` | 下架被举报商品 | 将商品状态改为 OFFLINE |
| `WARN_SENDER` | 警告举报人（恶意举报） | 给举报人发送警告消息 + 记录 |
| `BAN_PRODUCT` | 封禁被举报商品 | 将商品状态改为 BANNED（永久下架且不可恢复） |

---

## 4. 数据库变更

### 需要的新增 Flyway 迁移

当前不需要新的 DDL 变更（所有表已存在）。但可能需要：

1. **V4__admin_audit_log.sql** — 可选：独立的审核操作记录表（如需保留审核历史）
2. **开发数据补充** — 在 `R__insert_dev_test_data.sql` 中补充更多分类/订单数据以支撑管理端开发调试

---

## 5. 实施计划（Phase 分期）

### Phase 1: 基础设施 + 无依赖模块

1. 创建 `easyorange-admin` 模块（pom.xml、包结构）
2. 迁移现有 3 个 Controller + 3 个 Service + 所有 DTO 到新模块
3. 从 `easyorange-application` 中删除已迁移文件
4. 更新 Maven 依赖（父 pom、application pom）
5. 实现 **分类管理** 全部 6 个接口
6. 实现 **订单查询**（列表 + 详情 + 统计）3 个接口

### Phase 2: 核心干预功能

7. 实现订单干预（取消 / 强制完成 / 退款）3 个接口
8. 实现用户管理补充（解锁 / 重置密码 / 强制下线 / 角色）4 个接口
9. 实现 **举报管理** 全部 4 个接口

### Phase 3: 审核完善 + 仪表板增强

10. 实现商品审核补充（带原因审核 / 批量审核）2-3 个接口
11. 完善 AdminDashboardService 中的占位方法（getRecentProducts、countTodayOrders）
12. 补充开发测试数据
13. 全量编译验证 + ArchUnit 架构规则更新

---

## 6. 前端对接说明

前端管理页面已在 `easyorange-frontend/src/admin/` 下实现：

| 前端页面 | 对应后端接口 | 状态 |
|---------|-------------|------|
| `DashboardPage.tsx` | `/api/admin/dashboard/*` | ✅ 已对接 |
| `UserManagePage.tsx` | `/api/admin/users/*` | ⚠️ 部分对接（缺 4 个新接口） |
| `ProductReviewPage.tsx` | `/api/admin/products/*` | ⚠️ 部分对接（缺审核补充接口） |
| `OrderManagePage.tsx` | `/api/admin/orders/*` | ❌ 无后端（待实现） |
| `ReportManagePage.tsx` | `/api/admin/reports/*` | ❌ 无后端（待实现） |
| `StatsPage.tsx` | `/api/admin/orders/stats` + dashboard stats | ⚠️ 部分对接 |

实施过程中需同步检查前端 `adminApi.ts` 中的类型定义和 API 调用是否与新接口对齐。

---

## 7. 安全注意事项

1. **所有 `/api/admin/*` 路径均需管理员权限校验**（user_type = 02 或通过 SecurityConfig 配置）
2. **敏感操作必须记录操作日志**（谁、什么时间、对什么资源、做了什么操作、原因）
3. **重置密码等操作的返回数据不得缓存**，一次性展示
4. **批量操作需限制单次数量**（如批量审核最多 50 条）
5. **订单干预操作需要二次确认机制**（前端 confirm + 后端 reason 必填）
