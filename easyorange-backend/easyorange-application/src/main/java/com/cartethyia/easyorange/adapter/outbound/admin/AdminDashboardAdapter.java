package com.cartethyia.easyorange.adapter.outbound.admin;

import com.cartethyia.easyorange.admin.domain.port.AdminDashboardPort;
import com.cartethyia.easyorange.product.application.port.query.ProductQueryRepository;
import com.cartethyia.easyorange.product.domain.enums.ProductStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Admin 仪表板查询适配器
 * <p>
 * 实现 {@link AdminDashboardPort}，提供商品统计（总数 / 待审核数）。
 */
@Primary
@Component
@RequiredArgsConstructor
public class AdminDashboardAdapter implements AdminDashboardPort {

    private final ProductQueryRepository productQueryRepository;

    @Override
    public ProductStats getProductStats() {
        return new ProductStats(
                productQueryRepository.countByStatus(null),
                productQueryRepository.countByStatus(ProductStatus.DRAFT.getCode()));
    }

}
