# EasyOrange — Java AI Agent 工程化实战

> **定位**：Java AI Agent 工程化实战——按生产级标准做工程化（项目不对外运营，工程实践对标生产）：多范式工具编排（Workflow 式 4 路并行扇出 + 自治式多步工具循环）+ RAG 检索增强 + 评估闭环进 CI + 成本/延迟治理 + MCP 工具面 + LLM 专用可观测（Langfuse），AI 链路**可换供应商、可降级、可观测、可评估**。两条业务主线：卖家「发布助手」/ 买家「对话式找货」；DDD 六边形与分布式可靠性（ADR/ArchUnit/PIT 三板斧）是工程底座叙事。
> **业务**：C2C 资产流转（固定价格 + 直发 + 平台不碰货），复杂度留给 AI 工程化与架构。
> **口径**：可量化数字与定位口径以 [doc/工程指标.md](doc/工程指标.md) 为唯一来源；禁止「业务不是重点」等自我削弱叙事。

## 功能取舍判据（最高优先级，覆盖其它默认倾向）

功能增 / 删 / 改一律以 **「演示 + 面试 ROI」为唯一判据**——不以业务价值、实现成本、代码整洁度，也不以「保持现状 / 最小改动 / 向后兼容」为准：

> 对话中说「项目规则 / 按项目规则处理」即指本节（面试 ROI + 性价比），不指提交流程、构建等细则——细则在判据通过后照常执行。

- **准入**：能否让面试官 10 秒看懂价值、并支撑 15 分钟深挖？两条都不满足的功能，优先**删**而非优先改
- **落点**：优先落在**业务主链路**上；旁挂增强（删掉业务照跑）不作核心叙事
- **方向**：向当下技术浪潮靠（RAG 检索增强 / Agent 编排 / 多模态 / 评估闭环 / 成本与延迟治理）；优先把已有链路**换语料、换落点**复用，不新增旁路
- **双项目分工**：本仓库 = 生产级**单 Agent** 工程化（多步工具循环 + MCP 工具面 + RAG + 评估闭环）；**多智能体编排不在本仓库做**，归第二个项目（Python LangGraph 多智能体编排 + 评估为主体，经 MCP 消费本仓库 Java 工具面，2026-12 启动）——互补轴是编排范式（单 Agent 生产化 vs 多智能体编排）不是语言，新增 AI 功能前先对照本条
- **可量化**：主链路功能进叙事时至少要有**一个可引用数字**——口径 / 样本量 / 复现命令能当场交代（采纳率、偏离度、命中率、降级率、成本 token 每次、延迟分位、不超卖这类不变式）；LLM-as-Judge 分数不作对外质量证据（回归门禁可用，对外声称不可用）。缺数字按**测量债**处理：标 `待补口径` 限期补测，补不出才移出叙事；**其中要靠真实 API key / 额度才能取的（跑量指标、金标准回归、演示录屏）标记后即搁置**——不阻塞其它工作，也不反复当阻塞项回报，额度到位再补测
- **减法优先**：删除的功能要能说清「为什么砍」——面试里能解释减法比能罗列加法稀缺
- **允许大改**：为达成上述目标可重构 / 删除 / 新增任意模块与既有功能，不受「最小改动」倾向束缚；但改动仍须可验证、可回滚，且同一逻辑单元仍在一次提交内闭合

## 项目结构

monorepo：`easyorange-backend/`（Maven 多模块，约定见 [AGENTS.md](easyorange-backend/AGENTS.md)）· `easyorange-frontend/`（约定见 [AGENTS.md](easyorange-frontend/AGENTS.md)）· `doc/`（技术栈 / ADR / agents 参考 / DATABASE / 面试）· `infra/`（IaC）· `k8s/`（kustomize，无状态应用层）· `load-tests/`（k6 压测）

## 技术栈

