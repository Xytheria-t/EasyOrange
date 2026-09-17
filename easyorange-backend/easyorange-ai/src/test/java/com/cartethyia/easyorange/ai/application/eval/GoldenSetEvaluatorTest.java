package com.cartethyia.easyorange.ai.application.eval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.ai.application.dto.ChatAnswer;
import com.cartethyia.easyorange.ai.application.dto.ChatRequest;
import com.cartethyia.easyorange.ai.application.service.AiChatService;
import com.cartethyia.easyorange.ai.application.service.AiJudge;
import com.cartethyia.easyorange.ai.application.service.KnowledgeRetrievalService;
import com.cartethyia.easyorange.ai.domain.model.GenerationReport;
import com.cartethyia.easyorange.ai.domain.model.GoldenSet;
import com.cartethyia.easyorange.ai.domain.model.GoldenSetCase;
import com.cartethyia.easyorange.ai.domain.model.KnowledgeHit;
import com.cartethyia.easyorange.ai.domain.model.RetrievalReport;
import com.cartethyia.easyorange.ai.domain.port.RetrievalMetricPort;
import com.cartethyia.easyorange.common.idgen.IdGenerator;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("GoldenSetEvaluator (金标准回归) -> 测试")
class GoldenSetEvaluatorTest {

    @Mock
    private GoldenSetLoader loader;

    @Mock
    private AiChatService chatService;

    @Mock
    private AiJudge aiJudge;

    @Mock
    private KnowledgeRetrievalService retrievalService;

    @Mock
    private RetrievalMetricPort metricRecorder;

    @Mock
    private IdGenerator idGenerator;

    private GoldenSetEvaluator evaluator;

    private void setUp() {
        evaluator = new GoldenSetEvaluator(loader, chatService, aiJudge, retrievalService, metricRecorder, idGenerator);
    }

    @Test
    @DisplayName("生成回归：有参考的对照打分，无参考的四维打分，聚合平均分")
    void evaluateGeneration_averages() {
        setUp();
        when(loader.load())
                .thenReturn(new GoldenSet(List.of(
                        new GoldenSetCase("chat-001", "chat", "问题A", "参考A", List.of()),
                        new GoldenSetCase("chat-002", "chat", "问题B", null, List.of()))));
        when(chatService.answer(any(ChatRequest.class))).thenReturn(new ChatAnswer("回答", List.of(), "eval-x"));
        when(aiJudge.judgeAgainstReference("参考A", "回答")).thenReturn(Optional.of(new AiJudge.Judgement(4, "ok")));
        when(aiJudge.judge("chat", "回答")).thenReturn(Optional.of(new AiJudge.Judgement(3, "ok")));

        GenerationReport report = evaluator.evaluateGeneration();

        assertThat(report.totalCases()).isEqualTo(2);
        assertThat(report.judgedCases()).isEqualTo(2);
        assertThat(report.avgScore()).isEqualTo(3.5);
    }

    @Test
    @DisplayName("生成回归：模型异常跳过该条，不拖垮整批")
    void evaluateGeneration_skipsFailedCase() {
        setUp();
        when(loader.load())
                .thenReturn(new GoldenSet(List.of(new GoldenSetCase("chat-001", "chat", "问题A", null, List.of()))));
        when(chatService.answer(any(ChatRequest.class))).thenThrow(new RuntimeException("model down"));

        GenerationReport report = evaluator.evaluateGeneration();

        assertThat(report.judgedCases()).isZero();
        assertThat(report.avgScore()).isZero();
    }

    @Test
    @DisplayName("检索回归：命中记 hit@5 + MRR 分量，逐条落库")
    void evaluateRetrieval_metrics() {
        setUp();
        when(loader.load())
                .thenReturn(new GoldenSet(List.of(
                        new GoldenSetCase("retr-001", "retrieval", "退款", null, List.of("kb-0002")),
                        new GoldenSetCase("retr-002", "retrieval", "禁售", null, List.of("kb-0005")))));
        when(retrievalService.search("退款", 5)).thenReturn(List.of(new KnowledgeHit("kb-0002", "退款规则", "内容", 0.9)));
        when(retrievalService.search("禁售", 5)).thenReturn(List.of(new KnowledgeHit("kb-0001", "交易流程", "内容", 0.9)));
        when(idGenerator.generateId()).thenReturn("run-1");

        RetrievalReport report = evaluator.evaluateRetrieval();

        assertThat(report.totalCases()).isEqualTo(2);
        assertThat(report.hitCases()).isEqualTo(1);
        assertThat(report.hitRateAt5()).isEqualTo(0.5);
        assertThat(report.mrr()).isEqualTo(0.5);
        verify(metricRecorder).record("run-1", "retr-001", "退款", "kb-0002", true, 1.0);
        verify(metricRecorder).record("run-1", "retr-002", "禁售", "kb-0005", false, 0.0);
    }

