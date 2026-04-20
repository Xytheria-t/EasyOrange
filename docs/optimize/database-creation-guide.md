# EasyOrange 数据库创建与配置指南

本文档面向当前 EasyOrange 小型单体项目，目标是在不过度工程化的前提下，保留企业项目最重要的数据库规范：环境隔离、最小权限、可追踪迁移、密码不写死、建表脚本与代码一致。

## 结论

当前项目推荐使用：

- 数据库：MySQL 8.0
- 字符集：`utf8mb4`
- 排序规则：MySQL 8 优先 `utf8mb4_0900_ai_ci`
- 存储引擎：InnoDB
- 时区：`Asia/Shanghai`
- 结构管理：Flyway SQL migration
- 本地依赖：Docker Compose 启动 MySQL + Redis
- 账号策略：应用使用专用账号，不使用 `root`

不建议继续把 `easyorange-backend/init.sql` 作为主建库入口。它适合保留为历史参考，真正的建表来源应统一为：

```text
easyorange-backend/easyorange-application/src/main/resources/db/migration/
```

## 当前落地状态

当前项目已经按本文档的推荐方式整理为：

- `application.yaml` 使用标准 `spring.flyway` 配置。
- Spring Boot 4 需要引入 `spring-boot-flyway` 自动配置模块，否则只有 Flyway 核心库不会自动迁移。
- `V1__init_schema.sql` 作为结构基线，补齐当前实体和 Mapper 会访问的表、字段与索引。
- `V2__seed_categories.sql` 只写入基础分类字典，可用于所有环境。
- 开发测试数据放在 `db/dev/R__insert_dev_test_data.sql`，仅 dev profile 加载。
- `ProductMapper.xml` 使用的 `MATCH(name)` 已由 `product.name` 全文索引支撑。
- `sys_user.avatar`、`hot_keyword.last_search_time`、`upload_file`、`sys_oper_log`、`sys_oper_log_archive` 等当前代码会访问的结构已纳入基线。

注意：多数实体继承 `BaseDO`，主键策略是 MyBatis-Plus `ASSIGN_ID`；手写测试数据时仍应显式给 `id`，本文档中的 dev 测试数据也遵循这个规则。

## 推荐环境变量

本地开发可以使用 `.env` 或 PowerShell 临时环境变量。生产环境应由部署平台注入，不要提交真实密码。

```env
EASYORANGE_DB_HOST=localhost
EASYORANGE_DB_PORT=3306
EASYORANGE_DB_NAME=easyorange
EASYORANGE_DB_USERNAME=easyorange_app
EASYORANGE_DB_PASSWORD=easyorange_app_dev
MYSQL_ROOT_PASSWORD=root123456

REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=
```

应用 JDBC URL 建议支持 `EASYORANGE_DB_NAME`，不要把库名写死：

```yaml
spring:
  datasource:
    url: jdbc:mysql://${EASYORANGE_DB_HOST:localhost}:${EASYORANGE_DB_PORT:3306}/${EASYORANGE_DB_NAME:easyorange}?useUnicode=true&characterEncoding=UTF-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&connectTimeout=5000&socketTimeout=30000
    username: ${EASYORANGE_DB_USERNAME:easyorange_app}
    password: ${EASYORANGE_DB_PASSWORD:easyorange_app_dev}
    driver-class-name: com.mysql.cj.jdbc.Driver
```

## 创建数据库和账号

用管理员账号登录 MySQL 后执行：

```sql
CREATE DATABASE IF NOT EXISTS `easyorange`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

CREATE USER IF NOT EXISTS 'easyorange_app'@'%' IDENTIFIED BY 'change_this_password';

GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES
ON `easyorange`.*
TO 'easyorange_app'@'%';

FLUSH PRIVILEGES;
```

说明：

- 本项目当前推荐让应用启动时自动执行 Flyway，所以应用账号需要基本 DDL 权限。
- 更严格的生产环境可以拆成两个账号：`easyorange_migrator` 负责 Flyway DDL，`easyorange_app` 只保留 DML 权限。
- 无论本地还是生产，都不要用 `root` 作为应用连接账号。

生产环境双账号示例：

```sql
CREATE USER IF NOT EXISTS 'easyorange_migrator'@'%' IDENTIFIED BY 'change_this_migrator_password';
CREATE USER IF NOT EXISTS 'easyorange_app'@'%' IDENTIFIED BY 'change_this_app_password';

GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES
ON `easyorange`.*
TO 'easyorange_migrator'@'%';

GRANT SELECT, INSERT, UPDATE, DELETE
ON `easyorange`.*
TO 'easyorange_app'@'%';

FLUSH PRIVILEGES;
```

对应 Spring 配置：

```yaml
spring:
  datasource:
    username: ${EASYORANGE_DB_USERNAME:easyorange_app}
    password: ${EASYORANGE_DB_PASSWORD:easyorange_app_dev}
  flyway:
    user: ${EASYORANGE_DB_MIGRATION_USERNAME:${EASYORANGE_DB_USERNAME:easyorange_app}}
    password: ${EASYORANGE_DB_MIGRATION_PASSWORD:${EASYORANGE_DB_PASSWORD:easyorange_app_dev}}
```

