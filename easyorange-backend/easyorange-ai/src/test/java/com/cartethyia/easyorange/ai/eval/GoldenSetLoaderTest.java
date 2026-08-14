package com.cartethyia.easyorange.ai.eval;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GoldenSetLoader (金标准评测集) -> 测试")
class GoldenSetLoaderTest {

    private final GoldenSetLoader loader = new GoldenSetLoader();

    @Test
    @DisplayName("加载 golden-set.yaml -> 30 条用例（20 chat + 10 检索）")
    void load_allCases() {
        GoldenSet goldenSet = loader.load();

        assertThat(goldenSet.cases()).hasSize(30);
        assertThat(goldenSet.cases().stream().filter(c -> "chat".equals(c.scope())))
                .hasSize(30);
        assertThat(goldenSet.cases().stream()
                        .filter(c -> c.goldDocIds() != null && !c.goldDocIds().isEmpty()))
                .hasSize(20);
        assertThat(goldenSet.cases().stream().filter(c -> c.id().startsWith("chat-")))
                .hasSize(20);
        assertThat(goldenSet.cases().stream().filter(c -> c.id().startsWith("retr-")))
                .hasSize(10);
    }

    @Test
    @DisplayName("chat 用例必须带参考回答，检索用例必须带 gold_doc_ids")
    void load_caseContract() {
        GoldenSet goldenSet = loader.load();

        for (GoldenSetCase c : goldenSet.cases()) {
            assertThat(c.question()).as(c.id() + " 必须有问题").isNotBlank();
            if (c.id().startsWith("chat-")) {
                assertThat(c.referenceAnswer()).as(c.id() + " 必须有参考回答").isNotBlank();
            }
            if (c.id().startsWith("retr-")) {
                assertThat(c.goldDocIds()).as(c.id() + " 必须有期望命中文档").isNotEmpty();
            }
        }
    }

    @Test
    @DisplayName("检索用例引用的文档 ID 均在种子文档范围内（kb-0001 ~ kb-0005）")
    void load_goldDocIdsMatchSeed() {
        GoldenSet goldenSet = loader.load();

        for (GoldenSetCase c : goldenSet.cases()) {
            for (String docId : c.goldDocIds()) {
                assertThat(docId).as(c.id() + " 引用的 " + docId + " 必须在种子文档范围").matches("kb-000[1-5]");
            }
        }
    }

    @Test
    @DisplayName("加载 baselines.yaml -> chat 基线 4.0")
    void loadBaselines() {
        assertThat(loader.loadBaselines()).containsEntry("chat", 4.0);
    }
}
