# Agent 诊断与升级路线

> 2026-09-20 基于代码实测的内部工程文档（两个并行代码扫描 + 核心文件人工复核），供后续实施会话交接用。
> 对外 / 简历口径一律以 [工程指标.md](工程指标.md) 为准，本文不重复维护数字；诊断中的弱项表述**不进对外叙事**。
> 交接阅读顺序：本文 → [easyorange-backend/AGENTS.md](../easyorange-backend/AGENTS.md)「模块要点 → ai」→ `AgentLoopRunner`。

## 一、诊断结论

总判断：**骨架专业、接口层过时、数字缺位**。

- 工程治理层（降级 / 预算 / 观测 / 评估 / MCP）是真实代码而非叙事，高于 demo 水准——含金量的真实来源
- Agent 核心模型接口层是 prompt 约定 JSON（`response_format=json_object` + 手写 switch 分发），未走原生 function calling——「不专业」感的精确来源
- 可对外引用的 AI 效果数字当前为 0：唯一实测数字（旧口径的 Judge 均分 / hit@5）已在 5 篇语料时代被文档自己作废，新口径数字全部「待补」

### 1.1 弱项清单

| # | 问题 | 现状 | 位置 |
|---|---|---|---|
| W1 | 非原生 function calling | 决策靠 prompt 约定 JSON + 手写 switch 分发，无工具 JSON Schema、无供应商侧结构化约束；MCP 工具面已用规范 `@McpTool` 注解 schema，agent 循环自己没用 | [AgentLoopRunner.java:236](../easyorange-backend/easyorange-ai/src/main/java/com/cartethyia/easyorange/ai/application/service/AgentLoopRunner.java) + [ai_chat_tool.yml](../easyorange-backend/easyorange-ai/src/main/resources/prompts/ai_chat_tool.yml) |
| W2 | 可引用效果数字为 0 | 平均步数 / 降级率 / 步级 p95 / 注入 token p50-p95 / 裁剪触发率 / 新口径金标准分数全部待补（指标管道已就绪，只缺跑量） | [工程指标.md](工程指标.md) §1.3 各「待补实测」行 |
| W3 | 无跨供应商容灾 | 每场景单供应商，无 failover 链、无应用层断路器，降级是 stale 旧答案而非切换模型 | `AiChatService` |
| W4 | 预算存储单机 | `TokenBudgetStore` 仅 `InMemoryTokenBudgetStore` 实现，多实例下日预算失准 | [AiConfig.java](../easyorange-backend/easyorange-ai/src/main/java/com/cartethyia/easyorange/ai/config/AiConfig.java) |
| W5 | 决策无修复重试 | 决策 JSON 解析失败即降级单次检索，无带错误反馈的修复轮（W1 落地后此问题基本消失） | `AgentLoopRunner.decideStep` |

> W1 / W5 已随 P0-1 落地修复（见 §二）；W2 卡在真实 key（P0-2 的跑量需要可用额度）；W3 / W4 见 P2 / P1。

### 1.2 强项清单（重构中不得破坏）

| 机制 | 位置 |
|---|---|
| 降级三口径显式成文、已积累观察不丢弃、收敛到确定性生成 | `AgentLoopRunner` 类注释 + `executeLoop` |
| 流式取末帧真实 usage 记账、embedding 无用量高估兜底、AOP 与流式共用同一判定 | `AiModelSupport.recordBudgetUsage` / `TokenBudgetAspect` |
| 降级结果不写缓存（防供应商抖动被缓存固化） | `AiSearchEnhancerAdapter` |
| 前缀稳定多角色消息组装（吃供应商 KV-cache 折扣）+ 标签块注入防御 | `AiChatService.buildMessages` |
| 评估反摆设设计：同域干扰语料防 hit@5 恒 100%、评审覆盖率防幸存者平均、门禁键缺失加载期报错 | `eval/` + `baselines.yaml` |
| 步级 trace 落库 `eo_agent_step_trace` + 调用日志 `eo_ai_call_log` + Micrometer（loop/tool/step/context 维度） | `AgentTraceRecorder` / `AgentLoopRunner:441-461` |

### 1.3 面试追问风险（现状下会被打穿的三个点）

1. 「为什么不用原生 function calling？」——DeepSeek / OpenAI 兼容协议均支持 tools，「供应商可移植性」辩护不成立；W1 落地后此问题消失
2. 「评估闭环的数字呢？」——门禁机制真实但无可引用新鲜基准；P0-2 落地后消失
3. 「多实例部署预算怎么算？」——W4；P1 落地后消失

