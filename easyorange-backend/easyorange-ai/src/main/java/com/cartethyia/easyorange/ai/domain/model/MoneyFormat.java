package com.cartethyia.easyorange.ai.domain.model;

import java.math.BigDecimal;

/**
 * 金额展示 — 去掉小数尾巴（¥4200 而不是 ¥4200.00）；AssetComparison 与 PriceStats 的 observation 共用这一处。
 */
final class MoneyFormat {

    static String render(BigDecimal value) {
        return "¥" + value.stripTrailingZeros().toPlainString();
    }

    private MoneyFormat() {}
}
