# EasyOrange 数据库迁移文件结构

## 目录结构

```
src/main/resources/db/
├── migration/                    # Flyway 版本化迁移文件
│   ├── V1__init_schema.sql      # 基线：所有核心表结构（含支付、事件、Saga等）
│   └── rollback/                # 回滚脚本目录
│       └── V1__init_schema_down.sql
├── seed/                         # 种子数据（手动执行或脚本加载）
│   ├── 01_categories.sql        # 分类数据
│   ├── 02_payment_config.sql    # 支付配置
│   └── 03_message_templates.sql # 消息模板
├── test/                         # 测试数据（模块化拆分）
│   ├── 00_cleanup.sql           # 数据清理脚本
│   ├── 01_users.sql             # 用户测试数据
│   ├── 02_categories.sql        # 分类测试数据
│   ├── 03_products.sql          # 商品测试数据
│   ├── 04_orders_payments.sql   # 订单支付测试数据
│   ├── 05_messages.sql          # 消息测试数据
│   └── 06_search_others.sql     # 搜索及其他测试数据
└── dev/                          # 开发环境测试数据
    └── R__insert_dev_test_data.sql  # Repeatable 迁移
```

## 表命名规范

所有表统一使用 `eo_` 前缀：

| 模块 | 表名 |
|------|------|
| 用户 | `eo_user` |
| 商品 | `eo_category`, `eo_product`, `eo_product_detail`, `eo_product_image`, `eo_product_report`, `eo_favorite` |
| 搜索 | `eo_search_history`, `eo_hot_keyword` |
| 订单 | `eo_order` |
| 支付 | `eo_payment`, `eo_payment_config` |
| 消息 | `eo_message`, `eo_message_archive`, `eo_message_subscription`, `eo_message_template`, `eo_offline_message` |
| 文件 | `eo_upload_file` |
| 日志 | `eo_oper_log`, `eo_oper_log_archive` |
| 事件 | `eo_domain_event` |
| 幂等 | `eo_idempotency_key` |
| Saga | `eo_saga_status` |

## 迁移文件命名规范

### 版本化迁移 (Versioned Migrations)
- 格式：`V{version}__{description}.sql`
- 示例：`V1__init_schema.sql`, `V2__add_user_table.sql`
- 版本号必须是唯一的，按顺序递增

### 可重复迁移 (Repeatable Migrations)
- 格式：`R__{description}.sql`
- 示例：`R__insert_dev_test_data.sql`
- 当文件内容发生变化时重新执行

## 回滚脚本使用

回滚脚本位于 `db/migration/rollback/` 目录，命名格式为 `V{version}__{description}_down.sql`。

### 手动回滚步骤

```bash
# 1. 连接数据库
mysql -u easyorange_app -p easyorange

# 2. 执行回滚脚本
source /path/to/V1__init_schema_down.sql

# 3. 清理 Flyway 历史记录
DELETE FROM flyway_schema_history WHERE version = '1';
```

## 种子数据管理

### 种子数据 vs 测试数据

| 类型 | 位置 | 用途 | 执行方式 |
|------|------|------|----------|
| 种子数据 | `db/seed/` | 系统启动必需的基础数据 | 手动执行或初始化脚本 |
| 测试数据 | `db/test/` | 集成测试用数据 | TestDataLoader 或 Testcontainers |
| 开发数据 | `db/dev/` | 开发环境测试用数据 | Flyway Repeatable 迁移 |

### 加载种子数据

```bash
# 手动加载所有种子数据
for file in src/main/resources/db/seed/*.sql; do
    mysql -u easyorange_app -p easyorange < "$file"
done
```

## 测试数据管理

### 测试数据结构

测试数据按模块拆分，便于独立加载和清理：

| 文件 | 模块 | 说明 |
|------|------|------|
| 00_cleanup.sql | 清理 | 清理所有测试数据 |
| 01_users.sql | 用户 | 18个测试用户 |
| 02_categories.sql | 分类 | 子分类数据 |
| 03_products.sql | 商品 | 45个商品及图片详情 |
| 04_orders_payments.sql | 订单支付 | 12个订单和10个支付记录 |
| 05_messages.sql | 消息 | 消息、模板、订阅数据 |
| 06_search_others.sql | 搜索 | 搜索历史、热门关键词等 |

### 使用 TestDataLoader

```java
@Autowired
private TestDataLoader testDataLoader;

// 加载所有测试数据
testDataLoader.loadAllTestData();

// 加载指定模块
testDataLoader.loadTestData("01_users.sql", "03_products.sql");

// 清理测试数据
testDataLoader.cleanupTestData();
```

### 使用 IntegrationTestBase

```java
@SpringBootTest
@ActiveProfiles("integration-test")
class MyIntegrationTest extends IntegrationTestBase {
    
    @Test
    void testSomething() {
        // 测试数据已自动加载
        // 测试完成后自动清理
    }
}
```

### Testcontainers 配置

集成测试使用 Testcontainers 启动独立的 MySQL 容器：

```yaml
# application-integration-test.yaml
spring:
  datasource:
    url: jdbc:tc:mysql:8.0:///easyorange_test
    driver-class-name: org.testcontainers.jdbc.ContainerDatabaseDriver
```

## 新增迁移文件指南

### 1. 创建表结构迁移

```sql
-- V2__add_new_table.sql
CREATE TABLE `eo_new_table` (
    `id` BIGINT NOT NULL COMMENT '主键',
    `name` VARCHAR(100) NOT NULL COMMENT '名称',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='新表';
```

### 2. 创建对应回滚脚本

```sql
-- rollback/V2__add_new_table_down.sql
DROP TABLE IF EXISTS `eo_new_table`;
```

### 3. 添加种子数据（如需要）

```sql
-- seed/04_new_table_data.sql
INSERT INTO `eo_new_table` (`id`, `name`) VALUES (1, '默认数据');
```

## 最佳实践

1. **结构迁移与种子数据分离**
   - 结构变更放在 `migration/` 目录
   - 基础数据放在 `seed/` 目录
   - 测试数据放在 `dev/` 或 `test/` 目录

2. **必须创建回滚脚本**
   - 每个版本化迁移都应有对应的回滚脚本
   - 回滚脚本应能完全撤销迁移的影响

3. **使用幂等性语句**
   - 使用 `IF NOT EXISTS`, `ON DUPLICATE KEY UPDATE` 等
   - 避免重复执行导致错误

4. **表名统一使用 eo_ 前缀**
   - 所有表统一 `eo_` 前缀

5. **添加 CHECK 约束**
   - 状态字段使用 CHECK 约束保证数据一致性
   - 金额字段使用 CHECK 约束防止负值

## 常见问题

### Q: 如何查看当前迁移状态？

```sql
SELECT * FROM flyway_schema_history ORDER BY installed_rank;
```

### Q: 如何修复迁移失败？

1. 手动修复数据库问题
2. 删除失败的迁移记录：`DELETE FROM flyway_schema_history WHERE success = 0;`
3. 修复迁移文件后重新启动应用

### Q: 生产环境如何执行迁移？

1. 备份数据库
2. 在测试环境验证迁移
3. 使用 `flywayMigrate` 任务执行迁移
4. 验证迁移结果

## 相关文档

- [Flyway 官方文档](https://flywaydb.org/documentation/)
- [Spring Boot Flyway 集成](https://docs.spring.io/spring-boot/docs/current/reference/html/howto.html#howto.data-initialization.migration-tool)
