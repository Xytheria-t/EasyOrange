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
 * 多轮对话上下文的 token 级裁剪 — 轮数窗口（存储侧按轮数裁）之上的第二道预算：历史注入 prompt 前
 * 按估算 token 裁到预算内。从最新一条向前累计，保留放得进预算的<b>最新连续窗口</b>（丢旧不丢新、
 * 不跳条保留——对话连续性比多留一条旧消息值钱）；单条超预算时仍保最新一条，永不返回空历史。
 * 检索片段 / 资产详情等固定块不在裁剪范围——由 topK 与分块上限天然约束，会无界膨胀的只有对话原文。
 * <p>
 * 不做 LLM 摘要压缩（有意取舍）：步数上限与轮数窗口都不大时压缩收益小，而每轮摘要多一次模型调用、
 * 直接翻倍延迟与成本——裁剪 + 轮数窗口已封顶。
 */
@Component
public class ChatContextTrimmer {

    /** 裁剪动作 — {@link #TRIM_METRIC} 的封闭 tag 集（构造期按全集注册），tag 值是时序契约：改枚举名不改 tag。 */
    private enum TrimAction {
        WITHIN("within"),
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

    /** 裁剪要注入 prompt 的历史（预算内全量时原样返回）。 */
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
