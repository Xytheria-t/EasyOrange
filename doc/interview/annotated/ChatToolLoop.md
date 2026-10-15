# ChatToolLoop — 逐行注释版

> **基线**：develop `e176dc08`（2026-10-15）。
> 这是**学习材料，不是编译源**——源码在
> `easyorange-ai/src/main/java/com/cartethyia/easyorange/ai/application/chat/ChatToolLoop.java`，
> 那一份保持干净（仓规：注释 ≤ 代码行）。源码改了这里会旧，**以源码为准**。

**这个类是干什么的**：买家「对话式找货」的编排器。一次用户提问进来，它负责把「买家侧的口径」（system prompt、首轮上下文、工具面、轮数上限、预算判定、降级动作）打包交给 `ToolLoopKernel`，内核跑 for 循环、决定何时收口，它再把结果交出去。

---

## 一、import — 先认清"它靠谁干活"

```java
package com.cartethyia.easyorange.ai.application.chat;

// ── 本模块 support 包：横切小工具 ──
import com.cartethyia.easyorange.ai.application.support.FailureReason;       // 把异常压成一行日志原因（日志里不打整个栈）
import com.cartethyia.easyorange.ai.application.support.UntrustedText;       // 剥掉用户文本里的标签形态 → 防提示注入

// ── 本模块 toolcall 包：循环内核与决策（与 listing 链路共用） ──
import com.cartethyia.easyorange.ai.application.toolcall.ToolCallArguments;  // 模型给的参数串反序列化后的视图
import com.cartethyia.easyorange.ai.application.toolcall.ToolCallDecider;    // 真正"调模型选工具"的那个类
import com.cartethyia.easyorange.ai.application.toolcall.ToolCallDecision;   // 一次决策的解析结果（工具名 + 参数 + 原始串）
import com.cartethyia.easyorange.ai.application.toolcall.ToolLoopKernel;     // 循环机制本体（for + 四条出口）
import com.cartethyia.easyorange.ai.application.toolcall.ToolLoopOutcome;    // 出口枚举：FINISHED / ROUND_LIMIT / BUDGET / DECISION_FAILED

// ── 本模块其他 ──
import com.cartethyia.easyorange.ai.config.AiProperties;                     // yaml 配置（轮数上限等从这儿取）
import com.cartethyia.easyorange.ai.domain.enums.ModelCallScope;             // 记账轴：这笔钱记 chat 的账

// ── domain 模型：三类"召回物" + 历史 + 偏好 ──
import com.cartethyia.easyorange.ai.domain.model.AssetDetail;                // 商品详情（召回物之一）
import com.cartethyia.easyorange.ai.domain.model.AssetHit;                   // 商品命中（召回物之一）
import com.cartethyia.easyorange.ai.domain.model.ChatTurn;                   // 一轮历史对话（role + content）
import com.cartethyia.easyorange.ai.domain.model.KnowledgeHit;               // 知识命中（召回物之一）
import com.cartethyia.easyorange.ai.domain.model.UserPreference;             // 长期偏好（key=value）

// ── domain 端口（出站接口，实现都在 adapter 层） ──
import com.cartethyia.easyorange.ai.domain.port.ChatStreamHandler;           // 往 SSE 推 step 事件的出口（可空）
import com.cartethyia.easyorange.ai.domain.port.PromptRegistryPort;          // 按名字从 yaml 取 prompt

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;                                            // Lombok：自动生成 private static final Logger log
import org.jspecify.annotations.Nullable;                                   // 显式声明"这个字段可以为 null"
import org.springframework.ai.support.ToolCallbacks;                        // Spring AI：扫 @Tool 方法生成工具 schema
import org.springframework.ai.tool.ToolCallback;                            // 一个工具的回调描述
import org.springframework.beans.factory.annotation.Autowired;              // 构造器注入
import org.springframework.stereotype.Component;                            // 注册成 Spring bean
```

> `ToolCallDecider`（调模型的决策器）与 `ToolLoopKernel.RoundDecider`（内核每轮的决策契约）是**两层**，别混：前者是"实现"，后者是"接口"。

---

## 二、类头