## Flyway 配置

当前项目的 Flyway 配置应保持在 `spring.flyway` 下：

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: false
    validate-on-migrate: true
    clean-disabled: true
    table: flyway_schema_history
```

说明：

- 新库从零创建时，`baseline-on-migrate` 应为 `false`。
- 只有接管已有手工建表数据库时，才临时使用 `baseline-on-migrate: true`。
- 已经执行过的 `V*.sql` 不要修改；需要变更时新增更高版本迁移，例如 `V3__add_user_avatar.sql`。
- `clean-disabled: true` 防止误删库。

开发环境测试数据建议拆分：

```text
src/main/resources/db/migration/       # 所有环境都会执行：表结构、必要基础字典
src/main/resources/db/dev/             # 仅开发环境执行：测试用户、测试商品
```

`application-dev.yaml` 中单独追加 dev 数据目录：

```yaml
spring:
  flyway:
    locations: classpath:db/migration,classpath:db/dev
```

## 本地 Docker Compose 建议

当前 `docker-compose.yml` 可以继续用于本地开发，但建议逐步改成环境变量驱动，避免账号密码散落在文件里：

```yaml
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD:-root123456}
      MYSQL_DATABASE: ${EASYORANGE_DB_NAME:-easyorange}
      MYSQL_USER: ${EASYORANGE_DB_USERNAME:-easyorange_app}
      MYSQL_PASSWORD: ${EASYORANGE_DB_PASSWORD:-easyorange_app_dev}
      TZ: Asia/Shanghai
```

本地启动：

```powershell
docker compose up -d mysql redis
docker compose ps
```

如果要完全重建本地开发库，确认不需要保留数据后再执行：

```powershell
docker compose down -v
docker compose up -d mysql redis
```

## 后端启动流程

1. 启动 MySQL 和 Redis。
2. 设置数据库环境变量。
3. 启动 Spring Boot 应用。
4. Flyway 自动创建 `flyway_schema_history` 并按版本执行建表脚本。

PowerShell 示例：

```powershell
$env:EASYORANGE_DB_HOST='localhost'
$env:EASYORANGE_DB_PORT='3306'
$env:EASYORANGE_DB_NAME='easyorange'
$env:EASYORANGE_DB_USERNAME='easyorange_app'
$env:EASYORANGE_DB_PASSWORD='easyorange_app_dev'

cd easyorange-backend
mvn -pl easyorange-application -am spring-boot:run
```

验证迁移：

```sql
SELECT installed_rank, version, description, success, installed_on
FROM flyway_schema_history
ORDER BY installed_rank;

SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'easyorange'
ORDER BY table_name;
```

## 当前项目最小完整表清单

与现有实体、Mapper 和业务模块对齐时，基线迁移至少应包含：

| 模块 | 表 |
| --- | --- |
| 用户 | `sys_user` |
| 商品 | `category`, `product`, `product_detail`, `product_image`, `product_report`, `search_history`, `hot_keyword` |
| 订单 | `eo_order` |
| 支付 | `eo_payment`, `eo_payment_config` |
| 消息 | `eo_message`, `eo_message_archive`, `eo_message_subscription`, `eo_message_template`, `eo_offline_message` |
| 文件 | `upload_file` |
| 日志 | `sys_oper_log`, `sys_oper_log_archive` |
| 迁移 | `flyway_schema_history` |

## 表结构设计取舍

适合当前项目的精简规范：

- 保留逻辑删除字段：`del_flag`，值与 MyBatis-Plus 配置一致，`0` 正常，`2` 删除。
- 保留审计字段：`create_time`, `update_time`, `create_by`, `update_by`。
- 保留乐观锁字段：`version`。
- 商品详情继续垂直拆分到 `product_detail`，避免列表查询加载大字段。
- 高频查询字段必须建索引，例如商品状态/分类/发布时间，订单买家/卖家/状态，消息接收人/已读状态。
- 当前小项目可以暂不强制数据库外键，优先用索引和服务层校验降低迁移复杂度；如果后续接近真实生产交易，应给订单、支付、商品等核心链路补齐外键或一致性校验。
- 支付私钥、公钥不建议长期明文存数据库；当前 MVP 可以保留字段，但生产应接入密钥管理或至少加密存储。

## 推荐执行顺序

1. 新建环境直接按本文档启动 Docker Compose，再启动后端，由 Flyway 自动建表。
2. 本地已有旧数据卷时，建议备份后执行 `docker compose down -v` 重建，让 Flyway 从 `V1` 开始接管全量结构。
3. 生产环境使用独立 MySQL 实例和专用账号，不使用 `root` 连接应用。
4. 后续表结构变更只新增更高版本迁移，不修改已经在共享环境执行过的 `V*.sql`。
