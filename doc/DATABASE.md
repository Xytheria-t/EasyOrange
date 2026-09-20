# EasyOrange 数据库说明文档

> **字段级结构（列/类型/索引/CHECK）不在本文档复述**——单一事实来源是 `easyorange-backend/easyorange-application/src/main/resources/db/migration/` 下的 Flyway 脚本，看表结构直接读 SQL。
>
> 本文档只保留三类**从 SQL 读不出来**的内容：全局约定、表级索引（表清单 + 职责）、需要设计理由的表。

## 概述

| 项目 | 说明 |
|------|------|
| 数据库 | MySQL (LTS) |
| 字符集 | utf8mb4 / utf8mb4_0900_ai_ci |
| 主键策略 | UUID v7（VARCHAR(36)），全库所有 ID 字段统一使用 UUID v7（RFC 9562，决策与取舍见 [ADR-0011](adr/0011-uuid-v7-primary-key.md)） |
| 逻辑删除 | del_flag TINYINT（0 正常 / 1 删除） |
| 乐观锁 | version INT DEFAULT 0 |
| 时间精度 | 业务表 DATETIME，基础设施表 DATETIME(3) |
| 外键 | 无物理外键，通过应用层保证一致性 |
| 全文索引 | MySQL 侧不建全文索引；商品检索由 Elasticsearch + IK 分词器承担（`infra/elasticsearch` 镜像内置 analysis-ik，版本硬锁见该 Dockerfile 注释，经 `docker compose --profile search` 启用） |

## 迁移脚本

| 脚本 | 内容 |
|------|------|
| `V1__init_schema.sql` | 26 表初始化（当前完整 DDL；开发阶段三次收口为单文件，项目未发版无生产历史） |
| `V2__agent_step_trace.sql` | Agent 步级轨迹表 `eo_agent_step_trace`（一次请求一个 trace_id） |
| `R__seed_*.sql` | 可重复执行种子：分类、RAG 知识库文档 |

## Flyway 迁移规范

| 原则 | 说明 |
|------|------|
| **DDL 与 DML 分离** | 结构变更（V 前缀）与参考数据（R 前缀）分开成独立迁移文件 |
| **迁移不可变** | 已部署的 V 版本禁止修改，只能新增 |
| **自包含** | 一个功能一个迁移文件，含完整结构（建表、索引、约束内联在 CREATE TABLE 里）；不按「索引层 / 约束层」拆分 |
| **紧凑格式** | CREATE TABLE 列定义**禁止对齐填充**（列名与类型间大量空格会让 Flyway MySQL 解析器报 1064） |

**`R__` 可重复迁移**：必须 `ON DUPLICATE KEY UPDATE` 保证幂等（用 MySQL 8.0.20+ 的 `AS new` 别名语法，弃用 `VALUES()` 函数）、包在 `START TRANSACTION` / `COMMIT` 中；频繁更新的 DML 优先用 `R__`，避免堆空版本号。

**`R__` 执行顺序 = 名称字典序**：Flyway 在所有 V 版本之后按**描述名排序**依次执行可重复迁移，与目录、加入时间无关。因此**依赖其它种子数据的 `R__` 必须靠命名排到后面**——例如 `R__seed_zz_ai_demo.sql`（建议快照要读 `eo_category` 的类目名）排在 `R__seed_categories.sql` 之后。放错位置在已初始化过的库上看不出问题（数据早就在），只在**全新库**上静默写坏数据（读不到依赖 → 写入 NULL），验证时必须用空库按 `V* → R*` 全序跑一遍。

**ALTER TABLE**：同表多个操作合并成一条语句，顺序 `DROP CHECK` → `MODIFY COLUMN` → `ADD CONSTRAINT` → `ADD COLUMN`；新增列用 `AFTER {column}` 定位。**CHECK 约束值必须与字段 COMMENT 一致**——调用方可能按 COMMENT 判断，两者冲突等于埋雷。

**Flyway 配置要点**：`clean-disabled: true`（生产禁止 clean）；`validate-on-migrate: true` 仅干净库 / CI 开启，dev / it profile 关闭——开发阶段改 V1 后 checksum 会挡启动，不必每次重置，想刷新 schema 时 `DROP DATABASE easyorange; CREATE DATABASE easyorange;` 重跑应用即从头执行 V1。

**常用命令**：`mvn flyway:info`（状态）/ `flyway:migrate` / `flyway:validate`（CI）/ `flyway:repair`（修 checksum，谨慎）/ `flyway:clean`（仅开发）。

**反模式**：修改已部署的 V 迁移（checksum 失败）· DDL/DML 混合 · 按「索引层」拆文件（每次新表都要改旧文件）· NOT NULL 无默认值（锁表重写全表，应 nullable → backfill → 再加约束）· 生产执行 clean · 对齐列格式（1064）· 1 起点的枚举码列给 `DEFAULT 0`（见上文历史事故）· 缺 `create_by` / `update_by` / `del_flag` / `version` 审计字段。

## 表总览

