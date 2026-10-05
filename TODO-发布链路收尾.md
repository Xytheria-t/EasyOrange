# 发布链路多步化 · 收尾待办

> 2026-10-04 会话交接，2026-10-05 更新：后端（`497ce73f` → `5e5a7539`）与前端可视化均已落在 develop。
> 剩下的真实评估跑批需真实 key，按 [项目规则](AGENTS.md)「需真实调用大模型的一律先不做」搁置——
> 本文件降级为**搁置标记 + 跑法备查**，不再是要做的事。

## 已完成（验收对照）

| 验收项 | 状态 |
|---|---|
| 1. `./mvnw -pl easyorange-ai -am install -DskipTests` | ✅ 通过 |
| 2. `./mvnw -pl easyorange-ai test` 全绿 | ✅ 377 个（含 ToolCallLoopTest 零断言改动全绿、PromptContentTest 9、ListingToolsTest 6、AutoListingAppServiceTest 8） |
| 3. `python3 .githooks/check-metrics-drift.py` | ✅ OK（prompt_templates=6，golden_set_cases=62） |
| 4. GoldenSetEvaluator 跑发布多步用例报平均步数与 toolPath 分布 | ⏸ 需真实 key — 按项目规则先不做，跑法见下备查 |
| 5. 前端 /publish 步骤可视化 | ✅ `POST /api/ai/auto-listing/stream` 逐步进 `useAutoListing` 的 `steps`，`ThinkingProcess` 面板同轮渲染；前端 924 测试全绿 + `npm run build` 过 |
| 6. 改动说明 | ✅ 见本文末尾 |

## 搁置：真实评估跑批（验收 #4，需真实 key — 按项目规则先不做，跑法备查）

- 门槛跑法照 [常用命令](doc/agents/常用命令.md)：起 ES（`docker compose --profile search up -d --wait elasticsearch`）
  + 根 `.env` 有 `EASYORANGE_AI_API_KEY`，然后
  `./mvnw -pl easyorange-application verify -Dtest=__NoMatchingUnitTests__ -Dsurefire.failIfNoSpecifiedTests=false -Dit.test=GoldenSetRegressionIT -Deasyorange.search.elasticsearch.enabled=true`。
- 要看的数字在运行日志：`golden case list-*  listing route: ... outcome=..., rounds=...` 逐用例一行，
  汇总行 `Golden set listing routing eval: hit x/10, accuracy = ..., avg rounds = ...` ——
  把 **平均步数 + toolPath 分布** 抄进 `doc/工程指标.md` 的编排主线行（替换「待补口径」位）。
- 两个后续判断：
  - `baselines.yaml` 的 `routing.listing-min-accuracy: 0.6` 是先验值——全对说明门禁太松（上调），
    大面积挂在同一轮先修 `auto_listing_tool_system` prompt 再谈阈值；
  - 若禁售品类（list-009/010）没走早收敛，先看决策轮上下文拼装（`ListingPromptAssembler`）与
    `auto_listing_tool_system.yml` 的禁售规则措辞，不要动路由判据。

## 改动说明（验收 #6，后端会话记录）

**抽了什么**
- `ToolLoopKernel`（application/support）：ReAct 循环机制整体下沉——四条降级出口、assistant tool_calls +
  role=tool 协议回填、步级 trace/SSE、toolPath、listener 记账。判据：循环机制与「这是买家还是卖家」无关，
  写死在 ToolCallLoop 里发布链路就只能复制一份（复制 = 判重与降级口径双份漂移）。
- `RetrievalObservations`：60% 冗余判据、判重键、观察文案、TOP_K、ObservationTextConverter 单一来源。
  判据：这些是行为承载且两边逐字共用，双份必漂。
- `ToolCallDecider/ToolCallDecision/ToolCallArguments` 下沉 support：listing 复用决策器（重试一次、
  坏调用整轮作废的协议口径）而不造成 listing→chat 反向依赖。
- `ChatBudgetGuard` 不动，listing 的循环中途预算判定直接用 `TokenBudgetPolicy` 单点判定式 + 注解兜底常量。

**复用了什么**
- 循环内核与决策器（上）；`KnowledgeRetrievalAppService` / `AssetSourcingAppService` / `PriceStats` /
  `CategoryCatalogPort` 原样进 ListingTools 的工具体；工具名四个与 chat 同名
  （golden-set 校验、STEP_LABELS、trace 工具列三处只认一份词汇，一致性测试钉住）。
- 买家侧对外行为零变化：`ToolCallLoop` 公开 API 与构造器签名未动，20+ 测试类零断言改动即全绿。

**砍了什么**
- `AutoListingAppService` 的单次多模态直通链路：一次调用答不了「这个价行不行 / 这个品类能不能挂」，
  答案在平台数据里不在图片里；原来砍掉的是「同一批产出的重复调用」，现在加回的是「每步依赖上一步输出」
  的查证链，不是回滚。
- `categoryCatalogHint` 的调用侧拼接句（auto_listing v2.0.0 模板已收编该职责）。

**两个设计判断**
1. 图片入轮 = 视觉预识别一次转结构化线索进首轮，最终生成带原图。否决「图片进决策消息」：决策模型是
   chat_tool 场景的纯文本快模型，塞图要么换视觉模型（9 图 × 6 轮重发，成本治理作废）要么报错。
2. listing 决策失败的降级是 no-op（chat 是补检索）：发布链路的失败态就是「行情缺失 → price 置 null 留卖家」，
   是设计内降级；强塞猜测关键词的检索只会喂噪声。
