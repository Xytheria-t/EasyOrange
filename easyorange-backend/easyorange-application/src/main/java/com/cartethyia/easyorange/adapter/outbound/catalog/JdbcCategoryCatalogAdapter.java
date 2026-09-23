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
 * 与 {@code JdbcAiListingAdoptionAdapter} 同一分工（读需求在 ai，跨表读取在组合根）。
 */
@Component
@RequiredArgsConstructor
public class JdbcCategoryCatalogAdapter implements CategoryCatalogPort {

    /**
     * 只取启用中的<b>一级</b>类目：发布页下拉就是一级清单（{@code getCategoriesByLevel(1)}，6 项），
     * 喂二级叶子名（如「耳机音箱」）模型选得再准前端也匹配不到 ID、类别回填必空；
     * 禁用类目同理——被选中也落不到表单上。
     */
    private static final String SQL = """
            SELECT name FROM eo_category
            WHERE status = 1 AND del_flag = 0 AND level = 1
            ORDER BY sort_order, name
            """;

    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<String> listAvailableCategoryNames() {
        return jdbcTemplate.queryForList(SQL, String.class);
    }
}
