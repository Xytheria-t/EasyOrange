package com.cartethyia.easyorange.admin.adapter.inbound.web.assembler;

import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.AdminProductResponse;
import com.cartethyia.easyorange.admin.domain.model.ProductDetailView;
import com.cartethyia.easyorange.admin.domain.model.ProductListView;
import com.cartethyia.easyorange.admin.domain.port.AdminProductPort.ProductDetail;
import com.cartethyia.easyorange.admin.domain.port.AdminProductPort.ProductQueryResult;
import com.cartethyia.easyorange.admin.domain.port.AdminProductPort.ProductSummary;
import com.cartethyia.easyorange.common.result.PageResult;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 商品出参组装 — 主图取哪一张、列表与详情共用一套字段命名，都属展示口径，留在 web 边界。
 */
@Component
public class AdminProductAssembler {

    public PageResult<AdminProductResponse> toPageResponses(ProductListView view) {
        List<AdminProductResponse> records = view.page().records().stream()
                .map(summary -> toSummaryResponse(summary, view.images().getOrDefault(summary.id(), List.of())))
                .toList();
        ProductQueryResult page = view.page();
        return PageResult.of(records, page.total(), page.pageNum(), page.pageSize());
    }

    public AdminProductResponse toDetailResponse(ProductDetailView view) {
        return toDetailResponse(view.product(), view.images());
    }

    private AdminProductResponse toDetailResponse(ProductDetail detail, List<String> images) {
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

    private AdminProductResponse toSummaryResponse(ProductSummary summary, List<String> images) {
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