V1 的 24 个 `eo_*` 业务/观测表 + 2 个 Spring Modulith 基础设施表（EVENT_PUBLICATION / EVENT_PUBLICATION_ARCHIVE）+ V2 的 `eo_agent_step_trace`——**总数以[结构计数](./工程指标.md#结构计数)为准**。

> 早期建表时预留过 4 张从未被代码引用的表（eo_payment_config / eo_product_question / eo_audit_suggestion / eo_credit_change_log），已随 V1 收口删除——库里的表应当都有消费者。另有 eo_user_credit / eo_product_report / eo_report_handle_history 随信用、举报两个功能下线一并从 V1 移除。

| 模块 | 表名 | 说明 | 实体类 |
|------|------|------|--------|
| 用户 | eo_user | 用户信息 | UserDO |
| 商品 | eo_category | 商品分类（两级树） | CategoryDO |
| 商品 | eo_product | 商品信息 | ProductDO |
| 商品 | eo_product_detail | 商品详情（1:1） | ProductDetailDO |
| 商品 | eo_product_image | 商品图片（1:N） | ProductImageDO |
| 商品 | eo_stock_ledger | 库存流水（幂等落账 + 对账基准，见文末） | StockLedgerDO |
| 商品 | eo_product_audit_log | 商品审核记录 | — |
| 商品 | eo_product_review | 商品评价 | ProductReviewDO |
| 商品 | eo_favorite | 用户收藏 | FavoriteDO |
| 搜索 | eo_search_history | 搜索历史 | SearchHistoryDO |
| 搜索 | eo_hot_keyword | 热门关键词 | HotKeywordDO |
| 订单 | eo_order | 订单 | OrderDO |
| 订单 | eo_order_item | 订单行项 | OrderItemDO |
| 支付 | eo_payment | 支付记录 | PaymentDO |
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
| 观测 | eo_user_preference | 用户长期画像（Agent 长期记忆，聊天时注入 prompt） | — |
| 观测 | eo_retrieval_metric | RAG 检索指标采样（hit@5 / MRR，金标准集回归数据源） | — |
| 观测 | eo_agent_step_trace | Agent 步级轨迹（V2 表：工具 / 参数 / 理由 / 观察，一次请求一个 trace_id） | —（JDBC 直写） |

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

**枚举码列的默认值必须是该列的合法码**：布尔/0 起点标志用 `DEFAULT 0`；**1 起点的枚举码列不许给 `DEFAULT 0`**——要么不给默认值（`NOT NULL` 无默认，漏传即报错），要么给一个合法码，并用 `chk_eo_{table}_{column}` 把合法码钉死。历史事故：`eo_message.type DEFAULT 0` 不是合法 `MessageType` 码，未显式赋值即落成非法码，读侧枚举转换抛异常让整个消息列表 500（现行 DDL 已根治：无默认值 + `chk_eo_message_type` 钉死）。

## 表关系图

```
eo_user ──1:N── eo_product (user_id)
              ├──1:N── eo_product_image (product_id)
              ├──1:1── eo_product_detail (product_id)
              ├──1:N── eo_product_review (product_id)
              └──1:N── eo_favorite (user_id + product_id)

eo_category ──1:N── eo_product (category_id)
    └──自引用── eo_category (parent_id)

eo_user ──1:N── eo_order (buyer_id / seller_id)
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

### eo_stock_ledger — 库存流水表

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

关键列：`scope`（AI 调用场景）、`prompt_hash`（system+user prompt 摘要 MD5，去重与回归用）、`token_input` / `token_output`（供应商真实回报的用量，未回报记 0 不估算）、`subject_id`（调用主体，如商品 ID；部分调用发生在主体创建之前故可空）、`judge_score` / `judge_comment`（LLM-as-Judge 结果，NULL = 待评估）。

> **用量与主体的用途**：没有这两组列时，该表只能回答「哪个场景调用得多」，回答不了「哪个场景花得多」。补列后 `AiCostReportService` 可按场景出 token 报表（`GET /api/admin/ai/cost-report`），`subject_id` 供按主体做成本归因。注意 embedding 用量与未带 usage 的流式调用仍记 0。

### eo_product.ai_suggestion — AI 建议快照

> **来源**：拍照识别（发布助手）给出的六个字段（title / description / price / categoryName / conditionLevel / location）随创建请求一起落库，JSON 原文；**只写不改**，不参与定价逻辑与状态流转。

> **为什么落在商品侧**：拍照识别发生在商品创建之前，那时 `eo_ai_call_log.subject_id` 还没有值、商品也不存在，所以「AI 建议了什么」只能由商品自己记。落库后 `GET /api/admin/ai/listing-adoption` 才能算出字段级采纳率与价格偏离分布（口径与可引用性见 [工程指标](./工程指标.md)）。

> **为什么存原文而不是预先算好的采纳结论**：采纳判定（哪些算「一致」）随查询走，口径以后收紧时历史数据可直接重算，不必回填。

## 维护约定

| 触发条件 | 操作 |
|---|---|
| 新增/修改表结构 | 只改 `db/migration/` 下的 Flyway 脚本（禁止直接改库）；表总览与本文档的「需要单独说明的表」按需同步 |
| 新增表 | 更新「表总览」计数与行 |
| 索引命名 / 数据类型约定变更 | 更新对应规范表格 |
