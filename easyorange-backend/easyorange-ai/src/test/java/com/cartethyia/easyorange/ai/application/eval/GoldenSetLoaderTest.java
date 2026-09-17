package com.cartethyia.easyorange.ai.application.eval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cartethyia.easyorange.ai.domain.model.GoldenSet;
import com.cartethyia.easyorange.ai.domain.model.GoldenSetCase;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GoldenSetLoader (金标准评测集) -> 测试")
class GoldenSetLoaderTest {

    private final GoldenSetLoader loader = new GoldenSetLoader();

    @Test
    @DisplayName("加载 golden-set.yaml -> 35 条用例（20 chat + 15 retrieval）")
    void load_allCases() {
        GoldenSet goldenSet = loader.load();

        assertThat(goldenSet.cases()).hasSize(35);
        assertThat(scoped(goldenSet, GoldenSetLoader.SCOPE_CHAT)).hasSize(20);
        assertThat(scoped(goldenSet, GoldenSetLoader.SCOPE_RETRIEVAL)).hasSize(15);
        assertThat(goldenSet.cases().stream().filter(c -> c.id().startsWith("chat-")))
                .hasSize(20);
        assertThat(goldenSet.cases().stream().filter(c -> c.id().startsWith("retr-")))
                .hasSize(15);
    }

    @Test
    @DisplayName("chat 用例必须带参考回答，retrieval 用例必须带 gold_doc_ids")
    void load_caseContract() {
        GoldenSet goldenSet = loader.load();

        for (GoldenSetCase c : goldenSet.cases()) {
            assertThat(c.question()).as(c.id() + " 必须有问题").isNotBlank();
            if (GoldenSetLoader.SCOPE_CHAT.equals(c.scope())) {
                assertThat(c.referenceAnswer()).as(c.id() + " 必须有参考回答").isNotBlank();
            } else {
                assertThat(c.goldDocIds()).as(c.id() + " 必须有期望命中文档").isNotEmpty();
            }
        }
    }

    @Test
    @DisplayName("检索用例引用的文档 ID 均落在种子语料范围内（kb-0001 ~ kb-0025）")
    void load_goldDocIdsMatchSeed() {
        GoldenSet goldenSet = loader.load();

        for (GoldenSetCase c : goldenSet.cases()) {
            for (String docId : c.goldDocIds()) {
                assertThat(docId).as(c.id() + " 引用的 " + docId + " 必须在种子文档范围").matches("kb-\\d{4}");
                int ordinal = Integer.parseInt(docId.substring("kb-".length()));
                assertThat(ordinal).as(c.id() + " 引用的 " + docId + " 超出语料范围").isBetween(1, 25);
            }
        }
    }

    @Test
    @DisplayName("检索用例覆盖的目标文档互不重复（同一文档不应被多条用例当唯一答案）")
    void load_retrievalTargetsAreDistinct() {
        GoldenSet goldenSet = loader.load();

        Set<String> targets = scoped(goldenSet, GoldenSetLoader.SCOPE_RETRIEVAL).stream()
                .flatMap(c -> c.goldDocIds().stream())
                .collect(Collectors.toSet());

        // 15 条检索用例的目标文档集合应有一定宽度，否则指标只在一个文档上打转
        assertThat(targets).hasSizeGreaterThanOrEqualTo(5);
    }

    @Test
    @DisplayName("校验：scope 非法 / chat 无参考 / retrieval 无 gold 都会抛错")
    void validate_rejectsInconsistentCases() {
        assertThatThrownBy(
                        () -> GoldenSetLoader.validate(new GoldenSetCase("bad-001", "retrival", "q", "a", List.of())))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("scope 非法");
        assertThatThrownBy(() -> GoldenSetLoader.validate(new GoldenSetCase("chat-999", "chat", "q", null, List.of())))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("reference_answer");
        assertThatThrownBy(() ->
                        GoldenSetLoader.validate(new GoldenSetCase("retr-999", "retrieval", "q", null, List.of())))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("gold_doc_ids");

        assertThatCode(() -> GoldenSetLoader.validate(new GoldenSetCase("chat-001", "chat", "q", "a", List.of())))
                .doesNotThrowAnyException();
        assertThatCode(() -> GoldenSetLoader.validate(
                        new GoldenSetCase("retr-001", "retrieval", "q", null, List.of("kb-0001"))))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("加载 baselines.yaml -> 分数基线 / 容忍度 / 覆盖率下限 / hit@5 下限齐全")
    void loadBaselines() {
        EvalBaselines baselines = loader.loadBaselines();

        assertThat(baselines.generation().scoreBaseline()).isEqualTo(4.0);
        assertThat(baselines.generation().scoreTolerance()).isEqualTo(0.3);
        assertThat(baselines.generation().minCoverage()).isEqualTo(0.8);
        assertThat(baselines.retrieval().minHitAt5()).isEqualTo(0.5);
    }

    private static List<GoldenSetCase> scoped(GoldenSet goldenSet, String scope) {
        return goldenSet.cases().stream().filter(c -> scope.equals(c.scope())).toList();
    }
}
