package com.cartethyia.easyorange.ai.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.cartethyia.easyorange.ai.domain.model.AssetComparison.Dimension;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AssetComparison (资产逐维比对) -> 测试")
class AssetComparisonTest {

    @Test
    @DisplayName("少于 2 件可比较的候选返回 empty")
    void of_fewerThanTwoCandidates() {
        assertThat(AssetComparison.of(null)).isEmpty();
        assertThat(AssetComparison.of(List.of())).isEmpty();
        assertThat(AssetComparison.of(List.of(asset("p1", "3400", null, null, "ONLINE"))))
                .isEmpty();
        // null 元素先被剔除，剔除后不足 2 件仍走同一判据
        assertThat(AssetComparison.of(Arrays.asList(asset("p1", "3400", null, null, "ONLINE"), null)))
                .isEmpty();
    }

    @Test
    @DisplayName("价格维：胜出方是最低价，note 带金额")
    void of_pricePicksCheapest() {
        var comparison = AssetComparison.of(List.of(
                        asset("p1", "4200", null, null, "ONLINE"),
                        asset("p2", "3400", null, null, "ONLINE"),
                        asset("p3", "5000", null, null, "ONLINE")))
                .orElseThrow();

        assertThat(comparison.dimensions()).extracting(Dimension::name).containsExactly("价格");
        assertThat(comparison.dimensions().getFirst().winnerProductId()).isEqualTo("p2");
        assertThat(comparison.dimensions().getFirst().note()).isEqualTo("p2 最低 ¥3400");
        assertThat(comparison.observation()).isEqualTo("对比 3 件：p1、p2、p3；价格：p2 最低 ¥3400");
    }

    @Test
    @DisplayName("价格维：面议（price=null）不当最低价胜出，只在 note 里点明")
    void of_negotiablePriceNeverWins() {
        var comparison = AssetComparison.of(List.of(
                        asset("p1", null, null, null, "ONLINE"),
                        asset("p2", "3400", null, null, "ONLINE"),
                        asset("p3", "5000", null, null, "ONLINE")))
                .orElseThrow();

        assertThat(comparison.dimensions().getFirst().winnerProductId()).isEqualTo("p2");
        assertThat(comparison.dimensions().getFirst().note()).isEqualTo("p2 最低 ¥3400，p1 面议未参与比价");
    }

    @Test
    @DisplayName("价格维：只有一件报价时不判定（单件不构成「更便宜」）")
    void of_singlePricedCandidateSkipsPrice() {
        var comparison = AssetComparison.of(
                        List.of(asset("p1", null, null, null, "ONLINE"), asset("p2", "3400", null, null, "ONLINE")))
                .orElseThrow();

        assertThat(comparison.dimensions()).isEmpty();
        assertThat(comparison.observation())
                .isEqualTo("对比 2 件：p1、p2；价格 / 成色 / 地区 / 在售状态 均无可判定差异，不构成选择依据");
    }

    @Test
    @DisplayName("价格维：有效价格全等时该维不进结果")
    void of_equalPricesSkipPrice() {
        var comparison = AssetComparison.of(
                        List.of(asset("p1", "3400", null, null, "ONLINE"), asset("p2", "3400", null, null, "ONLINE")))
                .orElseThrow();

        assertThat(comparison.dimensions()).isEmpty();
    }

    @Test
    @DisplayName("成色维：胜出方是档位最新的一件")
    void of_conditionPicksNewest() {
        var comparison = AssetComparison.of(List.of(
                        asset("p1", null, "轻微使用痕迹", null, "ONLINE"),
                        asset("p2", null, "全新", null, "ONLINE")))
                .orElseThrow();

        assertThat(comparison.dimensions()).extracting(Dimension::name).containsExactly("成色");
        assertThat(comparison.dimensions().getFirst().winnerProductId()).isEqualTo("p2");
        assertThat(comparison.dimensions().getFirst().note()).isEqualTo("p2 成色最好（全新）");
    }

    @Test
    @DisplayName("成色维：档位并列最优时无胜出方")
    void of_conditionTieHasNoWinner() {
        var comparison = AssetComparison.of(List.of(
                        asset("p1", null, "全新", null, "ONLINE"),
                        asset("p2", null, "全新", null, "ONLINE"),
                        asset("p3", null, "明显使用痕迹", null, "ONLINE")))
                .orElseThrow();

        assertThat(comparison.dimensions().getFirst().winnerProductId()).isNull();
        assertThat(comparison.dimensions().getFirst().note()).isEqualTo("p1、p2 成色并列最好（全新）");
    }

    @Test
    @DisplayName("成色维：码表外的自由文本不猜档位，可判定不足 2 件则该维不进结果")
    void of_conditionFreeTextOutOfTableNotGuessed() {
        var comparison = AssetComparison.of(List.of(
                        asset("p1", null, "九五新，无磕碰", null, "ONLINE"),
                        asset("p2", null, "几乎全新", null, "ONLINE")))
                .orElseThrow();

        assertThat(comparison.dimensions()).isEmpty();
    }

    @Test
    @DisplayName("成色维：不可判定的候选退出比较，但要在 note 里点明")
    void of_conditionUnjudgedCandidateMentioned() {
        var comparison = AssetComparison.of(List.of(
                        asset("p1", null, "九五新，无磕碰", null, "ONLINE"),
                        asset("p2", null, "全新", null, "ONLINE"),
                        asset("p3", null, "轻微使用痕迹", null, "ONLINE")))
                .orElseThrow();

        assertThat(comparison.dimensions().getFirst().winnerProductId()).isEqualTo("p2");
        assertThat(comparison.dimensions().getFirst().note()).isEqualTo("p2 成色最好（全新），p1 成色不可判定");
    }

