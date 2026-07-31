package com.cartethyia.easyorange.admin.adapter.inbound.web.assembler;

import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.AdminProductResponse;
import com.cartethyia.easyorange.admin.domain.port.AdminProductQueryPort.ProductDetail;
import com.cartethyia.easyorange.admin.domain.port.AdminProductQueryPort.ProductSummary;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AdminProductAssembler {

    public AdminProductResponse toDetailResponse(ProductDetail detail, List<String> images) {
        return AdminProductResponse.builder()
            .productId(detail.id())
            .name(detail.name())
            .description(detail.description())
            .price(detail.price())
            .originalPrice(detail.originalPrice())
            .stock(detail.stock())
            .status(detail.status())
            .statusDesc(detail.statusDesc())
            .conditionLevel(detail.conditionLevel())
            .location(detail.location())
            .contactMethod(detail.contactMethod())
            .images(images)
            .mainImage(resolveMainImage(images))
            .categoryId(detail.categoryId())
            .sellerId(detail.sellerId())
            .viewCount(detail.viewCount())
            .createTime(detail.createTime())
            .updateTime(detail.updateTime())
            .build();
    }

    public AdminProductResponse toSummaryResponse(ProductSummary summary, List<String> images) {
        return AdminProductResponse.builder()
            .productId(summary.id())
            .name(summary.name())
            .price(summary.price())
            .originalPrice(summary.originalPrice())
            .stock(summary.stock())
            .status(summary.status())
            .statusDesc(summary.statusDesc())
            .conditionLevel(summary.conditionLevel())
            .location(summary.location())
            .contactMethod(summary.contactMethod())
            .images(images)
            .mainImage(resolveMainImage(images))
            .categoryId(summary.categoryId())
            .sellerId(summary.sellerId())
            .viewCount(summary.viewCount())
            .createTime(summary.createTime())
            .updateTime(summary.updateTime())
            .build();
    }

    private String resolveMainImage(List<String> images) {
        return images.isEmpty() ? null : images.get(0);
    }
}