| 层 | 技术 |
|---|------|
| **后端** | Java, Spring Boot, MyBatis-Plus |
| **前端** | TypeScript, React |
| **数据库** | MySQL, Redis |
| **消息队列** | RabbitMQ (Spring AMQP) |
| **搜索引擎** | Elasticsearch (IK 中文分词器) |
| **认证** | JWT Access (RSA) + Opaque Refresh (Redis, HttpOnly Cookie) |
| **迁移** | Flyway |
| **部署** | Docker / compose.yaml（凭据统一经根 `.env` 插值）+ K8s kustomize |

> **文档一律不复刻版本号**：精确版本以 `easyorange-backend/pom.xml` / `easyorange-frontend/package.json` 与 `compose.yaml` 为单一来源；技术栈选型与说明见 [README](README.md#技术栈)（README 顶部徽章的版本只作品牌展示，不在同步范围内）。
> **Elasticsearch 例外**：版本是硬锁——Spring Data ES 客户端与 IK 分词器都按它编译，见 `infra/elasticsearch/Dockerfile` 注释；升级须整体等 Boot 带动客户端，**不要在 infra 侧单独升**。

## 全局硬约束（任何改动都适用，违反即返工）

- **API 统一返回 `Result<T>`**；分页 `PageResult<T>`；搜索 `SearchPageResponse<T>`（`records/total/current/size/pages` + `facets` + `aiEnhancement`）。成功判据：`"A0000".equals(code)`
- **数据库变更必须通过 Flyway 迁移脚本**（CREATE TABLE 用紧凑格式，**禁止对齐列**）；DO 枚举字段经 `@EnumValue` 注解持久化（内置 `MybatisEnumTypeHandler`，禁止手写 TypeHandler）
- **DDD 分层**：domain → application → adapter，依赖方向单向向内；聚合根不可变（`@Builder(toBuilder = true)`），值对象用 `record`
- **CQRS + ACL 隔离**：命令与查询分离（product/order/payment/message）；跨模块必须通过 Port/ACL 适配，禁止直接依赖领域模型/Mapper
- **Assembler 模式**：DTO 转换统一在 `adapter/inbound/web/assembler/`，禁止在 Controller/Service 直接构造 Response DTO
- **异常**：领域异常必须继承 `BaseBusinessException`（否则落 500 兜底）；抛异常用 `BusinessException.of(...)` / `FileException.of(...)`；用模块专属 `ResultCode`（如 `ProductResultCode`），禁止回退全局 `B0002`；**每个业务模块只保留一个统一领域异常**，具体语义走类上的具名工厂（`notFound(id)` / `notOwner(id)`…）、构造器非公开，不新增「一码一类」的叶子异常（判据见 [架构参考](doc/agents/架构参考.md)「异常与校验细则」，门禁见 `ArchitectureRulesTest` Rule 11）
- **ID 统一 UUID v7 String**（36 位，`IdGenerator` / `UuidV7IdGenerator`）；前端实体 ID 保持 string
- **多模块构建**：修改子模块后启动前必须 `./mvnw install -DskipTests`（或 `clean package -pl <module> -am`），否则 ClassNotFoundException
- **删过资源文件就必须 `clean`**：`install` 只增量复制 `src/main/resources`，**不会删除 `target/classes` 里已移除的文件**。删迁移脚本/配置/模板后若只跑 `install`，老副本仍留在 classpath 上被读取（2026-09-17 实测：删掉 `R__seed_payment_config.sql` 后 Flyway 读到陈旧副本、对新 schema 执行而启动失败）。判据：`diff <(ls src/main/resources/**) <(ls target/classes/**)` 有差集就 `clean`
- **开发中增量验证**：改动只跑涉及模块的单测/集成测试，不核查 JaCoCo/PIT 覆盖率、不刷新 `doc/工程指标.md`（整体收口时统一跑一次）
- **STP 标准 API 优先**：优先框架/标准库内置功能，零新增自定义代码是最优方案（例：JWT 走 `oauth2ResourceServer()`，不手写 Filter/工具类）
- **MCP 工具面信任边界**：MCP server 只暴露**公开只读**工具（商品检索 / 详情 / 类目 / 平台规则知识），禁止暴露用户态数据（订单、个人信息、写路径）——外部 MCP client 无用户上下文；agent 内部工具与 MCP 对外工具面是两级暴露，鉴权假设各自独立

## 提交规范（Git 工作流）

- **小步提交**：一个逻辑单元一个提交（大特性 2~3 个封顶），验证通过即提交；禁攒「收口」大提交、禁 `git add -A`；纯格式调整揉进所属逻辑提交（`style` 仅用于修 spotless/CI 格式失败）
- **消息**：`<type>[(<scope>)]: <一句中文描述>`，一行说清改了什么/为什么，禁 `+` 拼接多主题；type 集合与「仅文档提交用 `docs`」等机械规则由 `.githooks/commit-msg` 校验，照报错改
- **分支与历史**：单人开发**直接在 `develop` 上提交**（不开 feature 分支）；不重写已推送历史，tag 只在真实发布时打
- **钩子**：`git config core.hooksPath .githooks` 启用（紧急 `SKIP=git-hooks`）

## 参考索引（按需读取，不常驻上下文）

| 主题 | 位置 |
|------|------|
| 商品 / 订单状态机、领域术语与 ADR 消费约定 | [doc/agents/领域参考.md](doc/agents/领域参考.md) |
| 模块职责 / 依赖边 / 错误码 / 异常判据 / 可观测 / 已知问题 | [doc/agents/架构参考.md](doc/agents/架构参考.md) |
| 后端编码约定（命名 / DTO / 缓存 / 安全 / 事件 / 各模块要点含 AI 全链路） | [easyorange-backend/AGENTS.md](easyorange-backend/AGENTS.md) |
| 构建 / 测试 / 启动 / 部署命令、gh CLI、CI/CD | [doc/agents/常用命令.md](doc/agents/常用命令.md) + [k8s/README.md](k8s/README.md) |
| 技术栈选型与说明 | [README](README.md#技术栈) |
| 数据库约定 / 表清单 / Flyway 迁移规范 / 脚本索引 | [doc/DATABASE.md](doc/DATABASE.md) |
| 测试数 / 覆盖率 / 压测数字 / [结构计数](doc/工程指标.md#结构计数)（模块 · Port · ADR · 消费者 · 表 · 规则 · 模板 · 金标准集，**数字单一来源**）；已知技术债 | [doc/工程指标.md](doc/工程指标.md) + [doc/技术债务清单.md](doc/技术债务清单.md) |
| 迭代路线（Agent 升级 sprint / 双项目排期 / 收口纪律） | [doc/迭代路线.md](doc/迭代路线.md) |
| 面试脚本（简历 / 自我介绍 / 追问应答 / 八股 / 代码走读 / 设计题） | [doc/interview/](doc/interview/) |
| ADR 决策记录（拒绝 Saga / UUID v7 主键 / RAG 改 RRF 等，篇数见[结构计数](doc/工程指标.md#结构计数)） | `doc/adr/` |
| Issues / PRD（含 triage label：`needs-triage` / `needs-info` / `ready-for-agent` / `ready-for-human` / `wontfix`） | GitHub issues，用 `gh` CLI（模板见常用命令.md） |
| 后端 REST 端点 | 起 dev 服务后看 Swagger UI（`/swagger-ui.html`，springdoc 从 Controller 注解生成，**无手工清单**） |

> **结构计数只在 [doc/工程指标.md](doc/工程指标.md#结构计数) 单点维护**：这类计数随代码变动，其余文档一律链接该区块或写定性表述（「Port 接口编译期隔离」），不再复制数字；改代码后跑 `python3 .githooks/check-metrics-drift.py --fix` 校准（提交时 pre-commit 校验）。
