# EasyOrange 数据库说明文档

## 概述

| 项目 | 说明 |
|------|------|
| 数据库 | MySQL 8.4 (LTS) |
| 字符集 | utf8mb4 / utf8mb4_0900_ai_ci |
| 主键策略 | UUID v7（VARCHAR(36)），全库所有 ID 字段统一使用 UUID v7（RFC 9562） |
| 逻辑删除 | del_flag TINYINT（0 正常 / 1 删除） |
| 乐观锁 | version INT DEFAULT 0 |
| 时间精度 | 业务表 DATETIME，基础设施表 DATETIME(3) |
| 外键 | 无物理外键，通过应用层保证一致性 |
| 全文索引 | ngram 分词器（docker-compose.yml 已配置 ngram_token_size=2） |

## 表总览

共 28 张表：26 张 `eo_*` 业务/观测表（其中 3 张预留）+ 2 张 Spring Modulith 基础设施表（EVENT_PUBLICATION / EVENT_PUBLICATION_ARCHIVE）。`eo_idempotency_key` 已由 V2 迁移删除（幂等统一由 framework 的 `IdempotencyKeyFilter` + Redis 承载，2026-08 双 Token 收口）；`eo_ai_call_log` 由 V3 迁移新增（LLM-as-Judge 离线评估数据源，未提交前按 V3 计）。

| 模块 | 表名 | 说明 | 实体类 |
|------|------|------|--------|
| 用户 | eo_user | 用户信息 | UserDO |
| 用户 | eo_user_credit | 用户信用评分 | UserCreditDO |
| 用户 | eo_credit_change_log | 信用分变更流水（预留） | — |
| 商品 | eo_category | 商品分类（两级树） | CategoryDO |
| 商品 | eo_product | 商品信息 | ProductDO |
| 商品 | eo_product_detail | 商品详情（1:1） | ProductDetailDO |
| 商品 | eo_product_image | 商品图片（1:N） | ProductImageDO |
| 商品 | eo_product_audit_log | 商品审核记录 | — |
| 商品 | eo_audit_suggestion | AI 审核建议（预留） | — |
| 商品 | eo_product_review | 商品评价 | ProductReviewDO |
| 商品 | eo_product_report | 商品举报 | ProductReportDO |
| 商品 | eo_report_handle_history | 举报处理历史 | ReportHandleHistoryDO |
| 商品 | eo_product_question | 商品问答（预留） | — |
| 商品 | eo_favorite | 用户收藏 | FavoriteDO |
| 搜索 | eo_search_history | 搜索历史 | SearchHistoryDO |
| 搜索 | eo_hot_keyword | 热门关键词 | HotKeywordDO |
| 订单 | eo_order | 订单 | OrderDO |
| 订单 | eo_order_item | 订单行项 | OrderItemDO |
| 支付 | eo_payment | 支付记录 | PaymentDO |
| 支付 | eo_payment_config | 支付渠道配置 | PaymentConfigDO |
| 消息 | eo_message | 消息 | MessageDO |
| 消息 | eo_message_archive | 消息归档 | — |
| 消息 | eo_offline_message | 离线消息 | OfflineMessageDO |
| 文件 | eo_upload_file | 文件上传记录 | UploadFileDO |
| 审计 | eo_audit_log | 审计日志 | AuditLog |
| 事件 | EVENT_PUBLICATION | 领域事件注册表（Spring Modulith） | Modulith |
| 事件 | EVENT_PUBLICATION_ARCHIVE | 领域事件归档表（Spring Modulith） | Modulith |
| 观测 | eo_ai_call_log | AI 调用日志（LLM-as-Judge 数据源，V3 新增） | —（JDBC 直写） |

## 公共字段

业务表统一继承以下公共字段：

| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| create_time | DATETIME | CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | CURRENT_TIMESTAMP ON UPDATE | 更新时间 |
| create_by | VARCHAR(36) | NULL | 创建人 ID |
| update_by | VARCHAR(36) | NULL | 更新人 ID |
| del_flag | TINYINT | 0 | 逻辑删除（0 正常 / 1 删除） |
| version | INT | 0 | 乐观锁版本号 |

基础设施表（EVENT_PUBLICATION / EVENT_PUBLICATION_ARCHIVE / eo_ai_call_log）使用 created_at / updated_at 时间字段，精度为毫秒 DATETIME(3)。

归档表（eo_message_archive）无 del_flag / version，使用 archived_at 记录归档时间。

eo_audit_log 无 del_flag / version / create_by / update_by，使用独立主键 id 和时间字段 created_at。

---

## 详细表结构

