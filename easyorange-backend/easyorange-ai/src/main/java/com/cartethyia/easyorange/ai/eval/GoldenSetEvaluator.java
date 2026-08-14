package com.cartethyia.easyorange.ai.eval;

import com.cartethyia.easyorange.ai.adapter.outbound.persistence.RetrievalMetricRecorder;
import com.cartethyia.easyorange.ai.dto.ChatAnswer;
import com.cartethyia.easyorange.ai.dto.ChatRequest;
import com.cartethyia.easyorange.ai.knowledge.KnowledgeHit;
import com.cartethyia.easyorange.ai.service.AiChatService;
import com.cartethyia.easyorange.ai.service.AiJudge;
import com.cartethyia.easyorange.ai.service.KnowledgeRetrievalService;
import com.cartethyia.easyorange.common.idgen.IdGenerator;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 金标准集回归评估器 — 两条评估线：
 * <ul>
 *   <li><b>生成质量</b>（LLM-as-Judge）：对每个 chat 用例调 {@link AiChatService#answer}（forceFresh 跳过缓存），
 *       有参考回答的对照参考打分，无参考的走四维通用打分，聚合 avg score。</li>
 *   <li><b>检索质量</b>（hit@5 / MRR）：对带 gold_doc_ids 的用例跑知识库检索，算命中率与平均倒数排名，
 *       逐条采样落 eo_retrieval_metric（回答「RAG 检索层好不好」的量化数据）。</li>
 * </ul>
 * 供定时任务（RetrievalEvalScheduler / 每日回归）与 CI 门禁（GoldenSetRegressionIT）复用。
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
    private final RetrievalMetricRecorder metricRecorder;
    private final IdGenerator idGenerator;

    /**
     * 生成质量回归：全部 chat 用例 Judge 打分，返回平均分。
     */
    public GenerationReport evaluateGeneration() {
        var cases = loader.load().cases().stream()
                .filter(c -> "chat".equals(c.scope()))
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
                "Golden set generation eval: judged {}/{} cases, avg score = {:.2f}", scores.size(), cases.size(), avg);
        return new GenerationReport(cases.size(), scores.size(), avg);
    }

    /**
     * 检索质量回归：带 gold_doc_ids 的用例跑检索，计算 hit@5 / MRR 并逐条落库。
     */
    public RetrievalReport evaluateRetrieval() {
        var cases = loader.load().cases().stream()
                .filter(c -> !c.goldDocIds().isEmpty())
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
                "Golden set retrieval eval: hit {}/{} cases, hit@5 = {:.2%}, MRR = {:.4f}",
                hits, cases.size(), hitRate, mrr);
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
