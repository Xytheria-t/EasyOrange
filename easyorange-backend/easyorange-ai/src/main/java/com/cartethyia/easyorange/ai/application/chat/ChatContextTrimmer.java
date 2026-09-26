package com.cartethyia.easyorange.ai.application.chat;

import com.cartethyia.easyorange.ai.config.AiProperties;
import com.cartethyia.easyorange.ai.domain.model.ChatTurn;
import com.cartethyia.easyorange.ai.domain.model.TokenEstimator;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 多轮对话上下文的 token 级裁剪 — 轮数窗口（{@code ChatSessionStore} 按轮数裁存储）之上的第二道预算：
 * 历史注入 prompt 前按估算 token 裁到预算内。
 * <p>
 * 从最新一条向前累计估算 token，保留放得进预算的<b>最新连续窗口</b>（丢旧不丢新、不跳条保留——对话连续性
 * 比多留一条旧消息值钱）；单条超预算时仍保最新一条，永不返回空历史。检索片段 / 资产详情等固定块不在裁剪
 * 范围——它们由 topK 与分块上限天然约束，会无界膨胀的只有用户与助手的原文。
 * <p>
 * 不做 LLM 摘要压缩（有意取舍）：步数上限（{@code max-steps}，默认 7）与轮数窗口（{@code historyLimit}，
 * 默认 6 轮）都不大时压缩收益小，而每轮摘要多一次模型调用、直接翻倍延迟与成本——裁剪 + 轮数窗口已封顶。
 * <p>
 * 指标（成本治理口径，meter 构造期一次注册、每请求记一次数据点）：{@code easyorange.ai.chat.context.tokens}
 * = 注入历史的估算 token（p50/p95）；{@code easyorange.ai.chat.context.trim{action}}（trimmed / within）
 * → 触发率 = trimmed ÷ 总数。预算 <=0 关闭裁剪时不记数据点（计数恒为 0，与「未发生的降级原因 count=0」同语义）。
 */
@Component
public class ChatContextTrimmer {

    /**
     * 裁剪动作 — {@link #TRIM_METRIC} 的封闭 tag 集，构造期按全集注册计数器（新增动作自动带上）。
     * tag 值是时序契约：改枚举名不改 tag。
     */
    private enum TrimAction {
        /** 历史全量在预算内，原样注入。 */
        WITHIN("within"),
        /** 触发裁剪，注入最新连续窗口。 */
        TRIMMED("trimmed");

        private final String tag;

        TrimAction(String tag) {
            this.tag = tag;
        }

        String tag() {
            return tag;
        }
    }

    private static final String TRIM_METRIC = "easyorange.ai.chat.context.trim";
    private static final String TOKENS_METRIC = "easyorange.ai.chat.context.tokens";

    private final AiProperties aiProperties;

    /** 裁剪动作计数 —— 按 action 全集注册，within 与 trimmed 共用同一组计数器。 */
    private final Map<TrimAction, Counter> trimCounters;

    private final DistributionSummary tokensSummary;

    public ChatContextTrimmer(AiProperties aiProperties, MeterRegistry meterRegistry) {
        this.aiProperties = aiProperties;
        this.trimCounters = new EnumMap<>(TrimAction.class);
        for (TrimAction action : TrimAction.values()) {
            trimCounters.put(action, meterRegistry.counter(TRIM_METRIC, "action", action.tag()));
        }
        this.tokensSummary = DistributionSummary.builder(TOKENS_METRIC)
                .description("注入 prompt 的历史上下文估算 token 数（裁剪后）")
                .publishPercentiles(0.5, 0.95)
                .register(meterRegistry);
    }

    /**
     * 裁剪要注入 prompt 的历史。
     *
     * @return 预算内放得下的最新连续窗口（预算内全量时原样返回）
     */
    public List<ChatTurn> trim(List<ChatTurn> history) {
        int budget = aiProperties.chat().maxHistoryTokens();
        // 预算 <=0 视为关闭 token 裁剪（退回纯轮数窗口），不记数据点
        if (budget <= 0) {
            return history;
        }
        int[] tokens = new int[history.size()];
        int total = 0;
        for (int i = 0; i < tokens.length; i++) {
            tokens[i] = TokenEstimator.estimate(history.get(i).content());
            total += tokens[i];
        }
        if (total <= budget) {
            record(TrimAction.WITHIN, total);
            return history;
        }
        // 最新一条无条件保留（超长单条也注入，由 maxTokensPerCall 兜住生成侧），其余从最新向前累计
        int keptFrom = tokens.length - 1;
        int kept = tokens[keptFrom];
        for (int i = keptFrom - 1; i >= 0; i--) {
            if (kept + tokens[i] > budget) {
                break;
            }
            kept += tokens[i];
            keptFrom = i;
        }
        record(TrimAction.TRIMMED, kept);
        return List.copyOf(history.subList(keptFrom, history.size()));
    }

    /** 每请求一次的两条口径同处记录 —— 裁剪触发率与注入 token 分布都取自这里。 */
    private void record(TrimAction action, int tokens) {
        trimCounters.get(action).increment();
        tokensSummary.record(tokens);
    }
}
