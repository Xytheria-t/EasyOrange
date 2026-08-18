# EasyOrange — LLM × DDD：Java 架构工程化实战

> **定位**：LLM × DDD 工程化实战项目 — 在 DDD 六边形架构里集成 LLM，让 AI 链路可换供应商、可降级、可观测。**两条技术主线**：AI 应用工程化（6 决策点 + 轻量 Agent 编排 + 8 件套工程化）+ 架构落地（DDD + 分布式可靠性 + ADR/ArchUnit/PIT 治理三板斧）。**业务**：C2C 资产流转（固定价格 + 直发 + 平台不碰货），把复杂度留给架构与 AI 工程化。**工程亮点**：DDD 六边形 + CQRS · 事件驱动 + Outbox + DLQ 三级重试 + traceId 全链路 · 分布式锁防超卖 · AI 8 件套（Spring AI 2.0 框架化 + Redisson 令牌桶 + stale 降级 + Prompt YAML + TokenBudget + Embedding 真实现 + 多模态 Vision + 4 路并行 Tool Calling）· ES 搜索 + IK 分词 · ArchUnit 10 条规则 · 10 ADR · 2,400+ 测试（JaCoCo 行覆盖 + PIT 变异测试双重门禁）· SpringDoc OpenAPI 3 · Biome 0 errors。**2025 年 11 月启动**。

## Agent skills

- **Issue tracker**：Issues 与 PRDs 存放在 GitHub issues，用 `gh` CLI 读写（命令模板见 [doc/agents/常用命令.md](doc/agents/常用命令.md)「GitHub Issues / PR」）；默认 triage label：`needs-triage` / `needs-info` / `ready-for-agent` / `ready-for-human` / `wontfix`
- **Domain docs**：single-context 布局，领域术语与 ADR 消费约定见 [doc/agents/领域参考.md](doc/agents/领域参考.md)「Agent 领域文档消费约定」

## 项目结构

monorepo：`easyorange-backend/`（Spring Boot 后端，11 Maven 模块，各模块规范见模块内 `AGENTS.md`）· `easyorange-frontend/`（React + Vite + TypeScript + TanStack Query）· `doc/`（架构 / 集成 / ADR / agents 参考 / DATABASE / PRODUCT_DIRECTION）· `infra/`（IaC：Prometheus / Grafana / ES IK 镜像）· `k8s/`（K8s kustomize，无状态应用层）· `load-tests/`（k6 压测）· `.claude/rules/ecc/`（AI 编码规则 ECC：common/java/typescript/react/web）

## 技术栈

| 层 | 技术 |
|---|------|
| **后端** | Java 25, Spring Boot 4.0.7, MyBatis-Plus 3.5.16 |
| **前端** | TypeScript, React |
| **数据库** | MySQL 8.4.11, Redis 8.10.0 |
| **消息队列** | RabbitMQ 4.3.4 (Spring AMQP 4.0.x) |
| **搜索引擎** | Elasticsearch 9.2.8 (IK 中文分词器) |
| **认证** | JWT Access (RSA) + Opaque Refresh (Redis, HttpOnly Cookie) |
| **迁移** | Flyway 11.15.0 |
| **部署** | Docker, docker-compose, compose.yaml（显式 env 直连）+ **K8s/kustomize** (k8s/, 无状态应用层) |

## 全局硬约束（任何改动都适用，违反即返工）

- **API 统一返回 `Result<T>`**；分页返回 `PageResult<T>`；搜索返回 `SearchPageResponse<T>`（含 `records/total/current/size/pages` + `facets` + `aiEnhancement`）。判断成功：`"A0000".equals(code)`
- **数据库变更必须通过 Flyway 迁移脚本**（CREATE TABLE 用紧凑格式，禁止对齐列）；DO 枚举字段经 `@EnumValue` 注解持久化（内置 `MybatisEnumTypeHandler`，禁止手写 TypeHandler）
- **DDD 分层**：domain → application → adapter，依赖方向单向向内；聚合根不可变（`@Builder(toBuilder = true)`），值对象用 `record`
- **CQRS + ACL 隔离**：命令与查询分离（product/order/payment/message）；跨模块必须通过 Port/ACL 适配，禁止直接依赖领域模型/Mapper
- **Assembler 模式**：DTO 转换统一在 `adapter/inbound/web/assembler/`，禁止在 Controller/Service 直接构造 Response DTO
- **异常**：领域异常必须继承 `BaseBusinessException`，禁止直接抛非其子类的 RuntimeException（否则落 500 兜底）；抛异常用 `BusinessException.of(...)` / `FileException.of(...)`；用模块专属 `ResultCode`（如 `ProductResultCode`），禁止回退全局 `B0002`
- **ID 统一 UUID v7 String**（36 位，`IdGenerator` / `UuidV7IdGenerator`）；前端实体 ID 保持 string
- **多模块构建**：修改子模块后启动前必须 `./mvnw install -DskipTests`（或 `clean package -pl <module> -am`），否则 ClassNotFoundException
- **开发中增量验证**：改动只跑涉及模块的单测/集成测试，不核查 JaCoCo/PIT 覆盖率、不刷新 `doc/工程指标.md`（整体收口时统一跑一次）
- **STP 标准 API 优先**：优先框架/标准库内置功能，零新增自定义代码是最优方案（例：JWT 走 `oauth2ResourceServer()`，不手写 Filter/工具类）
- **后端补充规范**（事务/命名/返回值/安全要点/踩坑警示/端口隔离）见 [easyorange-backend/AGENTS.md](easyorange-backend/AGENTS.md)；**编码细则**按路径激活的 ECC 规则见 `.claude/rules/ecc/`

## 提交规范（Git 工作流，2026-08 起）

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
| 数据库表清单 / 商品状态机 / 举报处理工作流 / AI 能力清单 | [领域参考.md](doc/agents/领域参考.md) | 动表结构、状态流转、AI 链路时 |
| 后端架构核心原则 / 模块依赖关系 / 错误码规范 / 已知问题 | [架构参考.md](doc/agents/架构参考.md) | 改架构、跨模块、异常/错误码、排查已知坑时 |
| 环境变量 / ECC 激活表 / 后端约定 / 前端约定 | [开发规范.md](doc/agents/开发规范.md) | 写后端/前端代码前 |
| 常用命令（构建/测试/gh CLI）/ CI/CD | [常用命令.md](doc/agents/常用命令.md) | 构建、测试、启动、部署、GitHub issues 时 |
| 架构文档（技术栈/系统架构/模块结构/DDD规范/安全认证/数据库迁移/部署演进） | [doc/架构/架构.md](doc/架构/架构.md) | 深入架构规范时 |
| AI 资产管理（6 决策点 / 营销文案 / WebSocket 协议） | [doc/集成/AI-资产管理.md](doc/集成/AI-资产管理.md) | 动 AI 决策点或沟通链路时 |
| 后端所有 REST + WebSocket 端点 | [doc/集成/API-速查.md](doc/集成/API-速查.md) | 找端点、写接口时 |
| 测试数 / 覆盖率单一来源 | [doc/工程指标.md](doc/工程指标.md) | 收口统计时 |
| ADR 决策记录（10 个，如 ADR-0007 拒绝 Saga） | `doc/adr/` | 做架构决策、改下单链路时 |

## Repository Map

`codemap.md` 为 AI 生成的可选索引（已被 `.gitignore` 忽略），若存在则工作前先读对应的 `codemap.md` 了解入口、模式和数据流；不存在时以本文档 + `doc/` 为准。
