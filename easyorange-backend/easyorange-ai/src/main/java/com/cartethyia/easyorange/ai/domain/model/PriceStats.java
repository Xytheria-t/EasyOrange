package com.cartethyia.easyorange.ai.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 已召回资产的价格统计 — market_price_stats 工具的观察物（件数 / 均价 / 价格区间，零 LLM 调用）。
 * {@link #count()} 是<b>有效价格件数</b>而非入参件数：面议（price 为 null）与非正价不进统计，也不能算进均价分母。
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
        if (hits == null) {
            return Optional.empty();
        }
        List<BigDecimal> prices = hits.stream()
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

    /** 与观察文本里的 N 同源（面议 / 非正价不计）。 */
    public int count() {
        return count;
    }

    /** 保留入参精度；比较用 compareTo，展示走 {@link #observation()}。 */
    public BigDecimal min() {
        return min;
    }

    public BigDecimal max() {
        return max;
    }

    /** HALF_UP 取整到整数——不要改精度：观察文案把均价当整数报给模型，拆成小数会诱导它自己再算一遍。 */
    public BigDecimal avg() {
        return avg;
    }

    public String observation() {
        String range = min.compareTo(max) == 0
                ? "均为 %s".formatted(MoneyFormat.render(min))
                : "价格区间 %s-%s".formatted(MoneyFormat.render(min), MoneyFormat.render(max));
        return "当前 %d 件在售，均价 %s，%s".formatted(count, MoneyFormat.render(avg), range);
    }
}