```java
/**
 * 买家侧多步工具调用循环（ReAct）— 找货对话的编排器：注入首轮上下文、工具面与降级口径，循环机制交给 {@link ToolLoopKernel}
 * （与 listing 链路共用）。
 * <p>
 * 降级口径 = 已积累观察不丢弃：轮数 / 预算超限用观察直接生成；决策失败补一次检索（白名单未含检索的消融臂跳过 —— 补检索会把该臂的检索来源加回来）。
 */
@Slf4j        // 生成 log 字段，类里 log.warn(...) 用它
@Component    // 交给 Spring 容器管理，别处直接构造器注入它
public class ChatToolLoop {
```

**类注释读法**：第一段说"我负责什么"（注入口径）+ "机制在谁那儿"（内核）；第二段是**降级口径**——超限时不丢已找到的东西，直接拿观察生成答案。

---

## 三、两个常量

```java
    /**
     * 机器主体标识 —— 唯一非登录调用方是评估跑批：会话 / 缓存键照常按此主体隔离；{@link #attributedUserId} 收敛成 null 后
     * 偏好写不进（{@link ChatTools} 拒收）、trace 的 user_id 为空。
     */
    public static final String MACHINE_SUBJECT = "machine";

    /** 决策对话的 system prompt 键（与 {@code prompts/ai_chat_tool.yml} 的 name 同名）。 */
    private static final String CHAT_TOOL_PROMPT = "ai_chat_tool_system";
```

- `MACHINE_SUBJECT` = **评估跑批用的假用户**。跑回归时没有真人提问，用 `"machine"` 当身份，好处是会话键 / 缓存键照常按它隔离，不会污染真实用户的数据。
- `CHAT_TOOL_PROMPT` = prompt 在 yaml 里的**名字**。注意它是**决策**用的 prompt（`ai_chat_tool_system`），跟生成用的 `ai_chat_system` 是两个文件——循环里每一轮选工具用的是这一个。

---

## 四、七个依赖（构造器注入）

```java
    private final PromptRegistryPort promptRegistry;   // 按名字取 prompt
    private final ChatToolsFactory toolsFactory;       // 造"工具实例"（有状态，每请求一个）
    private final ToolCallDecider decider;             // 决策：调模型选工具
    private final ToolLoopKernel loopKernel;           // 循环机制（for + 四条出口）
    private final ChatBudgetGuard budgetGuard;         // 预算判定（每轮问一次"耗尽没"）
    private final AiProperties aiProperties;           // 配置（轮数上限等）
    private final ChatLoopMetrics metrics;             // 指标埋点（它实现了内核的 ToolLoopListener）

    @Autowired
    public ChatToolLoop(
            PromptRegistryPort promptRegistry,
            ChatToolsFactory toolsFactory,
            ToolCallDecider decider,
            ToolLoopKernel loopKernel,
            ChatBudgetGuard budgetGuard,
            AiProperties aiProperties,
            ChatLoopMetrics metrics) {
        this.promptRegistry = promptRegistry;   // 逐个赋值，都是 final → 构造完不可变
        this.toolsFactory = toolsFactory;
        this.decider = decider;
        this.loopKernel = loopKernel;
        this.budgetGuard = budgetGuard;
        this.aiProperties = aiProperties;
        this.metrics = metrics;
    }
```

> **为什么不用 `@RequiredArgsConstructor`（Lombok）**？因为这个构造器上挂了 javadoc / 需要 `@Autowired` 显式标注，Lombok 生成的那份接不上。七个依赖是"每个都能说清它服务哪件事"的收敛结果（原先 13 个）。

---

## 五、`Input` — 进来的一整轮请求