### 1. eo_user — 用户信息表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| user_id | VARCHAR(36) | PK | 用户 ID |
| username | VARCHAR(30) | NOT NULL, UK | 用户账号 |
| password | VARCHAR(100) | NOT NULL | 密码（BCrypt） |
| user_type | VARCHAR(2) | NOT NULL DEFAULT '01' | 用户类型（01 普通 / 02 管理员） |
| email | VARCHAR(255) | UK | 邮箱 |
| phone | VARCHAR(20) | UK | 手机号码 |
| student_id | VARCHAR(20) | UK | 学号 |
| real_name | VARCHAR(30) | | 真实姓名 |
| nick_name | VARCHAR(30) | | 用户昵称 |
| avatar | VARCHAR(500) | | 头像 URL |
| sex | TINYINT | NOT NULL DEFAULT 0 | 性别（0 未知 / 1 男 / 2 女） |
| status | VARCHAR(20) | NOT NULL DEFAULT 'NORMAL' | 状态（NORMAL 正常 / DISABLED 禁用 / LOCKED 锁定） |
| login_ip | VARCHAR(128) | | 最后登录 IP |
| login_date | DATETIME | | 最后登录时间 |
| pwd_update_date | DATETIME | | 密码更新时间 |
| remark | VARCHAR(500) | | 备注 |
| + 公共字段 | | | |

**索引**：

| 名称 | 类型 | 列 |
|------|------|----|
| uk_eo_user_username | UNIQUE | username |
| uk_eo_user_email | UNIQUE | email |
| uk_eo_user_phone | UNIQUE | phone |
| uk_eo_user_student_id | UNIQUE | student_id |
| idx_eo_user_status_del | KEY | status, del_flag, create_time DESC |
| idx_eo_user_type_status | KEY | user_type, status, del_flag |

**CHECK 约束**：status IN ('NORMAL','DISABLED','LOCKED'), sex IN (0,1,2), user_type IN ('01','02')

---

### 2. eo_user_credit — 用户信用评分表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | VARCHAR(36) | PK | 主键 ID |
| user_id | VARCHAR(36) | NOT NULL, UK | 用户 ID |
| credit_score | INT | NOT NULL DEFAULT 100 | 信用评分（0-200） |
| level | VARCHAR(20) | NOT NULL DEFAULT 'NORMAL' | 信用等级（EXCELLENT / GOOD / NORMAL / LOW / BLACKLIST） |
| total_trades | INT | NOT NULL DEFAULT 0 | 总交易数 |
| completed_trades | INT | NOT NULL DEFAULT 0 | 已完成交易数 |
| cancelled_trades | INT | NOT NULL DEFAULT 0 | 已取消交易数 |
| total_reports | INT | NOT NULL DEFAULT 0 | 总举报数 |
| confirmed_reports | INT | NOT NULL DEFAULT 0 | 已确认举报数 |
| review_avg_rating | DECIMAL(3,2) | | 评价平均分 |
| last_updated | DATETIME | | 最后评分更新时间 |
| + 公共字段 | | | |

**索引**：

| 名称 | 类型 | 列 |
|------|------|----|
| uk_eo_user_credit_user_id | UNIQUE | user_id |
| idx_eo_user_credit_score | KEY | credit_score |
| idx_eo_user_credit_level | KEY | level |
| idx_eo_user_credit_last_updated | KEY | last_updated |

**CHECK 约束**：credit_score 0-200, total_trades>=0, completed_trades>=0, cancelled_trades>=0, total_reports>=0, confirmed_reports>=0

---

### 3. eo_credit_change_log — 信用分变更流水表（预留）

> **预留**：schema 已建表，代码未接入。信用分重算（`CreditScoringService`）当前只写 `eo_user_credit`，未落变更流水；该表为后续信用变更审计预留。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | VARCHAR(36) | PK | 主键 ID |
| user_id | VARCHAR(36) | NOT NULL | 用户 ID |
| change_amount | INT | NOT NULL | 变更分值 |
| before_score | INT | NOT NULL | 变更前评分 |
| after_score | INT | NOT NULL | 变更后评分 |
| change_type | VARCHAR(30) | NOT NULL | 变更类型（TRADE_COMPLETE/TRADE_CANCEL/REPORT_CONFIRMED/REVIEW_RATING/RECALCULATE/ADMIN_ADJUST） |
| reason | VARCHAR(500) | | 变更原因 |
| reference_id | VARCHAR(36) | | 关联业务 ID（订单ID/举报ID等） |
| create_by | VARCHAR(36) | | 操作人/系统 ID |
| create_time | DATETIME | NOT NULL DEFAULT CURRENT_TIMESTAMP | 创建时间 |

**索引**：

| 名称 | 类型 | 列 |
|------|------|----|
| idx_eo_credit_change_log_user_id | KEY | user_id |
| idx_eo_credit_change_log_type_time | KEY | change_type, create_time DESC |
| idx_eo_credit_change_log_create_time | KEY | create_time DESC |

---

### 4. eo_category — 商品分类表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | VARCHAR(36) | PK | 主键 ID |
| name | VARCHAR(50) | NOT NULL | 分类名称 |
| parent_id | VARCHAR(36) | NOT NULL DEFAULT 0 | 父分类 ID（0=顶级） |
| level | TINYINT | NOT NULL DEFAULT 1 | 层级（1/2） |
| icon | VARCHAR(255) | | 图标 |
| sort_order | INT | NOT NULL DEFAULT 0 | 排序 |
| status | TINYINT | NOT NULL DEFAULT 1 | 状态（0 禁用 / 1 启用） |
| + 公共字段 | | | |

**索引**：

