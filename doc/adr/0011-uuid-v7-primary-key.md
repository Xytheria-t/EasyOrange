# ADR 0011 — 全库主键统一 UUID v7，订单号由主键派生

- **状态**：接受
- **日期**：2026-09-16
- **决策者**：后端架构
- **标签**：`idgen` `uuid-v7` `persistence` `schema` `ddd`

---

## 上下文（Context）

主键形态是全库决策：它决定 ID 何时可得、由谁生成、占多少索引字节，并被所有模块与前端契约继承。

现状事实：

1. **全库单形态**：`V1__init_schema.sql` 共 32 张建表语句，**0 处 `AUTO_INCREMENT`**；主键列统一 `VARCHAR(36)`，其中 28 张以 `id` 命名，`eo_user.user_id` / `eo_product.product_id` / 2 张 Spring Modulith 表为业务列主键，类型同为 `VARCHAR(36)`。
2. **数据库不回填**：`BaseDO.id` 为 `@TableId(type = IdType.INPUT)`，MyBatis-Plus 不代为生成；`UserDO`/`ProductDetailDO` 沿用同一策略。
3. **前端契约**：全库 ID 以 36 位字符串（含连字符）对外，前端实体 ID 保持 string，跨模块 ACL 端口也以 String 传递。

约束：

- **约束 1 — DDD 聚合 ID 必须先于持久化存在**：`Order.createOrder(OrderCreateSpec)` 的首参就是 `OrderId`；订单号、`OrderCreatedEvent`、库存流水 `bizId`、支付单 `orderId` 全部要在同一本地事务内基于这个 ID 生成（[OrderCommandHandler.createOrderFlow](../../easyorange-backend/easyorange-order/src/main/java/com/cartethyia/easyorange/order/application/command/OrderCommandHandler.java)）。数据库自增 ID 在 INSERT 成功前不可得，与之直接冲突。
- **约束 2 — 多实例零协调**：部署形态为无状态应用层多副本（`k8s/`），无中央发号服务，数据库为单库 MySQL；发号不应引入跨节点协调。
- **约束 3 — ID 出现在 URL 与前端契约**：自增 ID 可枚举，会泄漏业务总量并放大越权遍历面。
- **约束 4 — 运维复杂度按人力计费**：单人项目，发号器的 workerId 分配、时钟回拨、号段续租等治理成本需真金白银地维护。

## 决策（Decision）

**全库主键统一 UUID v7（RFC 9562）字符串，由应用层生成、数据库不回填；订单号是主键的派生值，不引入独立发号器。**

1. **算法**：`UuidV7` 静态工具 —— 48-bit Unix 毫秒时间戳 | 4-bit 版本(7) | 12-bit 随机 | 2-bit 变体 | 62-bit 随机；随机源用 `ThreadLocalRandom`（122 位随机后缀不需要加密安全强度，无锁无熵阻塞）。时间戳在前 ⇒ 时间有序。
2. **生成时机与注入**：实体 ID 经 `IdGenerator` Port 由应用层注入（`UuidV7IdGenerator` 为 `@Primary`）；领域事件 ID 在聚合根内直接静态生成（纯算法、无外部协调，无需 Port）。
3. **落库形态**：`VARCHAR(36)` + `IdType.INPUT`，全库同构，跨模块 ACL 以 String 作契约，前端无类型转换负担。
4. **订单号派生**：`OrderNo.of("ORD" + orderId.value())` —— 订单号 = `ORD` 前缀 + 订单 ID，共 39 位；唯一性从主键继承，另由 `uk_eo_order_order_no` 唯一索引兜底。库存流水的幂等键 `bizId` 用**订单 ID**（`VARCHAR(36)` 装不下 39 位订单号）。
5. **二级索引与主键同宽**：业务表索引普遍包含 36 字节 ID 列（如 `eo_order` 的 `(buyer_id, status, del_flag, create_time DESC)`）。

关键实现：

- 算法：[UuidV7.java](../../easyorange-backend/easyorange-common/src/main/java/com/cartethyia/easyorange/common/idgen/UuidV7.java)
- Port 与适配器：[IdGenerator.java](../../easyorange-backend/easyorange-common/src/main/java/com/cartethyia/easyorange/common/idgen/IdGenerator.java)、[UuidV7IdGenerator.java](../../easyorange-backend/easyorange-framework/src/main/java/com/cartethyia/easyorange/framework/idgen/UuidV7IdGenerator.java)
- 持久化策略：[BaseDO.java](../../easyorange-backend/easyorange-common/src/main/java/com/cartethyia/easyorange/common/entity/BaseDO.java)
- 订单号派生与 ID 使用：[Order.java](../../easyorange-backend/easyorange-order/src/main/java/com/cartethyia/easyorange/order/domain/aggregate/Order.java)、[OrderCommandHandler.java](../../easyorange-backend/easyorange-order/src/main/java/com/cartethyia/easyorange/order/application/command/OrderCommandHandler.java)
- 表结构：[V1__init_schema.sql](../../easyorange-backend/easyorange-application/src/main/resources/db/migration/V1__init_schema.sql)

核心驱动力与约束一一对应：ID 前置（约束 1）、零协调发号（约束 2）、不可枚举（约束 3）、实现成本最低（约束 4）；v7 相对 v4 额外买到时间有序。

## 后果（Consequences）

### 正向后果

