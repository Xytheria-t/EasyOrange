package com.cartethyia.easyorange.ai.application.support;

import com.cartethyia.easyorange.ai.config.AiProperties;
import com.cartethyia.easyorange.ai.domain.enums.AiCallScope;
import com.cartethyia.easyorange.ai.domain.port.AiCallLogPort;
import com.cartethyia.easyorange.ai.domain.port.TokenBudgetStorePort;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

/**
 * AI 调用记账 — 每次 LLM/Embedding 调用落一条 eo_ai_call_log，并把真实 token 用量累加进场景预算。
 * <p>
 * 独立成类是为了让「治理只有一个出口」成为结构性事实而不是注释约定：{@code TokenBudgetAspect} 只做
 * 前置检查（切面拿不到响应体，只能拿到业务 DTO），本类是全工程唯一把供应商回报的 usage 写进
 * {@link TokenBudgetStorePort} 的地方。绕开记账直接调 {@code ChatModel} 的新代码在类型上就不成立 ——
 * 调用必须经 {@link AiModelSupport}，记账就必然跟着发生。
 * <p>
 * 两笔账的口径刻意不同，别互相套用：<b>调用日志记 0 而不估算</b>（估算值混进成本报表比缺数据更危险，
 * 报表要的是可对账的真值）；<b>预算未回报才退化为场景上限估算</b>（宁高估也不让预算静默失效）。
 * <b>失败调用不记账</b>：异常路径没有用量可依据，按上限估算会让故障期虚烧日预算（chat 口径下约
 * 100 个失败请求烧穿 30 万日限），故障恢复后整个场景被前置检查锁死。
 * <p>
 * 两笔账都是调用副产物，写失败只 debug 一行、绝不抛出。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiCallRecorder {

    /** {@code eo_ai_call_log.error_msg} 列宽（VARCHAR(512)）—— 不截断则超长时整条记录写不进去，而写不进去的恰是失败记录。 */
    private static final int ERROR_MSG_MAX = 512;

    private final AiCallLogPort callLogPort;
    private final TokenBudgetStorePort budgetStore;
    private final AiProperties aiProperties;

    /**
     * 执行一次模型调用并记账，返回调用结果；异常原样重抛（记账在 finally 里先落，不吞业务故障）。
     *
     * @param model 被调用的模型实例，只取实现类简名做日志维度（换供应商一眼能从成本报表看出来）
     * @param promptText 提示词全文，入库只留 MD5（报表按 hash 归并，响应文本也可能很大）
     */
    public <T> T record(AiCallScope scope, Object model, String promptText, Supplier<CallOutcome<T>> call) {
        long startNanos = System.nanoTime();
        CallOutcome<T> outcome = null;
        String errorMsg = null;
        try {
            outcome = call.get();
            return outcome.value();
        } catch (Exception e) {
            errorMsg = e.getMessage();
            throw e;
        } finally {
            recordLog(scope, model, promptText, outcome, startNanos, errorMsg);
            recordBudget(scope, outcome);
        }
    }

    /**
     * 落调用日志（成本报表数据源，同时是 LLM-as-Judge 离线评估的输入）。
     * <b>成功与否由 {@code outcome != null} 推导</b>：只有调用正常返回才会有 outcome，异常路径直接
     * 跳进 finally，两者不同时成立；再单传一个 success 标志只会多出一个可能与事实不符的状态。
     */
    private void recordLog(
            AiCallScope scope,
            Object model,
            String promptText,
            @Nullable CallOutcome<?> outcome,
            long startNanos,
            @Nullable String errorMsg) {
        try {
            Tokens tokens = Tokens.of(outcome == null ? null : outcome.usage());
            callLogPort.record(
                    scope.name(),
                    model.getClass().getSimpleName(),
                    md5(promptText),
                    outcome != null && outcome.value() instanceof String text ? text : null,
                    (System.nanoTime() - startNanos) / 1_000_000,
                    tokens.input(),
                    tokens.output(),
                    outcome != null,
                    truncate(errorMsg));
        } catch (Exception e) {
            // callLogPort 实现内部已吞异常，此处兜底；副产物写不进去不该让人误以为主链路也挂了
            log.debug("action=ai_call_log_failed, scope={}, reason={}", scope, e.getMessage());
        }
    }

    /**
     * 记录本次调用的 token 用量（场景键与 {@code @TokenBudget(scenario=...)} 对齐）。
     * 有真实用量就按真实值累加；供应商未回报时退化为场景配置的单次上限（偏高，但比「预算永远为 0、
     * 限流静默失效」安全）。<b>失败调用不记账</b>：{@code outcome == null} 即异常路径。
     * <p>
     * 场景在 yaml 缺配置时这里什么都不记（{@code configured == null} 直接返回）：注解上的
     * {@code maxTokensPerCall} 是切面前置检查的兜底契约，本路径取不到它属于配置缺失而非可猜的默认值，
     * 宁可漏记也不要按猜的数写进预算。
     */
    private void recordBudget(AiCallScope scope, @Nullable CallOutcome<?> outcome) {
        if (!aiProperties.budget().enabled() || outcome == null) {
            return;
        }
        try {
            String scenario = scope.budgetScenario();
            Tokens tokens = Tokens.of(outcome.usage());
            if (tokens.total() > 0) {
                budgetStore.recordUsage(scenario, tokens.input(), tokens.output());
                return;
            }
            var configured = aiProperties.budget().resolve(scenario);
            if (configured != null) {
                budgetStore.recordUsage(scenario, configured.maxTokensPerCall(), 0);
            }
        } catch (Exception e) {
            log.debug("action=ai_budget_record_failed, scenario={}, reason={}", scope.budgetScenario(), e.getMessage());
        }
    }

    /** 异常体常含完整 HTTP 响应（供应商动辄几 KB），按列宽截断保住可读前缀。 */
    private static @Nullable String truncate(@Nullable String errorMsg) {
        if (errorMsg == null || errorMsg.length() <= ERROR_MSG_MAX) {
            return errorMsg;
        }
        return errorMsg.substring(0, ERROR_MSG_MAX);
    }

    private static String md5(String input) {
        return DigestUtils.md5DigestAsHex(input.getBytes(StandardCharsets.UTF_8));
    }

    /** 供应商回报的 token 数（缺失字段记 0）—— 调用日志与预算记账共用这一套 null 安全取法。 */
    private record Tokens(int input, int output) {

        static Tokens of(@Nullable Usage usage) {
            return new Tokens(
                    orZero(usage == null ? null : usage.getPromptTokens()),
                    orZero(usage == null ? null : usage.getCompletionTokens()));
        }

        int total() {
            return input + output;
        }

        private static int orZero(@Nullable Integer value) {
            return value == null ? 0 : value;
        }
    }

    /** 调用结果 + 供应商回报的用量（未回报时为 null）—— 记账需要在结果之外拿到用量，故两者打成一个载体。 */
    public record CallOutcome<T>(T value, @Nullable Usage usage) {}

    /**
     * 流式响应里用量只出现在末尾分片，且可能整段缺失 —— 取最后一个真正回报了 token 的分片。
     * <p>
     * 「有壳无值」（metadata 在、token 全 null 或全 0）不算回报：部分供应商在每个分片都带一个空
     * usage 占位，不滤掉的话末尾那个空壳会覆盖掉前面真实回报的用量，记账直接退化成 0。
     */
    static final class UsageAccumulator implements Consumer<ChatResponse> {

        private @Nullable Usage latest;

        @Override
        public void accept(@Nullable ChatResponse response) {
            Usage usage = usageOf(response);
            if (hasReportedTokens(usage)) {
                latest = usage;
            }
        }

        @Nullable
        Usage result() {
            return latest;
        }
    }

    /** chat 响应的用量；无响应或无 metadata 时为 null。 */
    static @Nullable Usage usageOf(@Nullable ChatResponse response) {
        return response == null || response.getMetadata() == null
                ? null
                : response.getMetadata().getUsage();
    }

    /** embedding 响应的用量挂在与 chat 同一套 metadata 结构上，取法一致。 */
    static @Nullable Usage usageOf(@Nullable EmbeddingResponse response) {
        return response == null || response.getMetadata() == null
                ? null
                : response.getMetadata().getUsage();
    }

    /** 「供应商真的回报了 token」的判据 —— null、空壳、全 0 都算没回报，下游据此退化为按场景上限估算。 */
    private static boolean hasReportedTokens(@Nullable Usage usage) {
        if (usage == null) {
            return false;
        }
        return isPositive(usage.getPromptTokens()) || isPositive(usage.getCompletionTokens());
    }

    private static boolean isPositive(@Nullable Integer value) {
        return value != null && value > 0;
    }
}