| 名称 | 类型 | 列 |
|------|------|----|
| idx_eo_category_parent_id | KEY | parent_id |
| idx_eo_category_status_sort | KEY | status, del_flag, sort_order |

**分类树结构**：

```
电子数码(1) ─┬─ 手机(10) ─── 智能穿戴(13)
             ├─ 电脑(11) ─── 游戏设备(14)
             └─ 耳机音箱(12)
书籍教材(2) ─┬─ 教材(20) ─── 考研资料(21)
             └─ 课外读物(22)
服饰鞋包(3) ─┬─ 鞋靴(30) ─── 服装(31) ─── 箱包(32)
生活用品(4) ─┬─ 宿舍资产(40) ─ 数码配件(41)
运动健身(5) ─┬─ 健身器材(50) ─ 户外运动(51)
虚拟物品(6) ─┬─ 游戏账号(60) ─ 会员卡券(61)
```

---

### 5. eo_product — 商品信息表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | VARCHAR(36) | PK | 主键 ID |
| user_id | VARCHAR(36) | NOT NULL | 发布者 ID |
| category_id | VARCHAR(36) | | 分类 ID |
| name | VARCHAR(100) | NOT NULL | 商品名称 |
| price | DECIMAL(10,2) | NOT NULL | 售价 |
| original_price | DECIMAL(10,2) | | 原价 |
| stock | INT | NOT NULL DEFAULT 1 | 库存 |
| status | VARCHAR(20) | NOT NULL DEFAULT 'DRAFT' | 状态（DRAFT 草稿 / ONLINE 上架 / SOLD 已售 / OFFLINE 下架 / PENDING_REVIEW 待审核 / REJECTED 已驳回） |
| view_count | INT | NOT NULL DEFAULT 0 | 浏览次数 |
| condition_level | TINYINT | | 新旧程度（1-10） |
| location | VARCHAR(100) | | 交易地点 |
| contact_method | VARCHAR(200) | | 联系方式 |
| tags | VARCHAR(500) | | 标签 |
| search_text | TEXT | | 搜索冗余文本 |
| price_update_time | DATETIME | | 价格更新时间 |
| + 公共字段 | | | |

**索引**：

| 名称 | 类型 | 列 |
|------|------|----|
| idx_eo_product_category_status_time | KEY | category_id, status, del_flag, create_time DESC |
| idx_eo_product_search | KEY | status, del_flag, category_id, create_time DESC |
| idx_eo_product_status_del_price | KEY | status, del_flag, price |
| idx_eo_product_status_del_view | KEY | status, del_flag, view_count DESC |
| idx_eo_product_status_del_create_time | KEY | status, del_flag, create_time DESC |
| idx_eo_product_user_status_del | KEY | user_id, status, del_flag, create_time DESC |
| ft_eo_product_name | FULLTEXT(ngram) | name |
| ft_eo_product_search_text | FULLTEXT(ngram) | search_text |

**CHECK 约束**：price>=0, original_price>=0, stock>=0, status IN ('DRAFT','ONLINE','SOLD','OFFLINE','PENDING_REVIEW','REJECTED'), condition_level 1-10, view_count>=0

---

### 6. eo_product_detail — 商品详情表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| product_id | VARCHAR(36) | PK | 商品 ID（与 eo_product.id 1:1） |
| description | TEXT | | 详情描述 |
| + 公共字段 | | | |

---

### 7. eo_product_image — 商品图片表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | VARCHAR(36) | PK | 主键 ID |
| product_id | VARCHAR(36) | NOT NULL | 商品 ID |
| image_url | VARCHAR(500) | NOT NULL | 图片 URL |
| sort_order | INT | NOT NULL DEFAULT 0 | 排序 |
| is_main | TINYINT | NOT NULL DEFAULT 0 | 是否主图（0 否 / 1 是） |
| + 公共字段 | | | |

**索引**：idx_eo_product_image_product_sort (product_id, sort_order)

---

### 8. eo_product_audit_log — 商品审核记录表

> 无公共字段（审核记录不需要 del_flag/version，通过业务逻辑保证不可变）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | VARCHAR(36) | PK | 主键 ID |
| product_id | VARCHAR(36) | NOT NULL | 商品 ID |
| operator_id | VARCHAR(36) | NOT NULL | 操作人 ID |
| operator_name | VARCHAR(50) | NOT NULL | 操作人姓名 |
| action | TINYINT | NOT NULL | 审核动作（1 通过 / 2 拒绝 / 3 重新提交） |
| reason | VARCHAR(500) | | 审核原因 |
| audit_dimensions | VARCHAR(500) | | 审核维度 JSON |
| before_status | VARCHAR(20) | NOT NULL | 操作前状态 |
| after_status | VARCHAR(20) | NOT NULL | 操作后状态 |
| remark | VARCHAR(500) | | 管理员备注 |
| create_time | DATETIME | NOT NULL DEFAULT CURRENT_TIMESTAMP | 创建时间 |

**索引**：

| 名称 | 类型 | 列 |
|------|------|----|
| idx_audit_product | KEY | product_id, create_time DESC |
| idx_audit_operator | KEY | operator_id, create_time DESC |
| idx_audit_action_time | KEY | action, create_time DESC |

---