    @Test
    @DisplayName("地区维：地区不同时只列事实，无胜出方")
    void of_locationDiffersHasNoWinner() {
        var comparison = AssetComparison.of(List.of(
                        asset("p1", null, null, "杭州", "ONLINE"), asset("p2", null, null, "深圳", "ONLINE")))
                .orElseThrow();

        assertThat(comparison.dimensions().getFirst().name()).isEqualTo("地区");
        assertThat(comparison.dimensions().getFirst().winnerProductId()).isNull();
        assertThat(comparison.dimensions().getFirst().note()).isEqualTo("p1 杭州、p2 深圳，无客观优劣");
    }

    @Test
    @DisplayName("地区维：地区相同（或都未标注）时该维不进结果")
    void of_sameLocationSkipsDimension() {
        assertThat(AssetComparison.of(List.of(
                                        asset("p1", null, null, "杭州", "ONLINE"),
                                        asset("p2", null, null, "杭州", "ONLINE")))
                        .orElseThrow()
                        .dimensions())
                .isEmpty();
        assertThat(AssetComparison.of(
                                List.of(asset("p1", null, null, null, "ONLINE"), asset("p2", null, null, " ", "ONLINE")))
                        .orElseThrow()
                        .dimensions())
                .isEmpty();
    }

    @Test
    @DisplayName("在售状态维：非在售必须显式点出，胜出方是唯一仍在售的那件")
    void of_statusFlagsNonOnSale() {
        var comparison = AssetComparison.of(
                        List.of(asset("p1", null, null, null, "ONLINE"), asset("p2", null, null, null, "SOLD")))
                .orElseThrow();

        assertThat(comparison.dimensions().getFirst().name()).isEqualTo("在售状态");
        assertThat(comparison.dimensions().getFirst().winnerProductId()).isEqualTo("p1");
        assertThat(comparison.dimensions().getFirst().note()).isEqualTo("p2（SOLD）非在售，不建议推荐，仅 p1 仍在售");
    }

    @Test
    @DisplayName("在售状态维：全部非在售时无胜出方，仍然点出来")
    void of_statusAllNonOnSale() {
        var comparison = AssetComparison.of(
                        List.of(asset("p1", null, null, null, "SOLD"), asset("p2", null, null, null, "OFFLINE")))
                .orElseThrow();

        assertThat(comparison.dimensions().getFirst().winnerProductId()).isNull();
        assertThat(comparison.dimensions().getFirst().note()).isEqualTo("p1（SOLD）、p2（OFFLINE）非在售，不建议推荐");
    }

    @Test
    @DisplayName("在售状态维：码表外的状态取值按未知处理，不臆断成「非在售」")
    void of_statusOutOfCodeTableNotJudged() {
        var comparison = AssetComparison.of(
                        List.of(asset("p1", null, null, null, "ONLINE"), asset("p2", null, null, null, "上架")))
                .orElseThrow();

        assertThat(comparison.dimensions()).isEmpty();
    }

    @Test
    @DisplayName("全部字段为 null 时无维度进结果，观察文本仍可用")
    void of_allNullableFieldsNull() {
        var comparison = AssetComparison.of(List.of(
                        new AssetDetail("p1", "标题一", null, null, null, null, null, null, null),
                        new AssetDetail("p2", "标题二", null, null, null, null, null, null, null)))
                .orElseThrow();

        assertThat(comparison.dimensions()).isEmpty();
        assertThat(comparison.observation())
                .isEqualTo("对比 2 件：p1、p2；价格 / 成色 / 地区 / 在售状态 均无可判定差异，不构成选择依据");
    }

    @Test
    @DisplayName("维度顺序固定，观察文本是可进 prompt 的纯文本")
    void of_dimensionOrderAndPlainTextObservation() {
        var comparison = AssetComparison.of(List.of(
                        asset("p1", "4200", "轻微使用痕迹", "杭州", "ONLINE"),
                        asset("p2", "3400", "全新", "深圳", "ONLINE"),
                        asset("p3", "5000", "明显使用痕迹", "深圳", "SOLD")))
                .orElseThrow();

        assertThat(comparison.dimensions())
                .extracting(Dimension::name)
                .containsExactly("价格", "成色", "地区", "在售状态");
        assertThat(comparison.observation())
                .isEqualTo("对比 3 件：p1、p2、p3；价格：p2 最低 ¥3400；成色：p2 成色最好（全新）；"
                        + "地区：p1 杭州、p2 深圳、p3 深圳，无客观优劣；在售状态：p3（SOLD）非在售，不建议推荐")
                .doesNotContain("\"");
    }

    @Test
    @DisplayName("超过 MAX_CANDIDATES 不截断也不抛异常——截断是调用方的责任")
    void of_moreThanMaxCandidatesPassesThrough() {
        var comparison = AssetComparison.of(List.of(
                        asset("p1", "3400", null, null, "ONLINE"),
                        asset("p2", "3500", null, null, "ONLINE"),
                        asset("p3", "3600", null, null, "ONLINE"),
                        asset("p4", "3700", null, null, "ONLINE"),
                        asset("p5", "3800", null, null, "ONLINE")))
                .orElseThrow();

        assertThat(comparison.observation()).startsWith("对比 5 件：");
        assertThat(comparison.dimensions().getFirst().winnerProductId()).isEqualTo("p1");
    }

    /** 只填本用例关心的字段，其余留 null —— AssetDetail 的价格 / 成色 / 地区 / 状态都可能是空值。 */
    private static AssetDetail asset(String id, String price, String conditionDesc, String location, String status) {
        return new AssetDetail(
                id,
                id + " 标题",
                null,
                price == null ? null : new BigDecimal(price),
                null,
                conditionDesc,
                location,
                null,
                status);
    }
}
