# 商品上架审核流程 — 设计规格

> 日期: 2026-05-12
> 状态: 待评审
> 方案: A — 状态机扩展 + 审核记录表

## 1. 背景与目标

### 现状
- 已有基础审核功能：`AdminProductAuditController` + `ProductReviewPage`
- 商品状态仅 4 种：DRAFT(0) / ONLINE(1) / SOLD(2) / OFFLINE(3)
- **无**「待审核」中间态，商品从草稿直接跳到上架/下架
- **无**审核记录持久化（拒绝原因仅在内存中传递）
- **无**卖家通知机制

### 目标
构建完整的商品上架审核工作流：发布→待审核→通过/驳回→（重新提交）循环，含审核记录追踪和站内消息通知。

### 需求决策记录

| 决策项 | 选择 |
|--------|------|
| 触发方式 | 卖家发布后**自动提交**进入待审核 |
| 驳回流程 | **退回草稿态(REJECTED)**，支持**重新提交**（多轮审核） |
| 通知机制 | **站内消息通知**（接入 easyorange-message 模块） |
| 审核维度 | 基本信息、内容合规、图片质量、价格合理性（全部4项） |
| 数据库策略 | 开发阶段直接修改 V1 迁移脚本，不新增迁移文件 |

---

## 2. 状态机设计

### 2.1 商品状态枚举扩展

在现有 `ProductStatus` 基础上新增 2 个状态：

| 状态值 | 枚举名 | 说明 |
|--------|--------|------|
| 0 | `DRAFT` | 草稿 |
| **4** | **`PENDING_REVIEW`** | **待审核** — 发布后自动进入 |
| **5** | **`REJECTED`** | **已驳回** — 审核不通过，退回给卖家修改 |
| 1 | `ONLINE` | 上架 — 审核通过后进入 |
| 2 | `SOLD` | 已售出 |
| 3 | `OFFLINE` | 下架 — 管理员/卖家手动下架 |

### 2.2 状态流转图

```
                    ┌──────────────────────────────────────┐
                    │                                      │
                    ▼                                      │
  ┌──────┐   发布   ┌───────────┐   通过   ┌──────┐  售出  ┌──────┐
  │ DRAFT│ ──────▶ │ PENDING_  │ ──────▶ │ONLINE │ ────▶ │ SOLD │
  │  (0)  │         │ REVIEW(4) │         │ (1)   │       │ (2)  │
  └──────┘         └─────┬─────┘         └───┬───┘       └──────┘
    ▲                 │ 拒绝               │ 下架
    │ 重新提交        ▼                     ▼
    │           ┌───────────┐          ┌──────┐
    └───────────│ REJECTED  │          │OFFLINE│
                │   (5)     │          │ (3)  │
                └───────────┘          └──────┘
```

### 2.3 状态转换规则（后端校验）

| 当前状态 | 目标状态 | 操作者 | 条件 |
|----------|----------|--------|------|
| DRAFT | PENDING_REVIEW | 卖家 | 点击"发布商品" |
| PENDING_REVIEW | ONLINE | 管理员 | 审核通过 |
| PENDING_REVIEW | REJECTED | 管理员 | 必填拒绝原因 |
| REJECTED | PENDING_REVIEW | 卖家 | 修改后点击"重新提交" |
| ONLINE | OFFLINE | 卖家/管理员 | 主动下架或管理员下架 |
| ONLINE | SOLD | 系统 | 订单完成时自动 |

### 2.4 ProductStatus 新增方法

```java
public boolean canSubmitForReview() {
    return this == DRAFT || this == REJECTED;
}

public boolean canApprove() {
    return this == PENDING_REVIEW;
}

public boolean canReject() {
    return this == PENDING_REVIEW;
}
```

---

## 3. 数据库设计

### 3.1 策略：修改现有 V1 迁移脚本

开发阶段直接在 `V1__init_schema.sql` 的 `eo_product` 表定义后追加 `eo_product_audit_log` 表，并更新 `eo_product.status` 字段注释。

### 3.2 eo_product 表变更

将 `status` 字段注释更新为包含新状态值：

```sql
`status` TINYINT NOT NULL DEFAULT 0
  COMMENT '商品状态（0 草稿 4 待审核 5 已驳回 1 上架 2 已售出 3 下架）',
```

### 3.3 新增表：eo_product_audit_log

在 V1 脚本中 `eo_product` 表定义之后追加：