### 9. eo_audit_suggestion — AI 审核建议表（预留）

> **预留**：schema 已建表，代码未接入。用于 AI 商品审核建议（价格/描述/分类/图片维度），当前无对应实现。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | VARCHAR(36) | PK | 主键 ID |
| product_id | VARCHAR(36) | NOT NULL | 商品 ID |
| suggestion_type | VARCHAR(50) | NOT NULL | 建议类型（PRICE_AUDIT/DESCRIPTION_AUDIT/CATEGORY_AUDIT/IMAGE_AUDIT） |
| suggestion_content | JSON | | 建议内容（JSON） |
| confidence | DECIMAL(5,2) | NOT NULL DEFAULT 0.00 | 置信度（0.00-1.00） |
| status | TINYINT | NOT NULL DEFAULT 0 | 状态（0 待处理 1 已采纳 2 已忽略） |
| create_time | DATETIME | NOT NULL DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | 处理时间 |
| create_by | VARCHAR(36) | | 创建者 |
| update_by | VARCHAR(36) | | 处理者 |
| del_flag | TINYINT | NOT NULL DEFAULT 0 | 删除标志（0 正常 1 删除） |
| version | INT | NOT NULL DEFAULT 0 | 乐观锁版本号 |

**索引**：

| 名称 | 类型 | 列 |
|------|------|----|
| idx_eo_audit_suggestion_product_id | KEY | product_id |
| idx_eo_audit_suggestion_type_status | KEY | suggestion_type, status, create_time DESC |

---

### 10. eo_product_review — 商品评价表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | VARCHAR(36) | PK | 主键 ID |
| product_id | VARCHAR(36) | NOT NULL | 商品 ID |
| user_id | VARCHAR(36) | NOT NULL | 评价用户 ID |
| order_id | VARCHAR(36) | NOT NULL | 关联订单 ID |
| rating | TINYINT | NOT NULL DEFAULT 5 | 评分（1-5） |
| content | TEXT | NOT NULL | 评价内容 |
| reply_content | TEXT | | 资产方回复内容 |
| reply_time | DATETIME | | 资产方回复时间 |
| likes | INT | NOT NULL DEFAULT 0 | 点赞数 |
| status | TINYINT | NOT NULL DEFAULT 1 | 状态（0 隐藏 / 1 显示 / 2 待审核） |
| + 公共字段 | | | |

**索引**：

| 名称 | 类型 | 列 |
|------|------|----|
| uk_eo_product_review_user_order | UNIQUE | user_id, order_id |
| idx_eo_product_review_order_id | KEY | order_id |
| idx_eo_product_review_product_status_del_time | KEY | product_id, status, del_flag, create_time DESC |

**CHECK 约束**：rating 1-5, status IN (0,1,2), likes>=0

**业务约束**：同一用户对同一订单只能评价一次（唯一约束）

---

### 11. eo_product_report — 商品举报表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | VARCHAR(36) | PK | 主键 ID |
| product_id | VARCHAR(36) | NOT NULL | 被举报商品 ID |
| reporter_id | VARCHAR(36) | NOT NULL | 举报人 ID |
| reason | VARCHAR(500) | NOT NULL | 举报原因 |
| status | TINYINT | NOT NULL DEFAULT 0 | 状态（0 待处理 / 1 已处理 / 2 已忽略） |
| handle_result | VARCHAR(500) | | 处理结果 |
| + 公共字段 | | | |

**索引**：

| 名称 | 类型 | 列 |
|------|------|----|
| idx_eo_product_report_product_id | KEY | product_id |
| idx_eo_product_report_reporter_id | KEY | reporter_id |
| idx_eo_product_report_status_time | KEY | status, create_time DESC |

---

### 12. eo_report_handle_history — 举报处理历史表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | VARCHAR(36) | PK | 主键 ID |
| report_id | VARCHAR(36) | NOT NULL | 举报 ID |
| operator_id | VARCHAR(36) | NOT NULL | 操作人 ID |
| action | VARCHAR(30) | NOT NULL | 动作类型（IGNORE / PRODUCT_OFFLINE / WARN_SENDER / BAN_PRODUCT） |
| remark | VARCHAR(500) | | 备注 |
| + 公共字段 | | | |

**索引**：

| 名称 | 类型 | 列 |
|------|------|----|
| idx_eo_report_handle_history_report_id | KEY | report_id |
| idx_eo_report_handle_history_operator_id | KEY | operator_id |

---

### 13. eo_product_question — 商品问答表（预留）

> **预留**：schema 已建表，代码未接入。`AiQaService.answerQuestion()` 当前为内存回答、不落库；该表用于商品问答持久化。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | VARCHAR(36) | PK | 主键 ID |
| product_id | VARCHAR(36) | NOT NULL | 商品 ID |
| user_id | VARCHAR(36) | NOT NULL | 用户 ID |
| question | TEXT | NOT NULL | 问题内容 |
| answer | TEXT | | AI 回答内容 |
| status | TINYINT | NOT NULL DEFAULT 0 | 状态（0 待回答 1 已回答 2 已驳回） |
| create_time | DATETIME | NOT NULL DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | 更新时间 |
| create_by | VARCHAR(36) | | 创建者 |
| update_by | VARCHAR(36) | | 更新者 |
| del_flag | TINYINT | NOT NULL DEFAULT 0 | 删除标志（0 正常 1 删除） |
| version | INT | NOT NULL DEFAULT 0 | 乐观锁版本号 |