    @Test
    @DisplayName("分流按 scope：chat 用例不进检索分母，retrieval 用例不进 Judge")
    void evaluate_scopesAreDisjoint() {
        setUp();
        when(loader.load())
                .thenReturn(new GoldenSet(List.of(
                        new GoldenSetCase("chat-001", "chat", "问题A", "参考A", List.of("kb-0001")),
                        new GoldenSetCase("retr-001", "retrieval", "退款", null, List.of("kb-0002")))));
        when(chatService.answer(any(ChatRequest.class))).thenReturn(new ChatAnswer("回答", List.of(), "eval-x"));
        when(aiJudge.judgeAgainstReference("参考A", "回答")).thenReturn(Optional.of(new AiJudge.Judgement(5, "ok")));
        when(retrievalService.search("退款", 5)).thenReturn(List.of(new KnowledgeHit("kb-0002", "退款规则", "内容", 1.0)));
        when(idGenerator.generateId()).thenReturn("run-1");

        GenerationReport generation = evaluator.evaluateGeneration();
        RetrievalReport retrieval = evaluator.evaluateRetrieval();

        // 带 gold_doc_ids 的 chat 用例只算生成、不算检索（此前按 gold_doc_ids 过滤会让它进检索分母）
        assertThat(generation.totalCases()).isEqualTo(1);
        assertThat(retrieval.totalCases()).isEqualTo(1);
        assertThat(retrieval.hitRateAt5()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("MRR 分量：首个命中位置倒数，未命中为 0")
    void reciprocalRank() {
        assertThat(GoldenSetEvaluator.computeReciprocalRank(List.of("kb-1"), List.of("x", "kb-1")))
                .isEqualTo(0.5);
        assertThat(GoldenSetEvaluator.computeReciprocalRank(List.of("kb-1"), List.of("kb-1", "x")))
                .isEqualTo(1.0);
        assertThat(GoldenSetEvaluator.computeReciprocalRank(List.of("kb-1"), List.of("x", "y")))
                .isZero();
        assertThat(GoldenSetEvaluator.computeReciprocalRank(List.of("kb-1", "kb-2"), List.of("kb-2")))
                .isEqualTo(1.0);
    }

    @Test
    @DisplayName("EvalGate：分数低于基线 - 容忍度 -> 失败（卡 build）")
    void evalGate() {
        assertThat(EvalGate.check(4.1, 4.0, 0.3).passed()).isTrue();
        assertThat(EvalGate.check(3.8, 4.0, 0.3).passed()).isTrue();
        assertThat(EvalGate.check(3.6, 4.0, 0.3).passed()).isFalse();
        assertThat(EvalGate.check(3.6, 4.0, 0.3).delta()).isCloseTo(-0.4, org.assertj.core.data.Offset.offset(1e-9));
    }

    @Test
    @DisplayName("EvalGate 覆盖率：评审大面积失败判失败（防「幸存者平均」蒙过分数门禁）")
    void evalGate_coverage() {
        // 30 条只评出 1 条、那 1 条拿了 5 分：均分漂亮但样本已不代表质量
        assertThat(EvalGate.checkCoverage("chat", 1, 30, EvalGate.DEFAULT_MIN_COVERAGE)
                        .passed())
                .isFalse();
        assertThat(EvalGate.checkCoverage("chat", 24, 30, EvalGate.DEFAULT_MIN_COVERAGE)
                        .passed())
                .isTrue();
        assertThat(EvalGate.checkCoverage("chat", 0, 0, EvalGate.DEFAULT_MIN_COVERAGE)
                        .passed())
                .isFalse();
    }
}
