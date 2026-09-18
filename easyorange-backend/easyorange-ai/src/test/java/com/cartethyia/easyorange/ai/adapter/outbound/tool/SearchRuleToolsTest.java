package com.cartethyia.easyorange.ai.adapter.outbound.tool;

import static org.assertj.core.api.Assertions.assertThat;

import com.cartethyia.easyorange.product.application.query.readmodel.ProductReadModel;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * 搜索增强的两路规则工具测试。
 * <p>
 * 这两路原先由 LLM 生成（价格文本进、行情话术出；关键词进、追问列表出），输出不可断言、还会随
 * 供应商超时整路缺席。改成从 {@link SearchToolContext} 直接算之后，输出是纯函数结果，可以逐字断言
 * ——「改规则」这件事的收益之一就是这层可测性。
 */
@DisplayName("搜索增强规则工具 -> 测试")
class SearchRuleToolsTest {

    private final MarketAnalysisTool marketAnalysis = new MarketAnalysisTool();
    private final QuestionSuggestionTool questionSuggestion = new QuestionSuggestionTool();

    private static ProductReadModel product(BigDecimal price, String categoryName) {
        return ProductReadModel.builder()
                .id("p-" + price)
                .title("商品")
                .price(price)
                .categoryName(categoryName)
                .build();
    }

    private static SearchToolContext context(String keyword, ProductReadModel... products) {
        return new SearchToolContext(keyword, List.of(products));
    }

    @Nested
    @DisplayName("市场分析（规则价格统计）")
    class MarketAnalysis {

        @Test
        @DisplayName("多件在售 -> 件数 + 均价 + 价格区间")
        void multiplePrices() throws Exception {
            String result = marketAnalysis
                    .run(context(
                            "找笔记本",
                            product(BigDecimal.valueOf(1000), null),
                            product(BigDecimal.valueOf(2000), null),
                            product(BigDecimal.valueOf(3000), null)))
                    .get();

            assertThat(result).isEqualTo("当前 3 件在售，均价 ¥2000，价格区间 ¥1000-¥3000");
        }

        @Test
        @DisplayName("均价四舍五入到整数（1000 与 1001 -> 1001，不出现小数）")
        void averageRoundsHalfUp() throws Exception {
            String result = marketAnalysis
                    .run(context(
                            "找东西", product(BigDecimal.valueOf(1000), null), product(BigDecimal.valueOf(1001), null)))
                    .get();

            assertThat(result).isEqualTo("当前 2 件在售，均价 ¥1001，价格区间 ¥1000-¥1001");
        }

        @Test
        @DisplayName("只有一件 -> 区间退化为「均为」")
        void singlePrice() throws Exception {
            String result = marketAnalysis
                    .run(context("找东西", product(BigDecimal.valueOf(4200), null)))
                    .get();

            assertThat(result).isEqualTo("当前 1 件在售，均价 ¥4200，均为 ¥4200");
        }

        @Test
        @DisplayName("价格全为空或非正 -> 返回 null（管道「本轮无结果」语义）")
        void noUsablePrice() throws Exception {
            String result = marketAnalysis
                    .run(context("找东西", product(null, null), product(BigDecimal.ZERO, null)))
                    .get();

            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("建议问题（规则派生）")
    class QuestionSuggestion {

        @Test
        @DisplayName("关键词 + 分类 + 价格下限 -> 派生三条追问")
        void derivesThreeQuestions() throws Exception {
            List<String> result = questionSuggestion
                    .run(context("找便宜手机", product(BigDecimal.valueOf(1500), "数码")))
                    .get();

            assertThat(result).containsExactly("「找便宜手机」里哪件性价比最高？", "数码 类还有哪些选择？", "预算 ¥1500 以内能买到什么？");
        }

        @Test
        @DisplayName("长关键词截断到 12 字（自然语言搜索可能几十字，原样回显会很长）")
        void truncatesLongKeyword() throws Exception {
            List<String> result = questionSuggestion
                    .run(context("帮我找一个适合编程的笔记本电脑", product(BigDecimal.valueOf(4200), null)))
                    .get();

            assertThat(result).first().isEqualTo("「帮我找一个适合编程的笔记」里哪件性价比最高？");
        }

        @Test
        @DisplayName("价格下限向下取整到整数档（千元以上取百，千元以下取十）")
        void floorsBudgetToRoundNumber() throws Exception {
            assertThat(questionSuggestion
                            .run(context("找东西", product(BigDecimal.valueOf(856), null)))
                            .get())
                    .contains("预算 ¥850 以内能买到什么？");

            assertThat(questionSuggestion
                            .run(context("找东西", product(BigDecimal.valueOf(4200), null)))
                            .get())
                    .contains("预算 ¥4200 以内能买到什么？");
        }

        @Test
        @DisplayName("无关键词且无分类 -> 空列表，不产出空问题占位")
        void noKeywordNoCategory() throws Exception {
            List<String> result =
                    questionSuggestion.run(context("   ", product(null, null))).get();

            assertThat(result).isEmpty();
        }
    }
}
