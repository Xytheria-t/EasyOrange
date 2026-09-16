# EasyOrange 数据库说明文档

> **字段级结构（列/类型/索引/CHECK）不在本文档复述**——单一事实来源是 `easyorange-backend/easyorange-application/src/main/resources/db/migration/` 下的 Flyway 脚本，看表结构直接读 SQL。
>
> 本文档只保留三类**从 SQL 读不出来**的内容：全局约定、表级索引（表清单 + 职责）、需要设计理由的表。

## 概述

| 项目 | 说明 |
|------|------|
| 数据库 | MySQL 8.4 (LTS) |
| 字符集 | utf8mb4 / utf8mb4_0900_ai_ci |
| 主键策略 | UUID v7（VARCHAR(36)），全库所有 ID 字段统一使用 UUID v7（RFC 9562，决策与取舍见 [ADR-0011](adr/0011-uuid-v7-primary-key.md)） |
| 逻辑删除 | del_flag TINYINT（0 正常 / 1 删除） |
| 乐观锁 | version INT DEFAULT 0 |
| 时间精度 | 业务表 DATETIME，基础设施表 DATETIME(3) |
| 外键 | 无物理外键，通过应用层保证一致性 |
| 全文索引 | MySQL 侧不建全文索引；商品检索由 Elasticsearch + IK 分词器承担（`infra/elasticsearch` 镜像内置 analysis-ik 9.2.8，经 `docker compose --profile search` 启用） |

## 迁移脚本

| 脚本 | 内容 |
|------|------|
| `V1__init_schema.sql` | 32 张表初始化（开发阶段 V1~V9 收口为单文件） |
| `V2__favorite_price_snapshot.sql` | 收藏价格快照（收藏降价功能首个增量） |
| `V3__task_scan_and_message_cleanup_indexes.sql` | 消息清理 / 订单定时扫描索引 |
| `V4__stock_ledger.sql` | 库存流水表 `eo_stock_ledger` 与存量资产基线 |
| `R__seed_*.sql` | 可重复执行种子：分类、支付渠道配置、RAG 知识库文档 |

迁移规范与演进策略见 [架构-数据库迁移.md](架构/架构-数据库迁移.md)。

## 表总览

共 33 张表：31 张 `eo_*` 业务/观测表（其中 3 张预留）+ 2 张 Spring Modulith 基础设施表（EVENT_PUBLICATION / EVENT_PUBLICATION_ARCHIVE）。

| 模块 | 表名 | 说明 | 实体类 |
|------|------|------|--------|
| 用户 | eo_user | 用户信息 | UserDO |
| 用户 | eo_user_credit | 用户信用评分 | UserCreditDO |
| 用户 | eo_credit_change_log | 信用分变更流水（预留） | — |
| 商品 | eo_category | 商品分类（两级树） | CategoryDO |
| 商品 | eo_product | 商品信息 | ProductDO |
| 商品 | eo_product_detail | 商品详情（1:1） | ProductDetailDO |
| 商品 | eo_product_image | 商品图片（1:N） | ProductImageDO |
| 商品 | eo_stock_ledger | 库存流水（幂等落账 + 对账基准，见文末） | StockLedgerDO |
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
| 观测 | eo_ai_call_log | AI 调用日志（LLM-as-Judge 数据源，见文末） | —（JDBC 直写） |
| 观测 | eo_ai_feedback | AI 输出用户反馈（反馈飞轮，导出后自动扩充金标准评测集） | — |
| 观测 | eo_knowledge_doc | RAG 知识库文档（解析→分块→embed→ES 索引，启动补索引） | — |
| 观测 | eo_user_preference | 用户长期画像（Agent 长期记忆，聊天气氛注入） | — |
| 观测 | eo_retrieval_metric | RAG 检索指标采样（hit@5 / MRR，金标准集回归数据源） | — |

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

**例外**：