**索引**：

| 名称 | 类型 | 列 |
|------|------|----|
| idx_eo_product_question_product_id | KEY | product_id |
| idx_eo_product_question_user_id | KEY | user_id |
| idx_eo_product_question_status_time | KEY | status, create_time DESC |

---

### 14. eo_favorite — 用户收藏表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | VARCHAR(36) | PK | 主键 ID |
| user_id | VARCHAR(36) | NOT NULL | 用户 ID |
| product_id | VARCHAR(36) | NOT NULL | 商品 ID |
| + 公共字段 | | | |

**索引**：

| 名称 | 类型 | 列 |
|------|------|----|
| uk_eo_favorite_user_product_del | UNIQUE | user_id, product_id, del_flag |
| idx_eo_favorite_user_time | KEY | user_id, create_time DESC |
| idx_eo_favorite_product_count | KEY | product_id, del_flag |

---

### 15. eo_search_history — 搜索历史表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | VARCHAR(36) | PK | 主键 ID |
| user_id | VARCHAR(36) | NOT NULL | 用户 ID |
| keyword | VARCHAR(100) | NOT NULL | 搜索关键词 |
| search_time | DATETIME | NOT NULL DEFAULT CURRENT_TIMESTAMP | 搜索时间 |
| + 公共字段 | | | |

**索引**：

| 名称 | 类型 | 列 |
|------|------|----|
| uk_eo_search_history_user_keyword | UNIQUE | user_id, keyword |
| idx_eo_search_history_user_time | KEY | user_id, search_time DESC |
| idx_eo_search_history_keyword | KEY | keyword |

---

### 16. eo_hot_keyword — 热门关键词表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | VARCHAR(36) | PK | 主键 ID |
| keyword | VARCHAR(100) | NOT NULL, UK | 关键词 |
| search_count | INT | NOT NULL DEFAULT 0 | 搜索次数 |
| last_search_time | DATETIME | | 最后搜索时间 |
| + 公共字段 | | | |

**索引**：

| 名称 | 类型 | 列 |
|------|------|----|
| uk_eo_hot_keyword_keyword | UNIQUE | keyword |
| idx_eo_hot_keyword_count | KEY | search_count DESC |
| idx_eo_hot_keyword_last_time | KEY | last_search_time |

**CHECK 约束**：search_count >= 0

---

### 17. eo_order_item — 订单行项表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | VARCHAR(36) | PK | 主键 ID |
| order_id | VARCHAR(36) | NOT NULL | 所属订单 ID |
| product_id | VARCHAR(36) | NOT NULL | 商品 ID |
| product_snapshot | JSON | | 下单时商品快照 |
| unit_price | DECIMAL(10,2) | NOT NULL | 单价 |
| quantity | INT | NOT NULL DEFAULT 1 | 数量 |
| subtotal | DECIMAL(10,2) | NOT NULL | 小计金额 |
| + 公共字段 | | | |

**索引**：

| 名称 | 类型 | 列 |
|------|------|----|
| idx_eo_order_item_order_id | KEY | order_id |
| idx_eo_order_item_product_id | KEY | product_id |
| idx_eo_order_item_create_time | KEY | create_time DESC |

**CHECK 约束**：unit_price>=0, quantity>0, subtotal>=0, subtotal=unit_price*quantity

---

### 18. eo_order — 订单表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | VARCHAR(36) | PK | 主键 ID |
| order_no | VARCHAR(64) | NOT NULL, UK | 订单号 |
| buyer_id | VARCHAR(36) | NOT NULL | 认领方 ID |
| seller_id | VARCHAR(36) | NOT NULL | 资产方 ID |
| total_amount | DECIMAL(10,2) | NOT NULL | 订单总金额（行项总和） |
| status | TINYINT | NOT NULL DEFAULT 0 | 状态（0 待付款 / 1 待发货 / 2 待收货 / 3 已完成 / 4 已取消 / 5 已退款） |
| payment_status | TINYINT | NOT NULL DEFAULT 0 | 支付状态（0 未支付 / 1 已支付 / 2 已退款） |
| address | VARCHAR(500) | | 收货地址 |
| phone | VARCHAR(20) | | 联系电话 |
| remark | VARCHAR(500) | | 备注 |
| cancel_reason | VARCHAR(500) | | 取消原因 |
| cancel_time | DATETIME | | 取消时间 |
| refund_reason | VARCHAR(500) | | 退款原因 |
| refund_time | DATETIME | | 退款时间 |
| + 公共字段 | | | |

**索引**：

| 名称 | 类型 | 列 |
|------|------|----|
| uk_eo_order_order_no | UNIQUE | order_no |
| idx_eo_order_buyer_status_time | KEY | buyer_id, status, del_flag, create_time DESC |
| idx_eo_order_seller_status_time | KEY | seller_id, status, del_flag, create_time DESC |
| idx_eo_order_status_payment | KEY | status, payment_status, create_time DESC |

