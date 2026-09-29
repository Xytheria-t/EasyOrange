package com.cartethyia.easyorange.ai.application.eval;

import com.cartethyia.easyorange.ai.application.chat.AiChatAppService;
import com.cartethyia.easyorange.ai.application.chat.ToolCallLoop;
import com.cartethyia.easyorange.ai.application.dto.ChatAnswer;
import com.cartethyia.easyorange.ai.application.dto.ChatRequest;
import com.cartethyia.easyorange.ai.application.retrieval.KnowledgeRetrievalAppService;
import com.cartethyia.easyorange.ai.domain.model.GenerationReport;
import com.cartethyia.easyorange.ai.domain.model.KnowledgeHit;
import com.cartethyia.easyorange.ai.domain.model.RetrievalReport;
import com.cartethyia.easyorange.ai.domain.model.RoutingReport;
import com.cartethyia.easyorange.ai.domain.port.RetrievalMetricPort;
import com.cartethyia.easyorange.common.idgen.IdGenerator;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 金标准集回归评估器 — 三条评估线：生成质量（LLM-as-Judge）、检索质量（hit@5 / MRR）、路由质量
 * （期望工具路径命中率）。三条线读同一份用例集，各按自己的口径取子集。
 * <p>
 * 生成线对每个 chat 用例调 {@link AiChatAppService#answer}（forceFresh 跳过缓存；显式传机器主体
 * {@link ToolCallLoop#MACHINE_SUBJECT}，画像不落库，评估不被历史偏好污染）对照参考回答打分取均值；
 * 检索线逐条采样落 eo_retrieval_metric；路由线对标了 {@code expected_tools} 的用例跑一次工具循环。
 * <p>
 * <b>路由线只跑循环不跑生成</b>：它量的是选路，生成那步属生成分。两条线按 scope 字段分流，不用「有没有
 * gold_doc_ids」这类派生特征，那会让带 gold 的生成用例同时被算进检索分母。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GoldenSetEvaluator {

    private static final int RETRIEVAL_TOP_K = 5;

    private final GoldenSetLoader loader;
    private final AiChatAppService chatService;
    private final AiJudge aiJudge;
    private final KnowledgeRetrievalAppService retrievalService;
    private final RetrievalMetricPort metricRecorder;
    private final IdGenerator idGenerator;
    private final ToolCallLoop toolCallLoop;

    /** 生成质量回归：全部 chat 用例 Judge 打分，返回平均分。 */
    public GenerationReport evaluateGeneration() {
        var cases = loader.load().cases().stream()
                .filter(c -> GoldenSetLoader.SCOPE_CHAT.equals(c.scope()))
                .toList();
        var scores = new ArrayList<CaseScore>();
        for (var c : cases) {
            try {
                ChatAnswer answer = chatService.answer(
                        new ChatRequest(c.question(), "eval-" + c.id(), true), ToolCallLoop.MACHINE_SUBJECT);
                // 只有对照参考回答这一条评分路径：chat 用例必带 reference_answer，加载期已强校验
                aiJudge.judgeAgainstReference(c.referenceAnswer(), answer.answer())
                        .ifPresent(j -> scores.add(new CaseScore(c.id(), j.score())));
            } catch (Exception e) {
                log.warn("golden case {} generation eval failed: {}", c.id(), e.getMessage());
            }
        }
        // 均值分母是「成功评分的用例数」而非 cases.size()：判分失败的用例不参与平均，
        // 它们的流失由 GenerationReport 的 judgedCount 对比 sampleCount 单独暴露 ——
        // 拿全量当分母会把「模型答不出」算成「模型答得差」，两者在门禁上要分开看。
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
     * <p>
     * 分母是全量 {@code cases.size()}，与生成分的「成功评分数」口径相反：检索是纯本地计算，
     * 没有「判分失败」这一态，每条用例都出得了 hit 与 rr，所以漏掉的只能是真实未命中。
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
     * 跑失败 / 降级（{@code decision_failed}、{@code step_limit}）算未命中且仍计入分母 ——
     * 决策失败正是「模型没选出该选的工具」的一种形态，把它排除掉等于把最该看的失败藏起来。
     */
    public RoutingReport evaluateRouting() {
        var cases = loader.load().cases().stream()
                .filter(c -> GoldenSetLoader.SCOPE_CHAT.equals(c.scope())
                        && !c.expectedTools().isEmpty())
                .toList();
        int correct = 0;
        for (var c : cases) {
            List<String> actual;
            try {
                actual = toolCallLoop
                        .run(new ToolCallLoop.Input(
                                c.question(),
                                "eval-" + c.id(),
                                ToolCallLoop.MACHINE_SUBJECT,
                                List.of(),
                                List.of(),
                                null))
                        .toolPath();
            } catch (Exception e) {
                log.warn("golden case {} routing eval failed: {}", c.id(), e.getMessage());
                actual = List.of();
            }
            if (routeMatches(c.expectedTools(), actual)) {
                correct++;
            } else {
                log.info(
                        "golden case {} route miss: expected={}, actual={}",
                        c.id(),
                        String.join(",", c.expectedTools()),
                        actual.isEmpty() ? "(无)" : String.join(",", actual));
            }
        }
        double accuracy = cases.isEmpty() ? 0 : correct * 1.0 / cases.size();
        log.info(
                "Golden set routing eval: hit {}/{} cases, accuracy = {}",
                correct,
                cases.size(),
                "%.2f%%".formatted(accuracy * 100));
        return new RoutingReport(cases.size(), correct, accuracy);
    }

    /**
     * 路由命中判据：期望工具是否都出现在实际路径里（不比顺序与次数）。
     * 只看「有没有」而不是「先不先」：模型为稳妥多查一步仍然拿到了该查的信息，判错会让指标惩罚正确行为。
     */
    static boolean routeMatches(List<String> expectedTools, List<String> actualToolPath) {
        return expectedTools.stream().allMatch(actualToolPath::contains);
    }

    /** MRR 分量：第一个命中的期望文档在第 i 位（1 起）得 1/i，未命中为 0。 */
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