```java
    /**
     * 一次循环的输入 — 记忆（历史 / 偏好）由调用方装配，循环只管「决策 → 工具 → 观察」。
     *
     * @param sessionId 可空（请求不带会话），两处消费方各自兜底：trace 的 session_id 补 {@code anonymous} 哨兵、会话记忆 fail-open
     * @param userId 评估跑批传 {@link #MACHINE_SUBJECT}，偏好不落库
     * @param streamHandler 可空：非流式路径不推 step 事件，trace / 指标照常
     * @param allowedTools 工具白名单，判定语义见 {@link #allows}；RAG 有效性对照的「无检索来源」臂据此只挂
     *                     {@link ChatTools#TOOL_FINISH}（消融变量只落在工具装配上）
     */
    public record Input(
            String question,                                  // 用户这轮问的话
            @Nullable String sessionId,                       // 会话 id（多轮记忆的键）；可空
            String userId,                                    // 谁在问（可能是 MACHINE_SUBJECT）
            List<ChatTurn> history,                           // 历史对话（已被上层裁剪过窗口）
            List<UserPreference> prefs,                       // 长期偏好
            @Nullable ChatStreamHandler streamHandler,        // 流式回调；可空 = 非流式
            @Nullable Set<String> allowedTools) {             // 工具白名单；可空 = 不限

        // 便捷构造器：不带白名单的调用方走这个，白名单默认 null（= 全量工具面）
        public Input(
                String question,
                @Nullable String sessionId,
                String userId,
                List<ChatTurn> history,
                List<UserPreference> prefs,
                @Nullable ChatStreamHandler streamHandler) {
            this(question, sessionId, userId, history, prefs, streamHandler, null);   // 转调上面那个，第 7 个参数给 null
        }

        /** 白名单判定（装配过滤与降级补检索共用）：null = 不限（生产路径即全量工具面），非空只放行名单内工具。 */
        public boolean allows(String toolName) {
            return allowedTools == null || allowedTools.contains(toolName);   // 关键：null 表示"不限"而不是"都不允许"
        }
    }
```

**逐项要点**：
- 它是 `record` → **不可变**，字段只能读。
- `history` 和 `prefs` 是**上层装配好塞进来的**——循环自己不去查记忆，这样循环只关心「决策 → 工具 → 观察」。
- `allows` 的 `null = 不限` 这个约定很重要：**白名单为 null 是"放开全部"，不是"一个都不给"**。搞反了生产路径就一个工具都调不了。
- 白名单的**唯一放值方**是评估里的 RAG 消融臂（无检索臂只留 `finish`）。

---

## 六、`Result` — 出去的一整轮结果

```java
    /**
     * 循环结果 — 召回物供生成装配与溯源，outcome / rounds 供指标与降级归因，toolPath 供路由准确率评估（对照金标准集
     * {@code expected_tools}）：rounds 含 finish 轮、不计决策失败轮，降级补检索不进 toolPath。
     */
    public record Result(
            List<KnowledgeHit> knowledgeHits,   // 知识库命中（溯源用）
            List<AssetHit> assetHits,           // 商品命中（溯源用）
            List<AssetDetail> details,          // 查过的商品详情
            ToolLoopOutcome outcome,            // 从哪条出口出来的
            int rounds,                         // 跑了几轮（含 finish 轮）
            List<String> toolPath) {            // 按顺序调过哪些工具（评估路由准确率用）

        /** 出循环时一次性定稿 — 三个召回物来自 {@code tools} 实例上已积累的累加器，循环只有这一处读它。 */
        static Result of(ChatTools tools, ToolLoopOutcome outcome, int rounds, List<String> toolPath) {
            return new Result(
                    tools.knowledgeHits(),   // 从"工具实例"上取，不是从内核结果里取
                    tools.assetHits(),
                    tools.details(),
                    outcome,
                    rounds,
                    List.copyOf(toolPath));  // 拷贝一份不可变列表再交出去
        }
    }
```

**要点**：召回物住在 `ChatTools` 实例的累加器里，`Result.of(...)` 是**全类唯一读它的地方**——出循环时一次性定稿。这样内核完全不需要知道"召回物"这个概念。

---

## 七、`// ── 入口与装配 ──`

```java
    public Result run(Input input) {
        // ① 造工具实例：它有状态（累加器装着本轮的召回物），所以每请求新建、用完即弃 —— 不是单例 bean
        ChatTools tools = toolsFactory.create(attributedUserId(input));

        // ② 交给内核两样东西：spec = 本轮规格（下面拆），roundDecider = "每轮怎么决策"的函数
        //    for 循环、四条出口、trace 全在内核里，这里只喂买家侧口径
        ToolLoopKernel.Result result = loopKernel.run(spec(input, tools), roundDecider());

        // ③ 从 tools 实例取召回物，跟 outcome/rounds/toolPath 打包交付
        return Result.of(tools, result.outcome(), result.rounds(), result.toolPath());
    }
```