**CHECK 约束**：total_amount>=0, status IN (0,1,2,3,4,5), payment_status IN (0,1,2)

**状态流转**：

```
待付款(0) ─┬─ 待发货(1) ─── 待收货(2) ─── 已完成(3)
           ├─ 已取消(4)
           └─ 已退款(5)
```

---

### 19. eo_payment — 支付记录表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | VARCHAR(36) | PK | 主键 ID |
| payment_no | VARCHAR(64) | NOT NULL, UK | 支付流水号 |
| order_id | VARCHAR(36) | NOT NULL | 订单 ID |
| user_id | VARCHAR(36) | NOT NULL | 用户 ID |
| amount | DECIMAL(10,2) | NOT NULL | 支付金额 |
| refunded_amount | DECIMAL(10,2) | NOT NULL DEFAULT 0 | 已退款金额 |
| payment_method | TINYINT | | 支付方式（1 微信 / 2 支付宝 / 3 余额） |
| status | TINYINT | NOT NULL DEFAULT 0 | 状态（0 待支付 / 1 已支付 / 2 已退款 / 3 部分退款 / 4 失败 / 5 已关闭 / 6 支付中 / 7 退款中） |
| transaction_id | VARCHAR(64) | UK | 第三方流水号 |
| refund_reason | VARCHAR(500) | | 退款原因 |
| refund_time | DATETIME | | 退款时间 |
| attach | TEXT | | 附加数据 |
| + 公共字段 | | | |

**索引**：

| 名称 | 类型 | 列 |
|------|------|----|
| uk_eo_payment_payment_no | UNIQUE | payment_no |
| uk_eo_payment_transaction_id | UNIQUE | transaction_id |
| idx_eo_payment_order_id | KEY | order_id |
| idx_eo_payment_status_method | KEY | status, payment_method, create_time DESC |
| idx_eo_payment_user_status | KEY | user_id, status, create_time DESC |

**CHECK 约束**：amount>=0, refunded_amount>=0, status IN (0,1,2,3,4,5,6,7), payment_method IN (1,2,3)

---

### 20. eo_payment_config — 支付渠道配置表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | VARCHAR(36) | PK | 主键 ID |
| channel_code | VARCHAR(50) | NOT NULL, UK | 渠道编码 |
| channel_name | VARCHAR(100) | NOT NULL | 渠道名称 |
| app_id | VARCHAR(100) | | 应用 ID |
| private_key | TEXT | | 商户私钥 |
| public_key | TEXT | | 商户公钥 |
| sandbox | TINYINT | NOT NULL DEFAULT 0 | 是否沙箱 |
| status | TINYINT | NOT NULL DEFAULT 1 | 状态（0 禁用 / 1 启用） |
| remark | VARCHAR(500) | | 备注 |
| + 公共字段 | | | |

---

### 21. eo_message — 消息表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | VARCHAR(36) | PK | 主键 ID |
| sender_id | VARCHAR(36) | | 发送者 ID（NULL=系统消息） |
| receiver_id | VARCHAR(36) | NOT NULL | 接收者 ID |
| type | TINYINT | NOT NULL DEFAULT 0 | 消息类型（0 系统 / 1 私聊 / 2 订单） |
| title | VARCHAR(200) | | 标题 |
| content | TEXT | NOT NULL | 内容 |
| is_read | TINYINT | NOT NULL DEFAULT 0 | 是否已读（0 未读 / 1 已读） |
| read_time | DATETIME | | 已读时间 |
| business_id | VARCHAR(36) | | 业务 ID |
| conversation_id | VARCHAR(36) | | 会话 ID |
| msg_status | VARCHAR(20) | NOT NULL DEFAULT 'SENT' | 消息状态（SENT / DELIVERED / READ / RECALLED） |
| recalled_at | DATETIME | | 撤回时间 |
| + 公共字段 | | | |

**索引**：

| 名称 | 类型 | 列 |
|------|------|----|
| idx_eo_message_sender_time | KEY | sender_id, create_time DESC |
| idx_eo_message_receiver_read_type_del_time | KEY | receiver_id, is_read, del_flag, type, create_time DESC |
| idx_eo_message_business_id | KEY | business_id |
| idx_eo_message_conversation_time | KEY | conversation_id, create_time DESC |

---

### 22. eo_message_archive — 消息归档表

结构与 eo_message 相同，主键为 id（原消息 ID），额外增加 archived_at DATETIME 字段。无 del_flag / version。用于存储已归档的历史消息。

---

### 23. eo_offline_message — 离线消息表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | VARCHAR(36) | PK | 主键 ID |
| user_id | VARCHAR(36) | NOT NULL | 用户 ID |
| message_id | VARCHAR(36) | NOT NULL | 消息 ID |
| push_channel | VARCHAR(50) | NOT NULL | 推送渠道 |
| push_status | TINYINT | NOT NULL DEFAULT 0 | 推送状态（0 待推送 / 1 已推送 / 2 失败） |
| push_time | DATETIME | | 推送时间 |
| retry_count | INT | NOT NULL DEFAULT 0 | 重试次数 |
| max_retry_count | INT | NOT NULL DEFAULT 3 | 最大重试次数 |
| last_retry_time | DATETIME | | 最后重试时间 |
| + 公共字段 | | | |

