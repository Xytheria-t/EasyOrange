package com.cartethyia.easyorange.adapter.outbound.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.toolkit.ChainWrappers;
import com.cartethyia.easyorange.admin.domain.port.AdminDashboardPort;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.product.ProductDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.product.ProductMapper;
import com.cartethyia.easyorange.product.application.port.query.ProductQueryRepository;
import com.cartethyia.easyorange.product.domain.enums.ProductStatus;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Admin 仪表板查询适配器
 * <p>
 * 实现 {@link AdminDashboardPort}，聚合商品统计与榜单数据。
 */
@Primary
@Component
@RequiredArgsConstructor
public class AdminDashboardAdapter implements AdminDashboardPort {

    private final ProductMapper productMapper;
    private final ProductQueryRepository productQueryRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public ProductStats getProductStats() {
        return new ProductStats(
                productQueryRepository.countByStatus(null),
                productQueryRepository.countByStatus(ProductStatus.DRAFT.getCode()));
    }

    @Override
    public List<RecentProductRecord> getRecentProducts(int limit) {
        List<String> ids = ChainWrappers.lambdaQueryChain(productMapper)
                .eq(ProductDO::getDelFlag, 0)
                .orderByDesc(ProductDO::getCreateTime)
                .page(new Page<>(1, limit))
                .getRecords()
                .stream()
                .map(ProductDO::getId)
                .toList();

        return productQueryRepository.findProductsByIds(ids).stream()
                .map(model -> new RecentProductRecord(
                        model.id(),
                        model.sellerId(),
                        model.title(),
                        model.price(),
                        model.mainImageUrl(),
                        model.status(),
                        model.statusDesc(),
                        model.username(),
                        model.categoryName(),
                        model.views(),
                        model.createTime()))
                .toList();
    }

    @Override
    public List<TopProductRecord> getTopProducts(int limit) {
        return jdbcTemplate
                .queryForList(
                        "SELECT p.id, p.name, p.view_count, p.price, p.status, "
                                + "(SELECT pi.image_url FROM eo_product_image pi WHERE pi.product_id = p.id AND pi.del_flag = 0 ORDER BY pi.is_main DESC, pi.sort_order ASC LIMIT 1) AS main_image "
                                + "FROM eo_product p WHERE p.del_flag = 0 AND p.status = 'ONLINE' "
                                + "ORDER BY p.view_count DESC LIMIT ?",
                        limit)
                .stream()
                .map(row -> {
                    var statusValue = row.get("status");
                    var viewCountValue = row.get("view_count");
                    String statusCode = statusValue != null ? statusValue.toString() : null;
                    return new TopProductRecord(
                            String.valueOf(row.get("id")),
                            (String) row.get("name"),
                            viewCountValue != null ? ((Number) viewCountValue).intValue() : 0,
                            row.get("price") != null ? (BigDecimal) row.get("price") : BigDecimal.ZERO,
                            (String) row.get("main_image"),
                            statusCode,
                            statusDesc(statusCode));
                })
                .toList();
    }

    private static String statusDesc(String code) {
        if (code == null) {
            return "未知";
        }
        try {
            return ProductStatus.fromCode(code).getDesc();
        } catch (IllegalArgumentException e) {
            return "未知";
        }
    }
}