```sql
CREATE TABLE `eo_product_audit_log` (
    `id`              BIGINT       NOT NULL COMMENT '主键 ID',
    `product_id`      BIGINT       NOT NULL COMMENT '商品 ID',
    `operator_id`     BIGINT       NOT NULL COMMENT '操作人 ID',
    `operator_name`   VARCHAR(50)  NOT NULL COMMENT '操作人姓名（冗余存储）',
    `action`          TINYINT      NOT NULL COMMENT '审核动作（1-通过 2-拒绝 3-重新提交）',
    `reason`          VARCHAR(500) DEFAULT NULL COMMENT '审核原因/驳回理由',
    `audit_dimensions VARCHAR(500) DEFAULT NULL COMMENT '审核维度JSON: ["basic","compliance","image","price"]',
    `before_status`   TINYINT      NOT NULL COMMENT '操作前状态',
    `after_status`    TINYINT      NOT NULL COMMENT '操作后状态',
    `remark`          VARCHAR(500) DEFAULT NULL COMMENT '管理员备注',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_audit_product` (`product_id`, `create_time` DESC),
    KEY `idx_audit_operator` (`operator_id`, `create_time` DESC),
    KEY `idx_audit_action_time` (`action`, `create_time` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品审核记录表';
```

### 3.4 审核动作枚举

| action 值 | 常量名 | 含义 | 操作者 |
|-----------|--------|------|--------|
| 1 | APPROVE | 通过 | 管理员 |
| 2 | REJECT | 拒绝 | 管理员 |
| 3 | RESUBMIT | 重新提交 | 卖家 |

### 3.5 审核维度常量

| 维度标识 | 名称 | 说明 |
|----------|------|------|
| `basic` | 基本信息审核 | 名称、描述、分类、价格等基础信息合规性 |
| `compliance` | 内容合规审核 | 违禁品、虚假信息、侵权内容检查 |
| `image` | 图片质量审核 | 图片清晰度、真实性、与描述一致性 |
| `price` | 价格合理性审核 | 价格区间合理性，异常低价/高价检测 |

---

## 4. 后端 API 设计

### 4.1 改造策略

改造现有 `AdminProductAuditController` 和 `AdminProductAuditService`，不新增 Controller。

### 4.2 API 接口清单

| 方法 | 路径 | 变更类型 | 说明 |
|------|------|---------|------|
| PUT | `/api/admin/products/{id}/audit` | **增强** | 增加审核维度、强制拒绝原因、写审核日志 |
| POST | `/api/admin/products/batch-audit` | **增强** | 同上，批量操作 |
| GET | `/api/admin/products/{id}/audit-logs` | **新增** | 查询某商品的审核历史 |
| PUT | `/api/products/{id}/submit` | **新增** | 卖家重新提交审核（用户侧） |

### 4.3 请求 DTO

#### ProductAuditRequest（增强版）

```java
@Data
public class ProductAuditRequest {
    @NotNull(message = "审核动作不能为空")
    private Integer action;           // 1-通过 2-拒绝

    @Size(max = 500, message = "原因最长500字符")
    private String reason;            // 拒绝时必填

    private List<String> dimensions;  // 审核维度列表

    @Size(max = 500, message = "备注最长500字符")
    private String remark;            // 管理员可选备注
}
```

#### ResubmitRequest（新增）

```java
@Data
public class ResubmitRequest {
    // 无额外字段，仅用于触发状态变更
}
```

### 4.4 响应 VO

#### AuditLogVO（新增）

```java
public record AuditLogVO(
    Long id,
    Long productId,
    Long operatorId,
    String operatorName,
    Integer action,
    String actionDesc,
    String reason,
    List<String> dimensions,
    Integer beforeStatus,
    String beforeStatusDesc,
    Integer afterStatus,
    String afterStatusDesc,
    String remark,
    LocalDateTime createTime
) {}
```

### 4.5 服务层核心逻辑

#### auditProduct 流程

```
auditProduct(id, request):
  1. 加载商品 → 校验存在且未删除
  2. 校验当前状态 == PENDING_REVIEW
  3. action=APPROVE:
     → 商品状态 → ONLINE
     → reason 可选
  4. action=REJECT:
     → 校验 reason 非空且非空白
     → 商品状态 → REJECTED
  5. 写入 eo_product_audit_log（全字段）
  6. 发布 ProductAuditedEvent 领域事件
  7. 返回成功
```

#### submitForReview 流程（用户侧）

```
submitForReview(productId):
  1. 加载商品 → 校验存在且未删除
  2. 校验当前状态 == REJECTED（只有被驳回的商品可重新提交）
  3. 校验操作人是商品卖家本人
  4. 商品状态 → PENDING_REVIEW
  5. 写入 eo_product_audit_log（action=RESUBMIT）
  6. 返回成功
```

