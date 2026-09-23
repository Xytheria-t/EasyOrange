# EasyOrange — Java AI Agent 工程化实战

> **定位**：Java AI Agent 工程化实战——按生产级标准做工程化（项目不对外运营，实践对标生产）：多范式工具编排 + RAG 检索增强 + 评估闭环进 CI + 成本/延迟治理 + MCP 工具面 + Langfuse 可观测，AI 链路**可换供应商、可降级、可观测、可评估**。业务：C2C 资产流转（固定价格 + 直发 + 平台不碰货），复杂度留给 AI 工程化与架构；两条主线：卖家「发布助手」/ 买家「对话式找货」；DDD 六边形 + ADR/ArchUnit/PIT 是工程底座叙事。可量化数字以 [doc/工程指标.md](doc/工程指标.md) 为唯一来源；禁止自我削弱叙事。
> monorepo：`easyorange-backend/`（Maven 多模块）· `easyorange-frontend/` · `doc/` · `infra/` · `k8s/` · `load-tests/`（k6）。

## 功能取舍判据（最高优先级，覆盖其它默认倾向）

功能增 / 删 / 改一律以 **「演示 + 面试 ROI」为唯一判据**——不以业务价值、实现成本、代码整洁度，也不以「保持现状 / 最小改动 / 向后兼容」为准。对话中说「项目规则」即指本节；细则在判据通过后照常执行。

- **准入**：面试官 10 秒看懂价值、支撑 15 分钟深挖；两条都不满足优先**删**而非优先改
- **落点**：优先业务主链路；旁挂增强（删掉业务照跑）不作核心叙事
- **方向**：向技术浪潮靠（RAG / Agent 编排 / 评估闭环 / 成本延迟治理）；优先换语料、换落点复用已有链路，不新增旁路
- **双项目分工**：本仓库 = 生产级**单 Agent** 工程化；**多智能体编排归第二项目**（Python LangGraph + 评估，经 MCP 消费本仓库 Java 工具面，2027-03 起）——互补轴是编排范式不是语言，新增 AI 功能前先对照本条
- **可量化**：主链路叙事至少一个可引用数字（口径 / 样本量 / 复现命令当场交代）；LLM-as-Judge 分数不作对外质量证据（门禁可用、对外不可用）。缺数字标 `待补口径`；须真实 API key / 额度才能取的标记后即搁置——不阻塞、不反复回报
- **减法优先**：删除要说清为什么砍
- **允许大改**：可重构 / 删除 / 新增任意模块，不受最小改动束缚；改动可验证、可回滚，同一逻辑单元一次提交闭合

## 工作纪律

- **≥3 步的改动先列 todo 清单**，完成即勾
- **省 token**：没必要的不查 / 不改 / 不报；能一次工具调用不拆两次；输出只留结论与必要边界
- **本项目只用 ZCode 开发**：不建 CLAUDE.md / .cursorrules 等跨工具兼容文件

## 全局硬约束（任何改动都适用，违反即返工）

- **API 统一 `Result<T>`**、分页 `PageResult<T>`、搜索 `SearchPageResponse<T>`；成功判据 `"A0000"`
- **数据库变更必须走 Flyway**（CREATE TABLE 紧凑格式，禁对齐列）；DO 枚举经 `@EnumValue` 持久化，禁手写 TypeHandler
- **领域异常必须继承 `BaseBusinessException`**，用模块专属 `ResultCode` 与具名工厂（`notFound(id)`…）；每模块只一个统一领域异常，不新增叶子异常（门禁 `ArchitectureRulesTest` Rule 11）
- **DTO 转换统一在 `adapter/inbound/web/assembler/`**，Controller/Service 不直接构造 Response
- **ID 统一 UUID v7 String**（36 位），前端实体 ID 保持 string
- **改子模块后启动前必须 `./mvnw install -DskipTests`**，否则 ClassNotFoundException；**删过资源文件必须 `clean`**（install 不删 target 陈旧副本）
- **MCP 只暴露公开只读工具**（检索/详情/类目/规则），禁用户态数据与写路径——外部 client 无用户上下文
- **Elasticsearch 版本硬锁**（客户端与 IK 按它编译），不在 infra 侧单独升级
- **开发中只跑涉及模块的测试**，不查覆盖率、不刷工程指标（收口统一跑）；结构计数只在工程指标单点维护，改代码后跑 `python3 .githooks/check-metrics-drift.py --fix`

## 提交规范（Git 工作流）

- 一个逻辑单元一个提交，验证通过即提交；禁攒大提交、禁 `git add -A`；直接在 `develop` 提交
- 推送仅在用户主动要求时执行
- 消息格式与 type 规则照 `.githooks/commit-msg` 报错改；钩子 `git config core.hooksPath .githooks`（紧急 `SKIP=git-hooks`）

## 参考索引（按需读取，不常驻上下文）

- 状态机 / 领域术语 → [doc/agents/领域参考.md](doc/agents/领域参考.md)；模块职责 / 错误码 / 异常判据 → [doc/agents/架构参考.md](doc/agents/架构参考.md)
- 后端 / 前端约定 → [easyorange-backend/AGENTS.md](easyorange-backend/AGENTS.md) · [easyorange-frontend/AGENTS.md](easyorange-frontend/AGENTS.md)
- 构建 / 测试 / 部署 / gh / CI → [doc/agents/常用命令.md](doc/agents/常用命令.md) + [k8s/README.md](k8s/README.md)；技术栈选型 → [README](README.md#技术栈)
- 数据库 / 表清单 / Flyway 规范 → [doc/DATABASE.md](doc/DATABASE.md)
- 测试数 / 覆盖率 / 结构计数 / 技术债 → [doc/工程指标.md](doc/工程指标.md) + [doc/技术债务清单.md](doc/技术债务清单.md)
- 迭代路线 / 不做清单 → [doc/迭代路线.md](doc/迭代路线.md)；面试脚本 → [doc/interview/](doc/interview/)；ADR → `doc/adr/`；Issues 用 `gh` CLI；REST 端点看 Swagger（`/swagger-ui.html`，无手工清单）
