# EasyOrange — LLM × DDD：Java 架构工程化实战

> **定位**：LLM × DDD 工程化实战 — 在 DDD 六边形架构里工程化集成 LLM，让 AI 链路可换供应商、可降级、可观测。两条主线：AI 应用工程化（两条 AI 主线链路——卖家「发布助手」/ 买家「对话式找货」——+ 轻量 Agent 编排 + 8 件套；原「6 决策点（4 LLM + 2 规则）」为演进痕迹，信用画像两处零 LLM 已移出 AI 叙事）+ 架构落地（DDD + 分布式可靠性 + ADR/ArchUnit/PIT 治理三板斧）。
> **业务**：C2C 资产流转（固定价格 + 直发 + 平台不碰货），复杂度留给架构与 AI 工程化。
> **口径**：可量化数字与定位口径以 [doc/工程指标.md](doc/工程指标.md) 为唯一来源；禁止「业务不是重点」等自我削弱叙事。

## 功能取舍判据（最高优先级，覆盖其它默认倾向）

**整个项目**（不只 AI）的功能增 / 删 / 改，一律以 **「演示 + 面试 ROI」为唯一判据**——不以业务价值、实现成本、代码整洁度，也不以「保持现状 / 最小改动 / 向后兼容」为准。据此明确授权：

- **准入**：能否让面试官 10 秒看懂价值、并支撑 15 分钟深挖？两条都不满足的功能，优先**删**而非优先改
- **落点**：优先落在**业务主链路**上；旁挂增强（删掉业务照跑）不作核心叙事
- **方向**：向当下技术浪潮靠（RAG 检索增强 / Agent 编排 / 多模态 / 评估闭环 / 成本与延迟治理）；优先把已有链路**换语料、换落点**复用，不新增旁路
- **可量化**：每个进叙事的功能至少要有**一个不依赖 LLM 评 LLM 的数字**（采纳率 / 偏离度 / 命中率 / 调用次数 / 降级率）；拿不出数字的功能移出叙事
- **减法优先**：删除的功能要能说清「为什么砍」——面试里能解释减法比能罗列加法稀缺
- **允许大改**：为达成上述目标可重构 / 删除 / 新增任意模块与既有功能，不受「最小改动」倾向束缚；但改动仍须可验证、可回滚，且同一逻辑单元仍在一次提交内闭合

## Agent skills

- **Issue tracker**：Issues 与 PRDs 存放在 GitHub issues，用 `gh` CLI 读写（命令模板见 [doc/agents/常用命令.md](doc/agents/常用命令.md)「GitHub Issues / PR」）；默认 triage label：`needs-triage` / `needs-info` / `ready-for-agent` / `ready-for-human` / `wontfix`
- **Domain docs**：single-context 布局，领域术语与 ADR 消费约定见 [doc/agents/领域参考.md](doc/agents/领域参考.md)「Agent 领域文档消费约定」
- **面试准备 / 项目学习**：[doc/interview/00-怎么说.md](doc/interview/00-怎么说.md)（简历 / 自我介绍 / 追问应答 / 盲区标注，含学习队列与自测协议）；[doc/interview/01-怎么答.md](doc/interview/01-怎么答.md)（八股 59 题 / 13 类代码走读 / 设计题 / 手撕题）

## 项目结构

monorepo：`easyorange-backend/`（Spring Boot 后端，11 Maven 模块，各模块规范见模块内 `AGENTS.md`）· `easyorange-frontend/`（React + Vite + TypeScript + TanStack Query）· `doc/`（架构 / 集成 / ADR / agents 参考 / DATABASE）· `infra/`（IaC：Prometheus / Grafana / ES IK 镜像）· `k8s/`（K8s kustomize，无状态应用层）· `load-tests/`（k6 压测）· `.claude/rules/ecc/`（AI 编码规则 ECC：common/java/typescript/react/web）

## 技术栈

| 层 | 技术 |
|---|------|
| **后端** | Java 25, Spring Boot 4, MyBatis-Plus |
| **前端** | TypeScript, React 19 |
| **数据库** | MySQL 8.4, Redis 8 |
| **消息队列** | RabbitMQ 4.3 (Spring AMQP 4.0.x) |
| **搜索引擎** | Elasticsearch 9.2.8 (IK 中文分词器) |
| **认证** | JWT Access (RSA) + Opaque Refresh (Redis, HttpOnly Cookie) |
| **迁移** | Flyway 13 |

