package com.cartethyia.easyorange.ai.adapter.inbound.job;

import com.cartethyia.easyorange.ai.config.AiProperties;
import com.cartethyia.easyorange.ai.eval.GoldenSetEvaluator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * RAG 检索指标回归任务 — 定时跑金标准集检索用例（hit@5 / MRR）并落库。
 * <p>
 * 与 {@link AiEvalScheduler}（生成质量 Judge）互补：检索层指标不需要 LLM 生成，
 * 只依赖 embedding + ES，默认关闭（easyorange.ai.eval.retrieval-enabled=false）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RetrievalEvalScheduler {

    private final GoldenSetEvaluator evaluator;
    private final AiProperties aiProperties;

    @Scheduled(cron = "${easyorange.ai.eval.retrieval-cron:0 15 3 * * ?}")
    public void evaluateRetrievalMetrics() {
        if (!aiProperties.getEval().isRetrievalEnabled()) {
            return;
        }
        evaluator.evaluateRetrieval();
    }
}