**索引**：

| 名称 | 类型 | 列 |
|------|------|----|
| idx_eo_offline_message_user_status | KEY | user_id, push_status |
| idx_eo_offline_message_message_id | KEY | message_id |
| idx_eo_offline_message_retry | KEY | push_status, retry_count, create_time DESC |

---

### 24. eo_upload_file — 文件上传记录表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | VARCHAR(36) | PK | 主键 ID |
| file_name | VARCHAR(200) | NOT NULL | 文件名 |
| file_path | VARCHAR(500) | NOT NULL | 存储路径 |
| file_url | VARCHAR(500) | | 访问 URL |
| file_size | BIGINT | | 文件大小（字节） |
| file_type | VARCHAR(50) | | 扩展名 |
| mime_type | VARCHAR(100) | | MIME 类型 |
| md5 | VARCHAR(32) | | MD5 校验 |
| storage_type | VARCHAR(32) | NOT NULL DEFAULT 'LOCAL' | 存储类型（LOCAL/S3/OSS） |
| storage_key | VARCHAR(500) | | 存储后端标识键 |
| business_type | VARCHAR(50) | | 业务类型 |
| business_id | VARCHAR(36) | | 业务 ID |
| uploader_id | VARCHAR(36) | | 上传者 ID |
| status | TINYINT | NOT NULL DEFAULT 1 | 状态（0 禁用 / 1 正常） |
| + 公共字段 | | | |

**索引**：idx_eo_upload_file_md5 (md5), idx_eo_upload_file_business (business_type, business_id), idx_eo_upload_file_uploader (uploader_id)

---

### 25. eo_audit_log — 审计日志表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | VARCHAR(36) | PK | 日志主键 |
| title | VARCHAR(50) | | 模块标题 |
| business_type | VARCHAR(50) | NOT NULL DEFAULT '0' | 业务类型 |
| method | VARCHAR(100) | | 方法名称 |
| request_method | VARCHAR(10) | | 请求方式 |
| operator_type | TINYINT | NOT NULL DEFAULT 0 | 操作类别 |
| username | VARCHAR(50) | | 操作人员 |
| request_url | VARCHAR(255) | | 请求 URL |
| client_ip | VARCHAR(128) | | 客户端 IP |
| request_params | TEXT | | 请求参数（敏感字段已掩码） |
| response_data | TEXT | | 响应数据 |
| status | TINYINT | NOT NULL DEFAULT 0 | 状态（0 正常 / 1 异常） |
| error_msg | VARCHAR(2000) | | 错误消息 |
| created_at | DATETIME | NOT NULL DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| duration | INT | NOT NULL DEFAULT 0 | 执行耗时（毫秒） |

**索引**：

| 名称 | 类型 | 列 |
|------|------|----|
| idx_created_at | KEY | created_at |
| idx_username_created_at | KEY | username, created_at DESC |
| idx_business_type_created_at | KEY | business_type, created_at DESC |
| idx_status_created_at | KEY | status, created_at DESC |

**注意**：此表无 del_flag / version / create_by / update_by，不继承公共字段。

---

### eo_domain_event — 领域事件表（已删除）

> **注意**：此表在 V1 初始化时创建，用于 Outbox 模式。已通过 Flyway V4 在 2026-07-14 清理（`DROP TABLE IF EXISTS eo_domain_event`）。当前使用 Spring Modulith 的 `EVENT_PUBLICATION` 表代替。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | VARCHAR(36) | PK | 主键 ID |
| event_id | CHAR(36) | NOT NULL, UK | 事件 UUID |
| aggregate_type | VARCHAR(100) | NOT NULL | 聚合类型 |
| aggregate_id | VARCHAR(36) | NOT NULL | 聚合 ID |
| event_type | VARCHAR(100) | NOT NULL | 事件类型 |
| payload | TEXT | | 事件载荷（JSON） |
| status | VARCHAR(20) | NOT NULL DEFAULT 'PENDING' | 状态（PENDING / PUBLISHED / FAILED） |
| created_at | DATETIME(3) | NOT NULL DEFAULT CURRENT_TIMESTAMP(3) | 事件创建时间 |
| published_at | DATETIME(3) | | 事件发布时间 |
| + 公共字段 | | | |

**索引**：

| 名称 | 类型 | 列 |
|------|------|----|
| uk_eo_domain_event_event_id | UNIQUE | event_id |
| idx_eo_domain_event_aggregate | KEY | aggregate_type, aggregate_id |
| idx_eo_domain_event_status_created | KEY | status, created_at |
| idx_eo_domain_event_event_type | KEY | event_type |

**替代实现**：Spring Modulith 的 `EVENT_PUBLICATION` 表（V1 初始化脚本创建）—— ModulithDomainEventPublisher 在应用事务中写入该表，提交后异步读取并发布到 RabbitMQ，实现 at-least-once 语义。

---

### eo_idempotency_key — 幂等性键表（已删除）

