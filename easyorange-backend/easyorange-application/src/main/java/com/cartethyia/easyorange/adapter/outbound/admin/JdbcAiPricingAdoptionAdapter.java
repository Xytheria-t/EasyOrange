package com.cartethyia.easyorange.adapter.outbound.admin;

import com.cartethyia.easyorange.ai.domain.model.PricingAdoptionReport;
import com.cartethyia.easyorange.ai.domain.port.AiPricingAdoptionPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * AI 建议价采纳率读取适配器 — 实现 {@link AiPricingAdoptionPort}。
 * <p>
 * 放在 application 模块：这是管理端只读统计，需要跨模块读 product 侧的表，
 * 属组合根的职责范围（与 {@code AdminProductAuditAdapter} 同层）；ai 模块只声明端口，不碰商品表。
 * <p>
 * 只统计真的给出过建议的商品（{@code ai_suggested_price} 非空）：没走拍照识别的商品进分母，
 * 会把采纳率稀释成一个没有意义的数。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JdbcAiPricingAdoptionAdapter implements AiPricingAdoptionPort {

    private static final String ADOPTION_SQL = """
            SELECT COUNT(*)                                                                     AS samples,
                   COALESCE(SUM(CASE WHEN price = ai_suggested_price THEN 1 ELSE 0 END), 0)       AS adopted,
                   COALESCE(SUM(CASE WHEN ABS(price - ai_suggested_price) <= ai_suggested_price * 0.10
                                     THEN 1 ELSE 0 END), 0)                                     AS within10,
                   COALESCE(SUM(CASE WHEN ABS(price - ai_suggested_price) > ai_suggested_price * 0.30
                                     THEN 1 ELSE 0 END), 0)                                     AS beyond30,
                   COALESCE(AVG(ABS(price - ai_suggested_price) / ai_suggested_price * 100), 0) AS avg_deviation
            FROM eo_product
            WHERE del_flag = 0
              AND ai_suggested_price IS NOT NULL
              AND ai_suggested_price > 0
            """;

    private final JdbcTemplate jdbcTemplate;

    @Override
    public PricingAdoptionReport report() {
        return jdbcTemplate.queryForObject(ADOPTION_SQL, (rs, rowNum) -> {
            long samples = rs.getLong("samples");
            long adopted = rs.getLong("adopted");
            return new PricingAdoptionReport(
                    samples,
                    adopted,
                    samples > 0 ? (double) adopted / samples : 0d,
                    rs.getLong("within10"),
                    rs.getLong("beyond30"),
                    rs.getDouble("avg_deviation"));
        });
    }
}