- **聚合 ID 先于持久化存在**：订单号、领域事件、库存流水幂等键、支付单关联键在保存前即可确定，同一次 INSERT 事务内原子落地，不存在「先插后改」。
- **跨模块零回填**：payment / 库存流水 / 收藏直接携带订单 ID，没有「等数据库返回 ID」的时序耦合。
- **发号零协调**：无 workerId、无时钟回拨判据、无 Redis 发号依赖，`IdGenerator` 为纯内存实现。
- **时间有序**：主键近似顺序写入，避免 v4 随机插入导致的页分裂与缓冲池命中率下降。
- **不可枚举**：122 位随机后缀使 ID 不可猜，业务量不泄漏，URL 中 ID 不可遍历。
- **单形态低摩擦**：全库 ID 一律 36 位 String，跨模块端口、DTO、前端契约无「这列数字那列字符串」的转换负担。

### 负向后果

- **索引体积与写入放大**：`VARCHAR(36)` 实际占 36 字节 + 1 字节长度前缀（utf8mb4 下 ASCII 内容），是 `BIGINT`(8) 的约 4.6 倍、`BINARY(16)` 的约 2.3 倍；二级索引条目都携带它，多列索引（含 buyer_id/seller_id 等 ID 列）成倍放大。
- **字符串比较常数更大**：主键等值/范围扫描按 36 字节 + 排序规则比较，比整数比较略慢。
- **主键顺序不等于业务时序**：时间戳精度为毫秒，同毫秒内并发插入的顺序由随机后缀决定，主键排序不是严格创建顺序。
- **ID 不适合人工念读**：客服/对账无法口述 36 位 UUID，订单号（39 位）同样不适合人工念读，只能复制。

### 缓解措施

- 唯一性由主键约束裁决；订单号另有 `uk_eo_order_order_no` 唯一索引兜底，重复即报错而非静默覆盖。
- 业务时序以 `create_time`（`DATETIME DEFAULT CURRENT_TIMESTAMP`）为准，不把主键排序当时间线。
- 需人工沟通的场景统一用订单号（`ORD` 前缀便于识别）；库存流水等机器幂等键用订单 ID。
- 文档与注释统一口径「业务单号 = 订单 ID」，禁止把幂等键写成「订单号」（本次同步修正 order/product 模块注释与模块 AGENTS.md）。
- 索引体积按数据规模监控，触发条件见「备注」。

## 备选方案（Alternatives Considered）

- **`BIGINT AUTO_INCREMENT`**：拒绝。ID 在 INSERT 成功后才可得，与约束 1 正面冲突——订单号、`OrderCreatedEvent`、库存流水 `bizId` 都要在保存前确定，落地方案只能是「先插入拿 ID → 再 UPDATE 补订单号 → 延后构造事件」；此外自增 ID 可枚举（约束 3），并受单库序列约束（约束 2）。
- **Snowflake（64-bit 发号器）**：拒绝。workerId 分配（含 Redis 协调）与时钟回拨处理是纯运维成本（约束 4），本项目无跨集群发号需求；相关实现 `SnowflakeIdGenerator` / `WorkerIdProvider` / `RedisWorkerIdProvider` 已删除。数字 ID 还需额外解决对外展示与前端 String 契约的一致性。
- **UUID v4（全随机）**：拒绝。122 位全随机使 InnoDB 聚簇索引随机插入，带来页分裂与缓冲池命中率下降；v7 用同样的 128 位买到了时间有序。
- **ULID（Crockford Base32 同族方案）**：未采用。布局与 v7 同源（48-bit 时间戳 + 随机），但 JDK 无原生类型，需引三方库或自写编码/解析；UUID v7 是 RFC 9562 标准且可直接由 `java.util.UUID` 承载，本项目 ID 不追求 Base32 可读性（ID 不暴露给用户排序）。
- **UUID v7 + `BINARY(16)` 存储**：暂缓（算法与语义不变，只换存储形态）。可省约 55% 主键字节，但需全库所有表 + 全部 DO + MyBatis-Plus 读写路径 + ACL/DTO 序列化统一改造，收益在当前数据规模下不可测；触发条件见「备注」。

## 备注（Notes）

- 相关 ADR：[0006](0006-module-decoupling-port-adapter-acl.md)（跨模块 Port/ACL，ID 以 String 作契约）、[0007](0007-order-local-tx-over-saga.md)（本地单事务，ID 前置使订单号/事件/幂等键可在事务内派生）
- 相关文档：[doc/DATABASE.md](../DATABASE.md)（主键策略与表结构规范）、[easyorange-backend/AGENTS.md](../../easyorange-backend/AGENTS.md)「DTO / 类型 / 序列化」（`String` ID 与 `BaseDO` 约定）
- 相关代码：`UuidV7` / `IdGenerator` / `UuidV7IdGenerator` / `BaseDO` / `Order.createOrder` / `OrderCommandHandler.createOrderFlow`
- 后续演进触发条件：单表行数达千万级，或主键与二级索引占用成为可观测瓶颈（有表空间/缓冲池命中率数据支撑）时，评估 `BINARY(16)` 存储形态（`UUID_TO_BIN` / `BIN_TO_UUID`，转换收敛在出站适配层）；算法与 ID 语义不变，对外仍是 36 位字符串。
- 分库/分片兼容性：本决策无中央发号与自增序列依赖，未来拆库不构成阻碍（拆分判据见 [ADR-0010](0010-order-saga-evolution-plan.md)）。
