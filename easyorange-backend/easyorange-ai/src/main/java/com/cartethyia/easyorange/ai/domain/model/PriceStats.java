package com.cartethyia.easyorange.ai.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 已召回资产的价格统计 — market_price_stats 工具的观察物：件数 / 均价 / 价格区间，零 LLM 调用。
 * <p>
 * 口径与搜索管道的 {@code MarketAnalysisTool.summarize} 完全一致（只统计有效价格、均价 HALF_UP 取整到整数、
 * 金额去小数尾巴）：那边吃 {@code ProductReadModel}、这边吃 {@link AssetHit}，两个注册表互不相干，
 * 抽公共类会让搜索管道反过来依赖 agent 侧类型，所以是照口径重写而不是复用。
 * <p>
 * {@link #count()} 是<b>有效价格件数</b>而非入参件数：面议（price 为 null）与非正价不进统计，
 * 也就不能算进均价的分母。
 */
public final class PriceStats {

    private final int count;
    private final BigDecimal min;
    private final BigDecimal max;
    private final BigDecimal avg;

    private PriceStats(int count, BigDecimal min, BigDecimal max, BigDecimal avg) {
        this.count = count;
        this.min = min;
        this.max = max;
        this.avg = avg;
    }

    /** 无有效价格（空列表 / 全为 null / 全为非正）时返回 empty。 */
    public static Optional<PriceStats> of(List<AssetHit> hits) {
        List<BigDecimal> prices = hits == null
                ? List.of()
                : hits.stream()
                        .filter(Objects::nonNull)
                        .map(AssetHit::price)
                        .filter(price -> price != null && price.signum() > 0)
                        .toList();
        if (prices.isEmpty()) {
            return Optional.empty();
        }
        BigDecimal min = prices.stream().min(BigDecimal::compareTo).orElseThrow();
        BigDecimal max = prices.stream().max(BigDecimal::compareTo).orElseThrow();
        BigDecimal avg = prices.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(prices.size()), 0, RoundingMode.HALF_UP);
        return Optional.of(new PriceStats(prices.size(), min, max, avg));
    }

    /** 参与统计的有效价格件数（面议 / 非正价不计，与观察文本里的 N 同源）。 */
    public int count() {
        return count;
    }

    /** 最低有效价，保留入参精度（比较用 compareTo，展示走 {@link #observation()}）。 */
    public BigDecimal min() {
        return min;
    }

    /** 最高有效价，保留入参精度（比较用 compareTo，展示走 {@link #observation()}）。 */
    public BigDecimal max() {
        return max;
    }

    /** 均价，HALF_UP 取整到整数——与 MarketAnalysisTool 口径一致，不要改精度。 */
    public BigDecimal avg() {
        return avg;
    }

    /** 「当前 N 件在售，均价 ¥X，价格区间 ¥A-¥B」；min 等于 max 时说「均为 ¥X」。 */
    public String observation() {
        String range = min.compareTo(max) == 0
                ? "均为 %s".formatted(money(min))
                : "价格区间 %s-%s".formatted(money(min), money(max));
        return "当前 %d 件在售，均价 %s，%s".formatted(count, money(avg), range);
    }

    /** 金额展示：去掉小数尾巴（¥4200 而不是 ¥4200.00）。 */
    private static String money(BigDecimal value) {
        return "¥" + value.stripTrailingZeros().toPlainString();
    }
}