- 基础设施表（EVENT_PUBLICATION / EVENT_PUBLICATION_ARCHIVE / eo_ai_call_log）使用 created_at / updated_at 时间字段，精度为毫秒 DATETIME(3)。
- 归档表（eo_message_archive）无 del_flag / version，使用 archived_at 记录归档时间。
- eo_audit_log 无 del_flag / version / create_by / update_by，使用独立主键 id 和时间字段 created_at。
- eo_stock_ledger 无 version（append-only，落账是插入，冲突由唯一索引裁决）。

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

## 需要单独说明的表

以下表的设计理由不在 SQL 字面里，单独记录。

### eo_stock_ledger — 库存流水表（V4 新增）

> **定位**：库存变更的单一事实来源。每次库存变更（初始化 / 下单扣减 / 取消退款恢复 / 人工调整）都在同一事务内落一条流水，`eo_product.stock` 退化为可由流水复现的余额快照。
>
> **幂等**：唯一索引 `uk_eo_stock_ledger_biz (change_type, biz_id, product_id)` 承载幂等——同一订单对同一资产的同类变更只允许落账一次，重复投递（MQ 重投 / DLQ 重放）插入 0 行即被跳过，库存不会被二次加减。MySQL 唯一索引不约束 NULL，因此 `biz_id` 留空的 INIT / ADJUST 天然不参与幂等约束（同一资产可多次人工调整）。
>
> **对账**：`StockReconcileScheduler`（product `adapter/outbound/scheduler/`）每日比对 `eo_product.stock` 与每个资产最近一条流水的 `stock_after`，漂移即 ERROR 告警 + `easyorange.stock.reconcile.drift` 指标，**只告警不改写余额**（自动修复会把缺陷从可观测退回静默）。
>
> **落账实现**：`StockLedgerRepositoryImpl` + `mapper/StockLedgerMapper.xml`；幂等插入用 `INSERT IGNORE`（不能用 `ON DUPLICATE KEY UPDATE`：Connector/J 默认 `useAffectedRows=false` 带 CLIENT_FOUND_ROWS，ODKU 命中已存在行时返回「匹配行数 1」，幂等判定会整体失效，详见 `StockLedgerIdempotencyIT`）。

关键列：`biz_id`（业务单号即订单 ID，INIT / ADJUST 留空）、`change_type`（INIT / DECREASE / RESTORE / ADJUST）、`delta`（正增负减）、`stock_after`（对账基准）。

### eo_ai_call_log — AI 调用日志表

> **现状**：`AiCallLogRecorder`（easyorange-ai/adapter/outbound/persistence/）在每次 LLM/Embedding 调用后 JDBC 直写一条（记录失败仅告警，不阻塞主链路）；`AiEvalScheduler`（adapter/inbound/job/）定时对 `judge_score IS NULL AND success = 1` 的记录用 ChatModel 打分（1-5 + 评语）。默认关闭（`easyorange.ai.eval.enabled=false`）。

关键列：`scope`（AI 调用场景）、`prompt_hash`（system+user prompt 摘要 MD5，去重与回归用）、`judge_score` / `judge_comment`（LLM-as-Judge 结果，NULL = 待评估）。

### 已删除的表（历史记录）

| 表 | 删除时间 | 替代方案 |
|----|---------|---------|
| `eo_domain_event` | 2026-07-14 | Spring Modulith `EVENT_PUBLICATION` 表承载 Outbox 模式 |
| `eo_idempotency_key` | 2026-08（迁移合并时） | framework 的 `IdempotencyKeyFilter` + Redis 承载幂等保护 |

## 维护约定

| 触发条件 | 操作 |
|---|---|
| 新增/修改表结构 | 只改 `db/migration/` 下的 Flyway 脚本（禁止直接改库）；表总览与本文档的「需要单独说明的表」按需同步 |
| 新增表 | 更新「表总览」计数与行 |
| 索引命名 / 数据类型约定变更 | 更新对应规范表格 |