```java
    private ToolLoopKernel.Spec spec(Input input, ChatTools tools) {
        // Spec 是 record，10 个字段按位置传 —— 读的时候要数位置（这是 record 传参的代价）
        return new ToolLoopKernel.Spec(
                promptRegistry.require(CHAT_TOOL_PROMPT),      // 1 systemPrompt：按名取，取不到直接抛（不兜底）
                firstUserMessage(input),                       // 2 首条 user 消息（下一节专讲）
                toolCallbacks(tools, input),                   // 3 这轮模型能看到的工具
                aiProperties.chat().maxRounds(),               // 4 轮数上限（yaml，chat = 7）
                budgetGuard::exhausted,                        // 5 函数：内核每轮问一次"预算耗尽没"
                () -> fallbackSearch(input, tools),            // 6 函数：决策失败时的降级动作
                input.sessionId(),                             // 7 会话 id，可空
                attributedUserId(input),                       // 8 归属用户（跑批时为 null）
                input.streamHandler(),                         // 9 流式回调，可空
                metrics);                                      // 10 指标对象（实现了 ToolLoopListener）
    }
```

> **第 5、6 项为什么是函数而不是值**：内核**只收函数不收实现**——它不知道背后是模型还是 Redis，所以 listing 链路能复用同一个内核 bean。`budgetGuard::exhausted` 读作"把 `exhausted` 这个方法本身交给内核"，**什么时候调由内核决定**；第 6 项要带两个参数，所以写成 lambda。

```java
    @Nullable
    private static String attributedUserId(Input input) {
        // 常量写在左边：userId 可能是 null，写成 input.userId().equals(...) 会 NPE
        // 跑批用的 MACHINE_SUBJECT 收敛成 null → 偏好写不进库、trace 的 user_id 为空，防止假用户污染真表
        return MACHINE_SUBJECT.equals(input.userId()) ? null : input.userId();
    }

    /** 白名单在装配层生效 —— 模型拿不到工具 schema 就调不到。 */
    private static List<ToolCallback> toolCallbacks(ChatTools tools, Input input) {
        return Arrays.stream(ToolCallbacks.from(tools))                                   // Spring AI 扫 @Tool 方法 → schema
                .filter(callback -> input.allows(callback.getToolDefinition().name()))    // 按白名单筛掉
                .toList();                                                                // 收成不可变列表
    }
```

> **白名单为什么筛在装配层**：模型看不到某个工具的 schema，就**压根调不出来**——比"模型调了再拒绝"强一个层次。这就是 RAG 消融对照的消融变量。

---

## 八、`// ── 首轮上下文 ──`

```java
    /**
     * 首条 user 消息（历史 / 偏好 / 问题）— 每请求固定、全部轮次共享：改一个字节这轮的 KV cache 就作废。块序按
     * 「已写字节不再重写」排：历史追加式增长（跨轮前缀逐字节复用）→ 偏好少变 → 问题每轮必变，殿后兼取「数据在前、
     * 任务在后」。三个分量剥掉标签形态后统一进块（与生成侧同形）—— 这条上下文决定调哪个工具，散文小标题能被块内
     * 用户文本仿写，剥掉后仿不出来。
     */
    private static String firstUserMessage(Input input) {
        String history = UntrustedText.stripTags(formatHistory(input.history()));        // 先剥标签形态，再进块
        String preferences = UntrustedText.stripTags(UserPreference.format(input.prefs()));
        String question = UntrustedText.stripTags(input.question());
        return """
                <history>
                %s
                </history>

                <user_preferences>
                %s
                </user_preferences>

                <user_question>
                %s
                </user_question>
                """.formatted(history, preferences, question);     // Java 文本块：%s 按顺序填三个分量
    }
```

**三件事**：
1. **顺序有讲究**：历史 → 偏好 → 问题。历史跨轮只追加、前缀稳定，放最前面能吃到供应商的 KV cache 折扣；问题每轮都变，放最后。
2. **三个分量都先 `stripTags`**：用户如果在自己问题里写 `</user_question>`，就能在模型看到的上下文里"另开一块"——而这条上下文决定调哪个工具，等于让人操纵工具选择。剥掉标签形态就仿不出来了。
3. **首条 user 消息全轮次共享**：它在循环里只算一次，后面每轮都是"这一条 + 累积的 assistant/tool 消息"。

