package com.cartethyia.easyorange.ai.application.eval;

import com.cartethyia.easyorange.ai.application.chat.AiChatService;
import com.cartethyia.easyorange.ai.application.dto.ChatAnswer;
import com.cartethyia.easyorange.ai.application.dto.ChatRequest;
import com.cartethyia.easyorange.ai.application.retrieval.KnowledgeRetrievalService;
import com.cartethyia.easyorange.ai.domain.model.GenerationReport;
import com.cartethyia.easyorange.ai.domain.model.KnowledgeHit;
import com.cartethyia.easyorange.ai.domain.model.RetrievalReport;
import com.cartethyia.easyorange.ai.domain.port.RetrievalMetricPort;
import com.cartethyia.easyorange.common.idgen.IdGenerator;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 金标准集回归评估器 — 两条评估线，按用例 scope 分流（{@code chat} / {@code retrieval}）：
 * <ul>
 *   <li><b>生成质量</b>（LLM-as-Judge）：对每个 chat 用例调 {@link AiChatService#answer}（forceFresh 跳过缓存），
 *       对照参考回答打分，聚合 avg score。</li>
 *   <li><b>检索质量</b>（hit@5 / MRR）：对每个 retrieval 用例跑知识库检索，算命中率与平均倒数排名，
 *       逐条采样落 eo_retrieval_metric（回答「RAG 检索层好不好」的量化数据）。</li>
 * </ul>
 * 供定时任务（RetrievalEvalScheduler / 每日回归）与 CI 门禁（GoldenSetRegressionIT）复用。
 * <p>
 * 分流依据是 scope 字段本身（{@link GoldenSetLoader} 加载时已校验），不再用「有没有 gold_doc_ids」
 * 这类派生特征判断 —— 那会让带 gold_doc_ids 的生成用例同时被算进检索分母。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GoldenSetEvaluator {

    private static final int RETRIEVAL_TOP_K = 5;

    private final GoldenSetLoader loader;
    private final AiChatService chatService;
    private final AiJudge aiJudge;
    private final KnowledgeRetrievalService retrievalService;
    private final RetrievalMetricPort metricRecorder;
    private final IdGenerator idGenerator;

    /**
     * 生成质量回归：全部 chat 用例 Judge 打分，返回平均分。
     */
    public GenerationReport evaluateGeneration() {
        var cases = loader.load().cases().stream()
                .filter(c -> GoldenSetLoader.SCOPE_CHAT.equals(c.scope()))
                .toList();
        var scores = new ArrayList<CaseScore>();
        for (var c : cases) {
            try {
                ChatAnswer answer = chatService.answer(new ChatRequest(c.question(), "eval-" + c.id(), true));
                Optional<AiJudge.Judgement> judgement =
                        (c.referenceAnswer() != null && !c.referenceAnswer().isBlank())
                                ? aiJudge.judgeAgainstReference(c.referenceAnswer(), answer.answer())
                                : aiJudge.judge("chat", answer.answer());
                judgement.ifPresent(j -> scores.add(new CaseScore(c.id(), j.score())));
            } catch (Exception e) {
                log.warn("golden case {} generation eval failed: {}", c.id(), e.getMessage());
            }
        }
        double avg = scores.isEmpty()
                ? 0
                : scores.stream().mapToInt(CaseScore::score).average().orElse(0);
        log.info(
                "Golden set generation eval: judged {}/{} cases, avg score = {}",
                scores.size(),
                cases.size(),
                "%.2f".formatted(avg));
        return new GenerationReport(cases.size(), scores.size(), avg);
    }

    /**
     * 检索质量回归：对全部 retrieval 用例跑检索，计算 hit@5 / MRR 并逐条落库。
     */
    public RetrievalReport evaluateRetrieval() {
        var cases = loader.load().cases().stream()
                .filter(c -> GoldenSetLoader.SCOPE_RETRIEVAL.equals(c.scope()))
                .toList();
        String runId = idGenerator.generateId();
        int hits = 0;
        double mrrSum = 0;
        for (var c : cases) {
            List<KnowledgeHit> results = retrievalService.search(c.question(), RETRIEVAL_TOP_K);
            List<String> hitIds = results.stream().map(KnowledgeHit::docId).toList();
            double rr = computeReciprocalRank(c.goldDocIds(), hitIds);
            if (rr > 0) {
                hits++;
            }
            mrrSum += rr;
            metricRecorder.record(runId, c.id(), c.question(), String.join(",", c.goldDocIds()), rr > 0, rr);
        }
        double hitRate = cases.isEmpty() ? 0 : hits * 1.0 / cases.size();
        double mrr = cases.isEmpty() ? 0 : mrrSum / cases.size();
        log.info(
                "Golden set retrieval eval: hit {}/{} cases, hit@5 = {}, MRR = {}",
                hits,
                cases.size(),
                "%.2f%%".formatted(hitRate * 100),
                "%.4f".formatted(mrr));
        return new RetrievalReport(cases.size(), hits, hitRate, mrr);
    }

    /**
     * MRR 分量：第一个命中的期望文档若在第 i 位（1 起），得 1/i，未命中为 0。
     */
    static double computeReciprocalRank(List<String> goldDocIds, List<String> hitIds) {
        for (int i = 0; i < hitIds.size(); i++) {
            if (goldDocIds.contains(hitIds.get(i))) {
                return 1.0 / (i + 1);
            }
        }
        return 0;
    }

    private record CaseScore(String caseId, int score) {}
}
