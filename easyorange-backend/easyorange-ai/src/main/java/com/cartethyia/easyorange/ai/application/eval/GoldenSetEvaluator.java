package com.cartethyia.easyorange.ai.application.eval;

import com.cartethyia.easyorange.ai.application.chat.AiChatAppService;
import com.cartethyia.easyorange.ai.application.chat.ChatTools;
import com.cartethyia.easyorange.ai.application.chat.ToolCallLoop;
import com.cartethyia.easyorange.ai.application.dto.ChatAnswer;
import com.cartethyia.easyorange.ai.application.dto.ChatRequest;
import com.cartethyia.easyorange.ai.application.listing.AutoListingAppService;
import com.cartethyia.easyorange.ai.application.listing.ListingLoopResult;
import com.cartethyia.easyorange.ai.application.retrieval.KnowledgeRetrievalAppService;
import com.cartethyia.easyorange.ai.application.support.RetrievalObservations;
import com.cartethyia.easyorange.ai.application.toolcall.ToolCallLoopOutcome;
import com.cartethyia.easyorange.ai.domain.model.ArmComparisonReport;
import com.cartethyia.easyorange.ai.domain.model.GenerationReport;
import com.cartethyia.easyorange.ai.domain.model.GoldenSetCase;
import com.cartethyia.easyorange.ai.domain.model.KnowledgeHit;
import com.cartethyia.easyorange.ai.domain.model.RetrievalReport;
import com.cartethyia.easyorange.ai.domain.model.RoutingReport;
import com.cartethyia.easyorange.ai.domain.port.RetrievalMetricPort;
import com.cartethyia.easyorange.common.idgen.IdGenerator;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * 金标准集回归评估器 — 三条评估线：生成质量（LLM-as-Judge）、检索质量（hit@5 / MRR）、路由质量
 * （期望工具路径命中率），外加一条按需跑的双臂对照（RAG 有效性）。三条线读同一份用例集，各按自己的口径取子集。
 * <p>
 * 生成线对每个 chat 用例调 {@link AiChatAppService#answer}（forceFresh 跳过缓存；显式传机器主体
 * {@link ToolCallLoop#MACHINE_SUBJECT} 与空会话，画像不落库、记忆不参与，不被历史污染）对照参考回答打分取均值；
 * 检索线逐条采样落 eo_retrieval_metric；路由线对标了 {@code expected_tools} 的用例跑一次工具循环。
 * <p>
 * <b>路由线只跑循环不跑生成</b>：它量的是选路，生成那步属生成分。两条线按 scope 字段分流，不用「有没有
 * gold_doc_ids」这类派生特征，那会让带 gold 的生成用例同时被算进检索分母。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GoldenSetEvaluator {

    /** 无检索臂的白名单：只留 finish —— 模型拿不到检索工具的 schema，凭自身知识作答。 */
    private static final Set<String> NO_RETRIEVAL_TOOLS = Set.of(ChatTools.TOOL_FINISH);

    private final GoldenSetLoader loader;
    private final AiChatAppService chatService;
    private final AiJudge aiJudge;
    private final KnowledgeRetrievalAppService retrievalService;
    private final RetrievalMetricPort metricRecorder;
    private final IdGenerator idGenerator;
    private final ToolCallLoop toolCallLoop;
    private final AutoListingAppService listingAppService;

    private List<GoldenSetCase> casesOf(String scope) {
        return loader.load().cases().stream()
                .filter(c -> scope.equals(c.scope()))
                .toList();
    }

    /** 生成质量回归：全部 chat 用例 Judge 打分，返回平均分。 */
    public GenerationReport evaluateGeneration() {
        var cases = casesOf(GoldenSetLoader.SCOPE_CHAT);
        int judged = 0;
        int scoreSum = 0;
        for (var c : cases) {
            Optional<AiJudge.Judgement> judgement = scoreCase(c, null);
            if (judgement.isPresent()) {
                judged++;
                scoreSum += judgement.get().score();
            }
        }
        // 均值分母是「成功评分的用例数」而非 cases.size()：判分失败的用例不参与平均，
        // 它们的流失由 GenerationReport 的 judgedCount 对比 sampleCount 单独暴露 ——
        // 拿全量当分母会把「模型答不出」算成「模型答得差」，两者在门禁上要分开看。
        double avg = ratio(scoreSum, judged);
        log.info(
                "Golden set generation eval: judged {}/{} cases, avg score = {}",
                judged,
                cases.size(),
                "%.2f".formatted(avg));
        return new GenerationReport(cases.size(), judged, avg);
    }

    /**
     * RAG 有效性对照（双臂）— 同批 chat 用例跑两臂，唯一变量是检索来源：
     * 有检索臂走现状全量工具面，无检索臂只挂 finish，模型拿不到任何检索工具的 schema。
     * <p>
     * 两臂同模型、同 Judge 提示词、同机器主体、同 forceFresh，在同一次调用里跑完；评分路径复用
     * {@link #scoreCase}（对照参考回答），因此与 {@link #evaluateGeneration} 是同一套口径。
     * <b>只报配对差分</b>（见 {@link ArmComparisonReport}），两臂绝对分不作对外质量证据。
     */
    public ArmComparisonReport evaluateRagArmComparison() {
        var cases = casesOf(GoldenSetLoader.SCOPE_CHAT);
        var diffs = new ArrayList<Double>();
        var missing = new ArrayList<String>();
        double retrievalSum = 0;
        double noRetrievalSum = 0;
        for (var c : cases) {
            var retrieval = scoreCase(c, null);
            var noRetrieval = scoreCase(c, NO_RETRIEVAL_TOOLS);
            if (retrieval.isEmpty() || noRetrieval.isEmpty()) {
                missing.add(c.id() + "(有检索=" + retrieval.isPresent() + ",无检索=" + noRetrieval.isPresent() + ")");
                continue;
            }
            double r = retrieval.get().score();
            double n = noRetrieval.get().score();
            retrievalSum += r;
            noRetrievalSum += n;
            diffs.add(r - n);
        }
        double meanDiff =
                diffs.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        int paired = diffs.size();
        var report = new ArmComparisonReport(
                paired,
                ratio(retrievalSum, paired),
                ratio(noRetrievalSum, paired),
                meanDiff,
                stdDev(diffs),
                List.copyOf(missing));
        log.info(
                "RAG arm comparison (paired n={}): retrieval={} noRetrieval={} meanDiff={} stdDev={} missing={}",
                report.pairedCases(),
                "%.2f".formatted(report.retrievalMean()),
                "%.2f".formatted(report.noRetrievalMean()),
                "%+.4f".formatted(report.meanDiff()),
                "%.4f".formatted(report.diffStdDev()),
                report.missingArms());
        return report;
    }

    /**
     * 跑一臂并评分 — {@code allowedTools} 为 null 即全量工具面（与生产路径同一条链路）。
     * <p>
     * 会话传 null：评估是无状态单轮，不读也不写会话记忆。挂 {@code eval-<caseId>} 当会话键会串历史 ——
     * 无检索臂读到有检索臂刚落的那轮（历史同时进决策与生成 prompt），配对差被稀释；跨轮重跑也会读到上轮答案。
     */
    private Optional<AiJudge.Judgement> scoreCase(GoldenSetCase c, @Nullable Set<String> allowedTools) {
        try {
            ChatAnswer answer = chatService.answer(
                    new ChatRequest(c.question(), null, true), ToolCallLoop.MACHINE_SUBJECT, allowedTools);
            // 只有对照参考回答这一条评分路径：chat 用例必带 reference_answer，加载期已强校验
            return aiJudge.judgeAgainstReference(c.referenceAnswer(), answer.answer());
        } catch (Exception e) {
            log.warn("golden case {} generation eval failed: {}", c.id(), e.getMessage());
            return Optional.empty();
        }
    }

    /** 占比 / 均值 —— 空样本报 0 而不是 NaN：门禁要看到的是「没有样本」这一个信号。 */
    private static double ratio(double numerator, int denominator) {
        return denominator == 0 ? 0 : numerator / denominator;
    }

    /** 配对差分的样本标准差 — 判「差分是信号还是单轮噪声」的量级依据（n-1 分母）。 */
    private static double stdDev(List<Double> diffs) {
        if (diffs.size() < 2) {
            return 0;
        }
        double mean = diffs.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double variance =
                diffs.stream().mapToDouble(d -> (d - mean) * (d - mean)).sum() / (diffs.size() - 1);
        return Math.sqrt(variance);
    }

    /**
     * 检索质量回归：对全部 retrieval 用例跑检索，计算 hit@5 / MRR 并逐条落库。
     * <p>
     * 分母是全量 {@code cases.size()}，与生成分的「成功评分数」口径相反：检索是纯本地计算，
     * 没有「判分失败」这一态，每条用例都出得了 hit 与 rr，所以漏掉的只能是真实未命中。
     */
    public RetrievalReport evaluateRetrieval() {
        var cases = casesOf(GoldenSetLoader.SCOPE_RETRIEVAL);
        String runId = idGenerator.generateId();
        int hits = 0;
        double mrrSum = 0;
        for (var c : cases) {
            // topK 取生产检索同一常量：hit@5 的 5 就是产品召回窗口，评估不另立一套
            List<KnowledgeHit> results = retrievalService.search(c.question(), RetrievalObservations.TOP_K);
            List<String> hitIds = results.stream().map(KnowledgeHit::docId).toList();
            double rr = computeReciprocalRank(c.goldDocIds(), hitIds);
            if (rr > 0) {
                hits++;
            }
            mrrSum += rr;
            metricRecorder.record(runId, c.id(), c.question(), String.join(",", c.goldDocIds()), rr > 0, rr);
        }
        double hitRate = ratio(hits, cases.size());
        double mrr = ratio(mrrSum, cases.size());
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
     * 只收 chat 用例：listing 多步用例走 {@link #evaluateListingRouting()}，两条链路难度不同型、阈值分开。
     */
    public RoutingReport evaluateRouting() {
        var cases = casesOf(GoldenSetLoader.SCOPE_CHAT).stream()
                .filter(c -> !c.expectedTools().isEmpty())
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
        double accuracy = ratio(correct, cases.size());
        log.info(
                "Golden set routing eval: hit {}/{} cases, accuracy = {}",
                correct,
                cases.size(),
                "%.2f%%".formatted(accuracy * 100));
        return new RoutingReport(cases.size(), correct, accuracy);
    }

    /**
     * 发布链路路由回归 — listing 用例跑发布工具循环（只跑循环不生成），判据与 chat 路由同一条。
     * 每条用例落一行 outcome / rounds / toolPath：多步链路「实际走了几步、路径长什么样」是发布链路
     * 多步化有没有意义的硬证据，评估报告从这些行聚合平均步数与 toolPath 分布（只打日志聚合，
     * RoutingReport 保持三字段口径不因评估扩列）。
     */
    public RoutingReport evaluateListingRouting() {
        var cases = casesOf(GoldenSetLoader.SCOPE_LISTING);
        int correct = 0;
        int roundsSum = 0;
        for (var c : cases) {
            List<String> actual;
            ToolCallLoopOutcome outcome;
            int rounds;
            try {
                ListingLoopResult result =
                        listingAppService.runToolLoop(null, c.question(), "eval-" + c.id(), null, null);
                actual = result.toolPath();
                outcome = result.outcome();
                rounds = result.rounds();
            } catch (Exception e) {
                log.warn("golden case {} listing routing eval failed: {}", c.id(), e.getMessage());
                actual = List.of();
                outcome = ToolCallLoopOutcome.ERROR;
                rounds = 0;
            }
            roundsSum += rounds;
            boolean hit = routeMatches(c.expectedTools(), actual);
            if (hit) {
                correct++;
            }
            log.info(
                    "golden case {} listing route: expected={}, actual={}, outcome={}, rounds={}",
                    c.id(),
                    String.join(",", c.expectedTools()),
                    actual.isEmpty() ? "(无)" : String.join(",", actual),
                    outcome.getTag(),
                    rounds);
        }
        double accuracy = ratio(correct, cases.size());
        double avgRounds = ratio(roundsSum, cases.size());
        log.info(
                "Golden set listing routing eval: hit {}/{} cases, accuracy = {}, avg rounds = {}",
                correct,
                cases.size(),
                "%.2f%%".formatted(accuracy * 100),
                "%.2f".formatted(avgRounds));
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
}
