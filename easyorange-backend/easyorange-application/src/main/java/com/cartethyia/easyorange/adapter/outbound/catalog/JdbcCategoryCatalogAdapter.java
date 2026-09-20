package com.cartethyia.easyorange.adapter.outbound.catalog;

import com.cartethyia.easyorange.ai.domain.port.CategoryCatalogPort;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 平台类目清单读取适配器 — 实现 {@link CategoryCatalogPort}。
 * <p>
 * 放在 application 模块：类目表属 product 模块，ai 模块只声明端口、不碰别人的表 ——
 * 与 {@code JdbcAiPricingAdoptionAdapter} 同一分工（读需求在 ai，跨表读取在组合根）。
 */
@Component
@RequiredArgsConstructor
public class JdbcCategoryCatalogAdapter implements CategoryCatalogPort {

    /** 只取启用中的类目：禁用类目即便被模型选中，前端也匹配不到 ID，等于白填。 */
    private static final String SQL = """
            SELECT name FROM eo_category
            WHERE status = 1 AND del_flag = 0
            ORDER BY sort_order, name
            """;

    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<String> listAvailableCategoryNames() {
        return jdbcTemplate.queryForList(SQL, String.class);
    }
}
