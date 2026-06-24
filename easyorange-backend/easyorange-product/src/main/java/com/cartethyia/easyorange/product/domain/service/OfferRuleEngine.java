package com.cartethyia.easyorange.product.domain.service;

import com.cartethyia.easyorange.common.domain.Money;
import com.cartethyia.easyorange.product.domain.valueobject.OfferDecision;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * 议价规则引擎 — 纯领域服务，无框架依赖。
 * <p>
 * 根据买家出价、底价、当前价格和降价阶梯，决策接受/还价/拒绝。
 * 决策逻辑：
 * <ul>
 *   <li>出价 &gt;= 底价 → ACCEPT</li>
 *   <li>出价 &gt;= 底价 × 0.9 → COUNTER（还价金额 = 底价 × 0.95）</li>
 *   <li>出价 &lt; 底价 × 0.9 → REJECT</li>
 *   <li>如果已处于底价阶梯（level &gt;= 3）且出价 &gt;= 底价 × 0.85 → COUNTER（还价金额 = 底价，最后一次机会）</li>
 * </ul>
 */
public class OfferRuleEngine {

    private static final BigDecimal COUNTER_THRESHOLD = new BigDecimal("0.9");
    private static final BigDecimal COUNTER_RATIO = new BigDecimal("0.95");
    private static final BigDecimal FLOOR_LAST_CHANCE_THRESHOLD = new BigDecimal("0.85");

    /**
     * 评估买家出价，返回决策结果。
     *
     * @param offerPrice       买家出价
     * @param floorPrice       底价
     * @param currentPrice     商品当前价格
     * @param currentPriceLevel 当前降价阶梯等级
     * @return 议价决策
     */
    public OfferDecision evaluate(Money offerPrice, Money floorPrice, Money currentPrice, int currentPriceLevel) {
        Objects.requireNonNull(offerPrice, "offerPrice must not be null");
        Objects.requireNonNull(floorPrice, "floorPrice must not be null");
        // offer >= floorPrice → 接受
        if (isGreaterThanOrEqual(offerPrice, floorPrice)) {
            return new OfferDecision(
                    OfferDecision.DecisionType.ACCEPT,
                    offerPrice.value(),
                    null,
                    "出价达到底价，直接成交"
            );
        }

        // offer >= floorPrice × 0.9 → 还价
        Money counterThreshold = Money.of(floorPrice.value().multiply(COUNTER_THRESHOLD));
        if (isGreaterThanOrEqual(offerPrice, counterThreshold)) {
            Money counterPrice = Money.of(floorPrice.value().multiply(COUNTER_RATIO));
            return new OfferDecision(
                    OfferDecision.DecisionType.COUNTER,
                    null,
                    counterPrice.value(),
                    "出价接近底价，还价至底价95%"
            );
        }

        // 已到底价阶梯（level >= 3）且 offer >= floorPrice × 0.85 → 最后一次还价机会
        if (currentPriceLevel >= 3) {
            Money lastChanceThreshold = Money.of(floorPrice.value().multiply(FLOOR_LAST_CHANCE_THRESHOLD));
            if (isGreaterThanOrEqual(offerPrice, lastChanceThreshold)) {
                return new OfferDecision(
                        OfferDecision.DecisionType.COUNTER,
                        null,
                        floorPrice.value(),
                        "商品已到底价，最后一次还价至底价"
                );
            }
        }

        // 拒绝
        return new OfferDecision(
                OfferDecision.DecisionType.REJECT,
                null,
                null,
                "出价过低"
        );
    }

    /**
     * 判断 a >= b（Money 没有提供 isGreaterThanOrEqual 方法，使用 compareTo 组合）。
     */
    private static boolean isGreaterThanOrEqual(Money a, Money b) {
        return a.compareTo(b) >= 0;
    }
}