## 二、修复清单（按面试 ROI 排序）

### P0-1 原生 tool calling 迁移（W1 + W5）

> 状态：**已实施** —— 工具改由 `AgentTools` 的 `@Tool` 注解定义 schema（随请求走原生 tool calling），
> 循环仍手写在 `AgentLoopRunner`；W5 由「失败观察 → 模型换参数重试」自然消除。
> **后续演进（2026-09-20）**：偏好提取不再搭在 finish 轮参数上，已拆成独立工具 `remember_preference`
> （原方案在步数超限 / 预算耗尽 / 决策失败三条降级路径下会静默丢失偏好）；同时工具面由 4 个扩到 7 个，
> 新增两个计算类工具（`market_price_stats` / `compare_assets`）——原工具面全是只读检索，循环存在的
> 唯一意义是换关键词重试，补上计算类工具后第 N 步才真正依赖第 N-1 步的输出。步数上限随之 5 → 7。
> 跨会话并行的踩坑（Maven 必须串行、改对方文件前先看两侧 mtime）见 [常用命令.md](agents/常用命令.md) 的并发提示。
> **API 实况**：Spring AI 无 `internalToolExecutionEnabled`（自动工具执行已收进 ChatClient 的
> ToolCallingAdvisor），`ChatModel.call` 本就不执行工具 —— 走 `AiModelSupport.callWithTools` 即天然满足
> 「关闭框架内自动执行、循环控制权留在 runner」。

- **目标**：`AgentLoopRunner` 决策接口从 prompt 约定 JSON 迁移到 Spring AI 原生 tool calling；**保留手写循环**（「手写循环 + 原生模型接口」的叙事优于全托管框架循环）
- **方案**：`@Tool` 注解（或 `ToolCallback`）定义 4 个工具（knowledge_search / product_search / product_detail / finish），`internalToolExecutionEnabled(false)` 关闭框架内自动执行、循环控制权留在 `AgentLoopRunner`；具体 API 以仓库内 Spring AI 实际签名为准
- **迁移点**：
  - 工具参数 schema 从 prompt 文案移入注解；`ai_chat_tool.yml` 瘦身（只留规则 / 偏好约束 / 注入防御声明），`PromptContentTest` 模板清单同步
  - **偏好提取是决策 JSON 的旁路字段**（`AgentStepDecision.preference`），迁移后需保留等价能力——**已定（2026-09-20）：独立工具 `remember_preference`**，不再搭在 finish 轮上（原方案在三条降级路径下会丢偏好）
  - `decision_failed` 降级路径保留（模型故障仍可能发生），决策解析失败语义由「JSON 解析异常」变为「无 tool call 返回」
- **收益**：工具获得供应商侧校验的 JSON Schema、决策格式错误基本消失、具备并行工具调用基础
- **验收**：
  - 涉及模块 `./mvnw test` 全绿，`AgentLoopRunnerTest` 适配
  - SSE step 事件协议不变（前端零改动）；`eo_agent_step_trace` 表结构与字段语义不变
  - 降级三口径行为不变；指标 key（loop / tool / steps / step.duration）不变
  - 一个逻辑单元一次提交（含 prompt 瘦身与测试适配）

### P0-2 重跑评估回填数字（W2）

- dispatch `ai-eval.yml` 拿 23 篇语料 + RRF 下的新鲜金标准分数（真实调用成本不低，按需 dispatch）
- 自驱流量（curl 循环打 `/api/ai/chat/stream` 与非流式口）回填：平均步数 / 降级率 / 步级延迟 p95 / 注入 token p50-p95 / 裁剪触发率
- 回填落点：[工程指标.md](工程指标.md) 对应「待补实测」行，遵守其维护约定

### P1 TokenBudgetStore Redis 实现（W4）

- 已有 port，补一个 Redis adapter + 配置切换；口径与内存实现一致（日窗口、used + maxPerCall > dailyLimit）

### P2 跨供应商 failover / 断路器（W3，可选）

- 现有 stale 缓存降级叙事已自洽；仅在剩余预算充足时做，不做不进叙事

## 三、明确不做

- 多智能体编排——归第二个项目（LangGraph，经 MCP 消费本仓库工具面）
- LLM 摘要压缩历史——maxSteps ≤ 5 + 6 轮窗口下收益小、每轮摘要翻倍成本（既有取舍，见 [工程指标.md](工程指标.md) §1.3 上下文窗口治理行）