> **版本口径**：本表只写大版本（大版本才承载技术取舍，如 Java 25 虚拟线程 / Boot 4 包路径变更）；
> **精确版本以 `easyorange-backend/pom.xml` 与 `compose.yaml` 为单一来源**，二者不一致时以后者为准。
> Elasticsearch 例外——9.2.8 是硬锁（Spring Data ES 6.0.6 按它编译 + IK 插件同版本，见 `infra/elasticsearch/Dockerfile` 注释），升级须整体等 Boot 带动客户端。
| **部署** | Docker, docker-compose, compose.yaml（显式 env 直连，凭据统一经根 `.env` 插值）+ **K8s/kustomize** (k8s/, 无状态应用层) |

## 全局硬约束（任何改动都适用，违反即返工）

- **API 统一返回 `Result<T>`**；分页返回 `PageResult<T>`；搜索返回 `SearchPageResponse<T>`（含 `records/total/current/size/pages` + `facets` + `aiEnhancement`）。判断成功：`"A0000".equals(code)`
- **数据库变更必须通过 Flyway 迁移脚本**（CREATE TABLE 用紧凑格式，禁止对齐列）；DO 枚举字段经 `@EnumValue` 注解持久化（内置 `MybatisEnumTypeHandler`，禁止手写 TypeHandler）
- **DDD 分层**：domain → application → adapter，依赖方向单向向内；聚合根不可变（`@Builder(toBuilder = true)`），值对象用 `record`
- **CQRS + ACL 隔离**：命令与查询分离（product/order/payment/message）；跨模块必须通过 Port/ACL 适配，禁止直接依赖领域模型/Mapper
- **Assembler 模式**：DTO 转换统一在 `adapter/inbound/web/assembler/`，禁止在 Controller/Service 直接构造 Response DTO
- **异常**：领域异常必须继承 `BaseBusinessException`，禁止直接抛非其子类的 RuntimeException（否则落 500 兜底）；抛异常用 `BusinessException.of(...)` / `FileException.of(...)`；用模块专属 `ResultCode`（如 `ProductResultCode`），禁止回退全局 `B0002`；**每个业务模块只保留一个统一领域异常**，具体语义走类上的具名工厂（`notFound(id)` / `notOwner(id)`…）、构造器非公开，不新增「一码一类」的叶子异常；确需调用方按类型 catch 的才独立成类，且必须继承该模块统一异常（判据见 [架构-DDD规范](doc/架构/架构-DDD规范.md) 异常一节，门禁见 `ArchitectureRulesTest` Rule 11）
- **ID 统一 UUID v7 String**（36 位，`IdGenerator` / `UuidV7IdGenerator`）；前端实体 ID 保持 string
- **多模块构建**：修改子模块后启动前必须 `./mvnw install -DskipTests`（或 `clean package -pl <module> -am`），否则 ClassNotFoundException
- **删过资源文件就必须 `clean`**：`install` 只增量复制 `src/main/resources`，**不会删除 `target/classes` 里已移除的文件**。删迁移脚本/配置/模板后若只跑 `install`，老副本仍留在 classpath 上被读取（2026-09-17 实测：删掉 `R__seed_payment_config.sql` 后 `install` 未清 `target/classes`，Flyway 读到陈旧副本、对新 schema 执行而启动失败）。判据：`diff <(ls src/main/resources/**) <(ls target/classes/**)` 有差集就 `clean`
- **开发中增量验证**：改动只跑涉及模块的单测/集成测试，不核查 JaCoCo/PIT 覆盖率、不刷新 `doc/工程指标.md`（整体收口时统一跑一次）
- **STP 标准 API 优先**：优先框架/标准库内置功能，零新增自定义代码是最优方案（例：JWT 走 `oauth2ResourceServer()`，不手写 Filter/工具类）
- **后端补充规范**（事务/命名/返回值/安全要点/踩坑警示/端口隔离）见 [easyorange-backend/AGENTS.md](easyorange-backend/AGENTS.md)；**编码细则**按路径激活的 ECC 规则见 `.claude/rules/ecc/`

## 提交规范（Git 工作流）

