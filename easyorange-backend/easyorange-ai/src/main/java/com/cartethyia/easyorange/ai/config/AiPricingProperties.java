package com.cartethyia.easyorange.ai.config;

import java.math.BigDecimal;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 模型单价表 — 成本报表的货币化依据（元/百万 token，按模型名对齐 {@code eo_ai_call_log.model}）。
 * <p>
 * 价格是「排序与汇报」口径不是对账口径：供应商价目随活动/版本变动，官方调价后改这里即可，
 * 缺某个模型的条目时该行只出 token 不出钱（估算价混进成本报表比缺数据更危险，与「只认真实回报
 * 用量」同一口径）。
 */
@ConfigurationProperties(prefix = "easyorange.ai.pricing")
public record AiPricingProperties(Map<String, ModelPrice> models) {

    public AiPricingProperties {
        if (models == null) {
            models = Map.of();
        }
    }

    /** 单价 — 输入/输出分开计（输出单价通常是输入的数倍，混成单一均价会把「长回答更贵」算反）。 */
    public record ModelPrice(BigDecimal inputPerMillion, BigDecimal outputPerMillion) {}
}
