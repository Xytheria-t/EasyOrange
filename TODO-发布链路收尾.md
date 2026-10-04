# 发布链路多步化 · 收尾待办

> 2026-10-04 会话交接。后端已完成并分 6 个提交落在 develop（`497ce73f` → `5e5a7539`），
> 前端可视化与真实评估跑批未做，步骤在这里，做完即可删本文件。

## 已完成（验收对照）

| 验收项 | 状态 |
|---|---|
| 1. `./mvnw -pl easyorange-ai -am install -DskipTests` | ✅ 通过 |
| 2. `./mvnw -pl easyorange-ai test` 全绿 | ✅ 377 个（含 ToolCallLoopTest 零断言改动全绿、PromptContentTest 9、ListingToolsTest 6、AutoListingAppServiceTest 8） |
| 3. `python3 .githooks/check-metrics-drift.py` | ✅ OK（prompt_templates=6，golden_set_cases=62） |
| 4. GoldenSetEvaluator 跑发布多步用例报平均步数与 toolPath 分布 | ⏳ 待真实 key，见下 |
| 5. 前端 /publish 步骤可视化 | ❌ 未做，见下 |
| 6. 改动说明 | ✅ 见本文末尾 |

## 待办一：前端 /publish 步骤可视化（提交⑥）

后端已就绪：新端点 `POST /api/ai/auto-listing/stream`（事件 `step`/`done`/`error`，无 `token`/`sources`；
`done` 的 data 是 AutoListingResult 的 JSON 文本，`parseRaw` 会原样返回字符串，前端自行 `JSON.parse`）。
发布工具面四个工具与 chat 同名，前端只需补 `list_categories`：

1. `src/pages/playground/ThinkingProcess.tsx`：`STEP_LABELS` 加 `list_categories: '查类目'`，
   `StepIcon` 加一个 case（如 lucide 的 `List`）。
2. `src/pages/playground/PlaygroundPage.test.tsx`：`BACKEND_TOOLS` 映射加 `list_categories`（该测试是同步断言）。
3. 共享样式：把 `playground.css` 里 `.playground-think*` 的 18 处选择器抽成 `ThinkingProcess.css`
   并在 `ThinkingProcess.tsx` 自身 import（前端 AGENTS 约定：共享组件样式必须在组件文件 import），
   `playground.css` 删掉对应段。
4. `src/api/aiApi.ts`：加 `autoListingStream(imageUrls, onEvent, signal)` → `streamChat('/ai/auto-listing/stream', ...)`；
   原 `autoListing` 函数若无其他调用方则删（先查 e2e 是否 mock 了 `/api/ai/auto-listing`，是则连 e2e 一起改）。
5. `src/hooks/useAutoListing.ts`：切流式调用，新增 `steps: AgentStep[]` 状态（`step` 事件 append、
   重试清空）；`done` 事件 `JSON.parse(event.data)` 成 `AutoListingResult` 后走原有 setResult/toast 分支；
   `error` 事件沿用 failureMessage 逻辑。
6. `src/hooks/useAutoListing.test.tsx`：msw 桩从 `http.post('/api/ai/auto-listing')` 改成 SSE 响应
   （参考 PlaygroundPage.test 的 emit 做法），补一条「步骤事件进 steps 状态」的断言。
7. `src/pages/products/PublishPage.tsx`：AI 识别区（`onAnalyze` 附近）渲染
   `<ThinkingProcess steps={steps} thinking={isLoading} />`。
8. 验证：`npm test` 全绿（frontend-ci 口径），`npm run build` 过。

## 待办二：真实评估跑批（验收 #4，需真实 key）

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

## 待办三（小）：收尾杂项

- `doc/interview/02-代码走读.md` §2 工具面一节仍只写 ChatTools，可在面试前跑默认模式校验时顺手补一句
  「ListingTools 同构复用 §1 内核与 §2 的观察机制」（非阻塞）。
- 工作区里 `doc/interview/README.md`、两个 AGENTS.md、`HeroSection.*` 的改动是另一会话所留，与本任务无关，勿混提交。

## 改动说明（验收 #6）

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