- **小步提交**：一个逻辑单元（功能/修复/重构/文档）一个提交，验证通过即提交；禁止攒「收口」大提交、禁止 `git add -A` 批量盲提
- **粒度边界**：一个特性/修复 1~2 个提交（跨模块大特性 2~3 个封顶）；同特性内的跨模块基建（队列注册、常量等）并入特性提交，不单独成提交；本地未推送前可 `amend`/`rebase` 整理，推送后遵守下方「历史纪律」
- **消息格式**：`<type>[(<scope>)]: <一句中文描述>`；type 用 `feat|fix|refactor|docs|test|chore|perf|ci|style|build|revert`（日常以前 6 个为主）；一行说清改了什么/为什么，禁止 `+` 拼接多主题；一般无 body
- **消息与内容对应**：仅含文档文件（`doc/`、`*.md`）的提交 type 必须为 `docs`（commit-msg 钩子机械校验，避免文档改动误标代码 type 误导 bisect/changelog）；代码为主体的提交不要标 `docs`
- **提交前检查**：`git status` + `git diff` 审阅；按文件分组 `git add <路径>`；批量连发多个提交时逐一核对每条消息与 staged 内容对应；禁止把 AI 产物/测试残留/临时文件混入提交
- **格式改动不独立成提交**：import 顺序、占位符改名等纯格式调整揉进所属逻辑提交；`style` type 仅用于修复 spotless/CI 格式校验失败的提交
- **历史纪律**：不重写已推送历史；确需整理先 `git bundle` 备份并校验 `HEAD^{tree}` 一致；tag 只在真实发布时打
- **钩子**：仓库提供 `.githooks/`（commit-msg 格式 + 消息-内容一致性校验 / pre-commit 秒级快检 / pre-push 按变更模块跑测试），启用 `git config core.hooksPath .githooks`，紧急时 `SKIP=git-hooks` 跳过

## 参考索引（按需读取，不常驻上下文）

| 主题 | 位置 | 何时读 |
|------|------|--------|
| 商品/订单状态机 / 举报处理工作流 / 领域文档消费约定 | [领域参考.md](doc/agents/领域参考.md) | 动状态流转、领域术语时 |
| 错误码规范 / 模块依赖边 / 已知问题 | [架构参考.md](doc/agents/架构参考.md) | 改错误码、跨模块、排查已知坑时 |
| ECC 激活表 / 后端约定 / 前端约定 | [开发规范.md](doc/agents/开发规范.md) | 写后端/前端代码前 |
| 常用命令（构建/测试/gh CLI）/ CI/CD | [常用命令.md](doc/agents/常用命令.md) | 构建、测试、启动、部署、GitHub issues 时 |
| 架构规范（版本表 / 系统架构与模块结构 / DDD 规范 / DDD 选型取舍） | `doc/架构/`：入口 [架构-系统架构.md](doc/架构/架构-系统架构.md)，另有 [架构-DDD规范.md](doc/架构/架构-DDD规范.md)、[架构-DDD选型取舍.md](doc/架构/架构-DDD选型取舍.md)、[架构-技术栈.md](doc/架构/架构-技术栈.md)（版本表唯一权威落点） | 深入架构规范时 |
| 安全认证（双 Token 流程 / 配置要点 / OWASP） | [架构-安全认证.md](doc/架构/架构-安全认证.md) | 动认证、权限、脱敏时 |
| 部署（配置分层 / JVM / Docker / K8s） | [架构-部署.md](doc/架构/架构-部署.md) | 改配置分层、打包、部署时 |
| AI 资产管理（AI 主线链路 / 对话与 RAG / 成本与建议价采纳率 / WebSocket 协议） | [doc/集成/AI-资产管理.md](doc/集成/AI-资产管理.md) | 动 AI 链路或沟通链路时 |
| 后端所有 REST + WebSocket 端点 | [doc/集成/API-速查.md](doc/集成/API-速查.md) | 找端点、写接口时 |
| 数据库全局约定 / 表清单 / 迁移脚本索引；Flyway 流程规范 | [DATABASE.md](doc/DATABASE.md) + [架构-数据库迁移.md](doc/架构/架构-数据库迁移.md) | 动表结构、加迁移、查表时 |
| 测试数 / 覆盖率 / 压测数字（数字单一来源）；已知技术债 | [工程指标.md](doc/工程指标.md) + [技术债务清单.md](doc/技术债务清单.md) | 收口统计、写简历数字、被问 trade-off 时 |
| 面试脚本（简历 / 自我介绍 / 追问应答 / 八股 / 代码走读 / 设计题） | [doc/interview/](doc/interview/) | 准备面试、被追问怎么答时 |
| ADR 决策记录（12 个，如 ADR-0007 拒绝 Saga、ADR-0011 UUID v7 主键、ADR-0012 RAG 改 RRF） | `doc/adr/` | 做架构决策、改下单链路、动主键/ID 策略时 |

## Repository Map

`codemap.md` 为 AI 生成的可选索引（已被 `.gitignore` 忽略），若存在则工作前先读对应的 `codemap.md` 了解入口、模式和数据流；不存在时以本文档 + `doc/` 为准。