### 4.6 领域事件

#### ProductAuditedEvent（新增）

```java
package com.cartethyia.easyorange.product.domain.event;

public record ProductAuditedEvent(
    Long productId,
    String productName,
    Long sellerId,
    Integer action,          // 1=通过, 2=拒绝
    String reason,
    LocalDateTime auditTime
) {}
```

### 4.7 消息通知集成

#### EventListener 位置

`easyorange-application/src/main/java/com/cartethyia/easyorange/adapter/event/ProductAuditEventListener.java`

#### 通知逻辑

```java
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
@Async("domainEventExecutor")
public void onProductAudited(ProductAuditedEvent event) {
    if (event.action() == 1) {
        messageService.sendToUser(event.sellerId(), "AUDIT_SUCCESS",
            "商品审核通过", "您发布的「%s」已通过审核，现已上架销售！".formatted(event.productName()));
    } else {
        messageService.sendToUser(event.sellerId(), "AUDIT_REJECTED",
            "商品审核未通过", "您发布的「%s」未通过审核。原因：%s。请修改后重新提交。"
                .formatted(event.productName(), event.reason()));
    }
}
```

#### 消息模板（写入 V2 seed_data）

在 V2 的消息模板 INSERT 中追加两条：

```sql
(13, 'AUDIT_SUCCESS',   '审核通过通知', 'audit', '商品审核通过 🎉',   '您发布的「${productName}」已通过审核，现已上架销售！',        '["productName"]', 1, NOW(), NOW()),
(14, 'AUDIT_REJECTED', '审核驳回通知', 'audit', '商品审核未通过 ⚠️', '您发布的「${productName}」未通过审核。原因：${reason}。', '["productName","reason"]', 1, NOW(), NOW()),
```

---

## 5. 前端交互设计

### 5.1 改造范围总览

| 组件/文件 | 变更类型 | 说明 |
|-----------|---------|------|
| `admin/types/admin.ts` | **新增类型** | AuditLogVO, AuditDimension 等 |
| `admin/api/adminApi.ts` | **新增方法** | getAuditLogs(), 增强 auditProduct() |
| `admin/hooks/useAdminProductAudit.ts` | **增强** | 支持 dimensions/reason 参数 |
| `admin/pages/products/ProductReviewPage.tsx` | **增强** | 状态筛选器更新 + 默认待审核 |
| `admin/pages/products/ProductDetailDrawer.tsx` | **重构** | 维度勾选 + 原因输入 + 审核历史时间线 |
| 用户端商品页 | **增强** | 审核状态标签 + 重新提交按钮 |

### 5.2 类型定义

```typescript
// admin/types/admin.ts 新增

export type AuditAction = 1 | 2 | 3;
export type AuditDimension = 'basic' | 'compliance' | 'image' | 'price';

export interface AuditLogVO {
  id: number;
  productId: number;
  operatorId: number;
  operatorName: string;
  action: AuditAction;
  actionDesc: string;
  reason: string | null;
  dimensions: AuditDimension[];
  beforeStatus: number;
  beforeStatusDesc: string;
  afterStatus: number;
  afterStatusDesc: string;
  remark: string | null;
  createTime: string;
}

export interface ProductAuditRequest {
  action: 1 | 2;
  reason?: string;
  dimensions?: AuditDimension[];
  remark?: string;
}
```

### 5.3 状态筛选器更新

```typescript
const statusOptions: { value: number | ''; label: string }[] = [
  { value: '', label: '全部状态' },
  { value: 4, label: '待审核' },     // 新增
  { value: 5, label: '已驳回' },     // 新增
  { value: 0, label: '草稿' },
  { value: 1, label: '上架' },
  { value: 2, label: '已售' },
  { value: 3, label: '下架' },
];
```

默认行为：页面加载时默认选中 `status=4`（待审核），优先展示需处理的内容。

### 5.4 ProductDetailDrawer 审核操作区重构

#### 布局结构

```
┌──────────────────────────────────────────────┐
│  📋 审核操作                                  │
│                                              │
│  审核维度：                                   │
│  ☐ 基本信息合规    ☐ 内容无违规               │
│  ☐ 图片质量合格    ☐ 价格合理                 │
│                                              │
│  审核意见（选填）：                            │
│  ┌────────────────────────────────────────┐  │
│  │ ...                                    │  │
│  └────────────────────────────────────────┘  │
│                                              │
│  [✅ 通过审核]  [🚫 驳回商品]  [关闭]         │
└──────────────────────────────────────────────┘
```

#### 驳回确认弹窗增强

