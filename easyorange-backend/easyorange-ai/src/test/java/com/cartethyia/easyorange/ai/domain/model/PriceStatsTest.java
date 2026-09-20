package com.cartethyia.easyorange.ai.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PriceStats (召回资产价格统计) -> 测试")
class PriceStatsTest {

    @Test
    @DisplayName("无有效价格（空列表 / 全为 null / 全为非正）返回 empty")
    void of_noValidPrice() {
        assertThat(PriceStats.of(null)).isEmpty();
        assertThat(PriceStats.of(List.of())).isEmpty();
        assertThat(PriceStats.of(List.of(hit("p1", null), hit("p2", null)))).isEmpty();
        assertThat(PriceStats.of(List.of(hit("p1", "0"), hit("p2", "-5")))).isEmpty();
    }

    @Test
    @DisplayName("单件：min / max / avg 同值，观察文本说「均为」")
    void of_singlePricedHit() {
        var stats = PriceStats.of(List.of(hit("p1", "1999"))).orElseThrow();

        assertThat(stats.count()).isEqualTo(1);
        assertThat(stats.min()).isEqualByComparingTo("1999");
        assertThat(stats.max()).isEqualByComparingTo("1999");
        assertThat(stats.avg()).isEqualByComparingTo("1999");
        assertThat(stats.observation()).isEqualTo("当前 1 件在售，均价 ¥1999，均为 ¥1999");
    }

    @Test
    @DisplayName("多件：均价 HALF_UP 取整到整数，区间文案与 MarketAnalysisTool 一致")
    void of_matchesMarketAnalysisToolWording() {
        var stats = PriceStats.of(List.of(hit("p1", "3000"), hit("p2", "4200"), hit("p3", "5000")))
                .orElseThrow();

        // 12200 / 3 = 4066.67 -> HALF_UP -> 4067（scale 0，不带小数）
        assertThat(stats.count()).isEqualTo(3);
        assertThat(stats.min()).isEqualByComparingTo("3000");
        assertThat(stats.max()).isEqualByComparingTo("5000");
        assertThat(stats.avg()).isEqualByComparingTo("4067");
        assertThat(stats.avg().toPlainString()).isEqualTo("4067");
        assertThat(stats.observation()).isEqualTo("当前 3 件在售，均价 ¥4067，价格区间 ¥3000-¥5000");
    }

    @Test
    @DisplayName("均价 .5 向上取整")
    void of_avgRoundsHalfUp() {
        var stats = PriceStats.of(List.of(hit("p1", "3400"), hit("p2", "3401"))).orElseThrow();

        // 6801 / 2 = 3400.5 -> HALF_UP -> 3401
        assertThat(stats.avg()).isEqualByComparingTo("3401");
        assertThat(stats.observation()).contains("均价 ¥3401");
    }

    @Test
    @DisplayName("面议与非正价不进统计，也不算进均价的分母")
    void of_negotiableAndInvalidPricesExcluded() {
        var stats = PriceStats.of(List.of(hit("p1", null), hit("p2", "100"), hit("p3", "0"), hit("p4", "-5")))
                .orElseThrow();

        assertThat(stats.count()).isEqualTo(1);
        assertThat(stats.avg()).isEqualByComparingTo("100");
        assertThat(stats.observation()).isEqualTo("当前 1 件在售，均价 ¥100，均为 ¥100");
    }

    @Test
    @DisplayName("金额展示去掉小数尾巴（¥4200 而不是 ¥4200.00）")
    void of_stripsTrailingZeros() {
        var stats = PriceStats.of(List.of(hit("p1", "4200.00"), hit("p2", "4600.00")))
                .orElseThrow();

        assertThat(stats.observation())
                .isEqualTo("当前 2 件在售，均价 ¥4400，价格区间 ¥4200-¥4600")
                .doesNotContain("4200.00");
    }

    @Test
    @DisplayName("null 元素被跳过，不抛异常")
    void of_nullHitElementsIgnored() {
        var stats = PriceStats.of(Arrays.asList(hit("p1", "100"), null)).orElseThrow();

        assertThat(stats.count()).isEqualTo(1);
        assertThat(stats.observation()).isEqualTo("当前 1 件在售，均价 ¥100，均为 ¥100");
    }

    private static AssetHit hit(String id, String price) {
        return new AssetHit(id, id + " 标题", price == null ? null : new BigDecimal(price), null, null, 1.0);
    }
}
