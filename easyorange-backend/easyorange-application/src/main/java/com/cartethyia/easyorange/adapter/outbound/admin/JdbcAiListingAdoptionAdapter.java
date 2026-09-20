package com.cartethyia.easyorange.adapter.outbound.admin;

import com.cartethyia.easyorange.ai.domain.model.AiListingAdoptionReport;
import com.cartethyia.easyorange.ai.domain.port.AiListingAdoptionPort;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * AI 建议采纳率读取适配器 — 实现 {@link AiListingAdoptionPort}。
 * <p>
 * 放在 application 模块：这是管理端只读统计，需要跨模块读 product 侧的表，
 * 属组合根的职责范围（与 {@code JdbcCategoryCatalogAdapter} 同层）；ai 模块只声明端口，不碰商品表。
 * <p>
 * 采纳判定写在读的一侧而不是落库时算好：快照存的是原文，口径（算哪些字段、怎么算一致）随查询走，
 * 以后收紧判定不必回填历史数据。
 * <p>
 * 只统计真的给出过建议的商品（{@code ai_suggestion} 非空）：没走拍照识别的商品进分母，
 * 会把采纳率稀释成一个没有意义的数。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JdbcAiListingAdoptionAdapter implements AiListingAdoptionPort {

    /** 先把两边的取值归一化成同一形状，判定表达式才不至于重复五遍；{@code <=>} 是 NULL 安全等号。 */
    private static final String ADOPTION_SQL = """
            WITH listing AS (SELECT p.name                                                  AS name,
                                    p.price                                                 AS price,
                                    p.condition_level                                       AS condition_level,
                                    p.location                                              AS location,
                                    d.description                                           AS description,
                                    c.name                                                  AS category_name,
                                    p.ai_suggestion ->> '$.title'                           AS ai_title,
                                    p.ai_suggestion ->> '$.description'                     AS ai_description,
                                    CAST(p.ai_suggestion ->> '$.price' AS DECIMAL(10, 2))   AS ai_price,
                                    p.ai_suggestion ->> '$.categoryName'                    AS ai_category_name,
                                    p.ai_suggestion ->> '$.conditionLevel'                  AS ai_condition_level,
                                    p.ai_suggestion ->> '$.location'                        AS ai_location
                             FROM eo_product p
                                      LEFT JOIN eo_product_detail d ON d.product_id = p.id AND d.del_flag = 0
                                      LEFT JOIN eo_category c ON c.id = p.category_id AND c.del_flag = 0
                             WHERE p.del_flag = 0
                               AND p.ai_suggestion IS NOT NULL)
            SELECT COUNT(*)                                                                     AS samples,
                   COALESCE(SUM(name <=> ai_title), 0)                                          AS title_adopted,
                   COALESCE(SUM(COALESCE(description, '') = COALESCE(ai_description, '')), 0)    AS description_adopted,
                   COALESCE(SUM(price <=> ai_price), 0)                                         AS price_adopted,
                   COALESCE(SUM(category_name <=> ai_category_name), 0)                         AS category_adopted,
                   COALESCE(SUM(condition_level <=> ai_condition_level), 0)                     AS condition_adopted,
                   COALESCE(SUM(COALESCE(location, '') = COALESCE(ai_location, '')), 0)         AS location_adopted,
                   COALESCE(SUM((name <=> ai_title)
                                    AND (COALESCE(description, '') = COALESCE(ai_description, ''))
                                    AND (price <=> ai_price)
                                    AND (category_name <=> ai_category_name)
                                    AND (condition_level <=> ai_condition_level)
                                    AND (COALESCE(location, '') = COALESCE(ai_location, ''))), 0) AS fully_adopted,
                   COALESCE(SUM(ABS(price - ai_price) <= ai_price * 0.10), 0)                    AS within10,
                   COALESCE(SUM(ABS(price - ai_price) > ai_price * 0.30), 0)                     AS beyond30,
                   COALESCE(AVG(ABS(price - ai_price) / NULLIF(ai_price, 0) * 100), 0)           AS avg_deviation
            FROM listing
            """;

    private final JdbcTemplate jdbcTemplate;

    @Override
    public AiListingAdoptionReport report() {
        return jdbcTemplate.queryForObject(ADOPTION_SQL, (rs, rowNum) -> {
            long samples = rs.getLong("samples");
            long fullyAdopted = rs.getLong("fully_adopted");
            return new AiListingAdoptionReport(
                    samples,
                    fullyAdopted,
                    rate(fullyAdopted, samples),
                    List.of(
                            field("title", rs.getLong("title_adopted"), samples),
                            field("description", rs.getLong("description_adopted"), samples),
                            field("price", rs.getLong("price_adopted"), samples),
                            field("categoryName", rs.getLong("category_adopted"), samples),
                            field("conditionLevel", rs.getLong("condition_adopted"), samples),
                            field("location", rs.getLong("location_adopted"), samples)),
                    rs.getLong("within10"),
                    rs.getLong("beyond30"),
                    rs.getDouble("avg_deviation"));
        });
    }

    private static AiListingAdoptionReport.FieldAdoption field(String field, long adopted, long samples) {
        return new AiListingAdoptionReport.FieldAdoption(field, adopted, rate(adopted, samples));
    }

    private static double rate(long part, long total) {
        return total > 0 ? (double) part / total : 0d;
    }
}