```java
    private static String formatHistory(List<ChatTurn> history) {
        if (history.isEmpty()) {
            return "(无)";     // 空历史给个占位符，别让块里出现空白（模型容易把空块当缺失信息）
        }
        return history.stream()
                .map(turn -> (turn.role().isUser() ? "用户" : "助手") + ": " + turn.content())   // 角色 + 内容
                .collect(Collectors.joining("\n"));
    }
```

---

## 九、`// ── 决策 ──`

```java
    /** 决策恒用快模型（路由角色 decision），记账进 chat 预算；入参摘要按本链路工具面取。 */
    private ToolLoopKernel.RoundDecider roundDecider() {
        // 这个方法不决策，它"造一个决策函数"交给内核 —— 所以叫 roundDecider() 而不是 decide()
        return (sessionId, messages, callbacks) ->                       // 内核每轮递进来三样：谁在问 / 全部消息 / 可用工具
                decider.decide(                                          // 真正调模型的是 ToolCallDecider
                        sessionId, messages, callbacks,
                        ModelCallScope.CHAT,                             // 本链路口径①：这笔钱记 chat 的账（listing 写 AUTO_LISTING）
                        ChatToolLoop::toolInputOf);                      // 本链路口径②：把工具参数压成一行摘要（listing 有自己的压法）
    }
```

> `ChatToolLoop::toolInputOf` 读作「把 `toolInputOf` 这个**函数本身**传过去」，不是现在调用它。

```java
    /** 工具入参摘要（trace 落库与失败日志用）— 工具名缺失已在 {@link ToolCallDecision} 构造期收敛成空串，这里走 default，
     * 不再判空。 */
    private static String toolInputOf(ToolCallDecision decision) {
        ToolCallArguments parsed = decision.parsedArguments();
        return switch (decision.tool()) {                                   // 按工具名分派，取"这个工具最值得记的那个参数"
            case ChatTools.TOOL_PRODUCT_DETAIL -> parsed.productId();       // 查详情 → 记商品 id
            case ChatTools.TOOL_COMPARE_ASSETS ->                           // 对比 → 记商品 id 列表（null 就保持 null）
                parsed.productIds() == null ? null : String.join("、", parsed.productIds());
            case ChatTools.TOOL_REMEMBER_PREFERENCE ->                      // 记偏好 → 记 key=value
                orEmpty(parsed.preferenceKey()) + "=" + orEmpty(parsed.preferenceValue());
            default -> parsed.query();                                      // 其余（检索类）→ 记查询词
        };
    }

    private static String orEmpty(@Nullable String value) {
        return value == null ? "" : value;   // 小工具：null 当空串，省得到处判空
    }
```

---

## 十、`// ── 降级 ──`

```java
    /** 决策失败的降级补检索 — 判重口径与工具步一致（在 {@code ChatTools} 内），自身故障不外抛（对话不死在降级路径）。 */
    private void fallbackSearch(Input input, ChatTools tools) {
        if (!input.allows(ChatTools.TOOL_KNOWLEDGE_SEARCH)) {
            return;   // 白名单里没有检索工具 → 直接跳过（RAG 消融臂就靠这里不把检索来源加回来）
        }
        try {
            tools.searchKnowledgeForFallback(input.question());   // 按原始问题补检索一次
        } catch (Exception e) {
            log.warn(                                                // 降级路径自己挂了也不能把对话带死 —— 只记日志
                    "action=tool_call_fallback_search_failed, sessionId={}, reason={}",
                    input.sessionId(),
                    FailureReason.of(e));
        }
    }
}
```

**为什么这个类要有降级**：模型没返回工具调用 = 决策失败。此时如果不补一次检索，用户会拿到一个"什么都没查"的答案。补一次检索至少让生成侧有点东西可依据——**但前提是白名单里有检索工具**，否则等于把消融臂的消融变量偷偷加回去。

---

## 全类一句话

**`run` 造工具 → 拼规格 → 交内核；`spec` 把买家侧口径打成一个包；`roundDecider` 把"决策"这件事连同两条链路口径交给内核；`fallbackSearch` 是决策失败时唯一的补救动作。** 这个类里**没有 for 循环**——循环、四条出口、trace 都在 `ToolLoopKernel`，它只负责"买家侧怎么说"。