> **注意**：此表已在 V1 初始化时创建，后由 **V2 迁移删除**（2026-08，双 Token 现代化收口 b5f0f879）——幂等保护统一由 framework 的 `IdempotencyKeyFilter` + Redis 实现承载，DB 表不再需要。下表仅为历史记录，当前数据库无此表。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | VARCHAR(36) | PK | 主键 ID |
| idempotency_key | VARCHAR(255) | NOT NULL, UK | 幂等性键 |
| user_id | VARCHAR(36) | NOT NULL | 用户 ID |
| request_hash | VARCHAR(64) | NOT NULL | 请求哈希 |
| response_data | TEXT | | 响应数据（JSON） |
| status | VARCHAR(20) | NOT NULL DEFAULT 'PENDING' | 状态（PENDING / COMPLETED / FAILED） |
| expires_at | DATETIME | NOT NULL | 过期时间 |
| + 公共字段 | | | |

**索引**：uk_eo_idempotency_key_key (idempotency_key), idx_eo_idempotency_key_user_expires (user_id, expires_at)

---

## 索引命名规范

| 类型 | 格式 | 示例 |
|------|------|------|
| 主键 | PK | 自动 PRIMARY KEY |
| 唯一索引 | uk_eo_{table}_{columns} | uk_eo_user_username |
| 普通索引 | idx_eo_{table}_{columns} | idx_eo_product_user_id |
| 全文索引 | ft_eo_{table}_{column} | ft_eo_product_name |
| CHECK 约束 | chk_eo_{table}_{column} | chk_eo_user_status |

## 数据类型规范

| 场景 | 类型 | 示例 |
|------|------|------|
| 主键 | VARCHAR(36) | id VARCHAR(36) NOT NULL |
| 状态/标志 | TINYINT | status TINYINT NOT NULL DEFAULT 0 |
| 金额 | DECIMAL(10,2) | price DECIMAL(10,2) NOT NULL |
| 短文本 | VARCHAR(30-200) | username VARCHAR(30) |
| 长文本 | TEXT | description TEXT |
| 时间（业务） | DATETIME | create_time DATETIME |
| 时间（基础设施） | DATETIME(3) | created_at DATETIME(3) |
| UUID | VARCHAR(36) | conversation_id VARCHAR(36) |
| 布尔 | TINYINT | is_main TINYINT DEFAULT 0 |
| 文件大小 | BIGINT | file_size BIGINT |

## 表关系图

```
eo_user ──1:N── eo_product (user_id)
              ├──1:N── eo_product_image (product_id)
              ├──1:1── eo_product_detail (product_id)
              ├──1:N── eo_product_review (product_id)
              ├──1:N── eo_product_report (product_id)
              │   └──1:N── eo_report_handle_history (report_id)
              └──1:N── eo_favorite (user_id + product_id)

eo_category ──1:N── eo_product (category_id)
    └──自引用── eo_category (parent_id)

eo_user ──1:N── eo_order (buyer_id / seller_id)
eo_user ──1:1── eo_user_credit (user_id)
eo_order ──1:N── eo_order_item (order_id)
eo_product ──1:N── eo_order_item (product_id)
eo_order ──1:1── eo_payment (order_id)
eo_order ──1:N── eo_product_review (order_id)

eo_user ──1:N── eo_message (sender_id / receiver_id)
eo_user ──1:N── eo_search_history (user_id)
eo_user ──1:N── eo_offline_message (user_id)

eo_message ──1:1── eo_message_archive (id)

```

---

### eo_ai_call_log — AI 调用日志表（V3 新增）

> **现状**：`AiCallLogRecorder`（easyorange-ai/adapter/outbound/）在每次 LLM/Embedding 调用后 JDBC 直写一条（记录失败仅告警，不阻塞主链路）；`AiEvalScheduler`（adapter/inbound/job/）定时对 `judge_score IS NULL AND success = 1` 的记录用 ChatModel 打分（1-5 + 评语）。默认关闭（`easyorange.ai.eval.enabled=false`）。

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | VARCHAR(36) | PK | 主键 UUID v7 |
| scope | VARCHAR(32) | NOT NULL | AI 调用场景（PRICING/REVIEW/COPY/AUTO_LISTING/SEMANTIC/QA/SEARCH_ENHANCE） |
| model | VARCHAR(64) | NOT NULL | 模型标识 |
| prompt_hash | CHAR(32) | NOT NULL | system+user prompt 摘要 MD5（去重与回归用） |
| response_text | TEXT | | 模型输出文本 |
| latency_ms | BIGINT | NOT NULL DEFAULT 0 | 调用耗时（毫秒） |
| success | TINYINT(1) | NOT NULL DEFAULT 1 | 是否成功 1/0 |
| error_msg | VARCHAR(512) | | 失败原因 |
| judge_score | TINYINT | NULL | LLM-as-Judge 质量评分 1-5（NULL=待评估） |
| judge_comment | VARCHAR(255) | | 评审评语 |
| created_at | DATETIME | NOT NULL DEFAULT CURRENT_TIMESTAMP | 创建时间 |

**索引**：idx_ai_call_log_scope (scope, created_at)、idx_ai_call_log_judge (judge_score, created_at)