- 标题：「确认驳回商品」
- 必填原因输入框（textarea，最多 500 字）
- 快捷理由选项（标签式选择，可多选补充）：
  - 「信息不完整」「图片模糊」「疑似虚假信息」「价格异常」「违规内容」「其他」
- 提交后关闭抽屉并刷新列表

#### 审核历史时间线（新增面板）

位于商品描述区块下方，当 `auditLogs.length > 0` 时展示：

```
📜 审核记录
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
● 2026-05-12 14:30  管理员_张三  驳回
  原因：图片模糊，无法看清商品细节
  维度: 图片质量

● 2026-05-12 10:15  管理员_李四  驳回
  原因：价格明显低于市场价
  维度: 价格合理性

○ 2026-05-11 16:20  系统  提交审核
  卖家首次发布商品
```

时间线样式：
- 最新记录在最上方（倒序）
- 管理员操作用实心圆点 ●，系统操作用空心圆点 ○
- 通过操作用绿色标记，驳回用红色标记，提交用灰色标记

### 5.5 用户侧体验

#### 商品列表/详情页 — 审核状态标签

| 状态 | 标签样式 | 文案 | 可执行操作 |
|------|---------|------|-----------|
| PENDING_REVIEW(4) | 黄色底 + 时钟图标 | ⏳ 审核中 | 只读，不可编辑 |
| REJECTED(5) | 红色底 + X 图标 | 🔴 已驳回 | 「查看原因」+「修改并重新提交」 |
| DRAFT(0) | 灰色底 | 草稿 | 编辑 / 发布（→ 进入待审核） |
| ONLINE(1) | 绿色底 | 在售 | 正常操作 |

#### 重新提交按钮

在被驳回的商品详情页展示：
- 按钮：「修改并重新提交」
- 点击后调用 `PUT /api/products/{id}/submit`
- 成功后状态变为「审核中」，禁用编辑功能

---

## 6. 实施任务清单

### 后端任务（9 项）

| # | 模块 | 任务 | 涉及文件 |
|---|------|------|---------|
| B1 | DB | 修改 V1：追加 `eo_product_audit_log` 表 + 更新 status 注释 | `V1__init_schema.sql` |
| B2 | Domain | `ProductStatus` 枚举新增 `PENDING_REVIEW(4)`, `REJECTED(5)` + 状态转换方法 | `ProductStatus.java` |
| B3 | Domain | 新增 `ProductAuditedEvent` 领域事件 | `ProductAuditedEvent.java` |
| B4 | Persistence | 新增 `ProductAuditLogDO` + `ProductAuditLogMapper` | DO/Mapper |
| B5 | Admin DTO | 重构 `ProductAuditRequest`（action/dimensions/remark）+ 新增 `AuditLogVO` | DTO 文件 |
| B6 | Admin Service | 重构 `AdminProductAuditService`：完整状态校验→写日志→发事件 | Service |
| B7 | Admin Controller | 增强 audit 接口 + 新增 `GET /audit-logs` | Controller |
| B8 | User API | 新增 `PUT /products/{id}/submit` 重新提交接口 | ProductController |
| B9 | Event | 新增 `ProductAuditEventListener` → 消息模块发通知 | EventListener |

### 前端任务（6 项）

| # | 模块 | 任务 | 涉及文件 |
|---|------|------|---------|
| F1 | Types | admin.ts 新增 `AuditLogVO`, `ProductAuditRequest` 等类型 | `admin.ts` |
| F2 | API | adminApi.ts 新增 `getAuditLogs()` + 增强 `auditProduct()` | `adminApi.ts` |
| F3 | Hooks | useAdminProductAudit 增加 dimensions/reason 支持 | Hook |
| F4 | 审核页 | ProductReviewPage：状态筛选器 + 默认待审核 | Page |
| F5 | 抽屉 | ProductDetailDrawer：维度勾选 + 原因输入 + 审核时间线 | Drawer |
| F6 | 用户侧 | 商品列表/详情：审核状态标签 + 重新提交按钮 | 用户页面 |

### 配置任务（1 项）

| # | 任务 | 涉及文件 |
|---|------|---------|
| C1 | V2 追加审核通知消息模板（AUDIT_SUCCESS, AUDIT_REJECTED） | `V2__seed_data.sql` |

---

## 7. 不包含在本次范围

以下功能作为后续迭代考虑，不在本次实施范围内：

- 多级审批流程（初审 → 复审）
- 自动化审核规则 / AI 辅助审核
- 审核绩效统计报表
- 卖家端独立审核进度查询页
- 审核超时自动处理机制
