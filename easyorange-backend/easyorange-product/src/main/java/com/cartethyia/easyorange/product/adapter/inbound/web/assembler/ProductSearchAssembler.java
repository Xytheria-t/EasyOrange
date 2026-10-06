package com.cartethyia.easyorange.product.adapter.inbound.web.assembler;

import com.cartethyia.easyorange.product.adapter.inbound.web.dto.response.FacetBucketResponse;
import com.cartethyia.easyorange.product.adapter.inbound.web.dto.response.HotKeywordResponse;
import com.cartethyia.easyorange.product.adapter.inbound.web.dto.response.ProductResponse;
import com.cartethyia.easyorange.product.adapter.inbound.web.dto.response.SearchHistoryResponse;
import com.cartethyia.easyorange.product.adapter.inbound.web.dto.response.SearchPageResponse;
import com.cartethyia.easyorange.product.application.port.query.FacetBucket;
import com.cartethyia.easyorange.product.application.query.dto.ProductSearchResult;
import com.cartethyia.easyorange.product.application.query.readmodel.HotKeywordReadModel;
import com.cartethyia.easyorange.product.application.query.readmodel.ProductReadModel;
import com.cartethyia.easyorange.product.application.query.readmodel.SearchHistoryReadModel;
import java.util.List;
import org.springframework.stereotype.Component;

/** 搜索侧读模型 → 响应 DTO 的唯一转换点（Controller 不直接构造 Response）。 */
@Component
public class ProductSearchAssembler {

    public SearchPageResponse<ProductResponse> toSearchPageResponse(ProductSearchResult result) {
        return new SearchPageResponse<>(
                toProductResponses(result.page().records()),
                result.page().total(),
                result.page().current(),
                result.page().size(),
                result.page().pages(),
                toFacetBucketResponses(result.facets()));
    }

    public List<ProductResponse> toProductResponses(List<ProductReadModel> models) {
        return models.stream().map(ProductSearchAssembler::toProductResponse).toList();
    }

    public List<SearchHistoryResponse> toSearchHistoryResponses(List<SearchHistoryReadModel> models) {
        return models.stream()
                .map(h -> SearchHistoryResponse.builder()
                        .id(h.id())
                        .keyword(h.keyword())
                        .createTime(h.createTime())
                        .build())
                .toList();
    }

    public List<HotKeywordResponse> toHotKeywordResponses(List<HotKeywordReadModel> models) {
        return models.stream()
                .map(k -> HotKeywordResponse.builder()
                        .id(k.id())
                        .keyword(k.keyword())
                        .searchCount(k.searchCount())
                        .hotLevel(k.hotLevel())
                        .build())
                .toList();
    }

    private List<FacetBucketResponse> toFacetBucketResponses(List<FacetBucket> buckets) {
        return buckets.stream()
                .map(fb -> new FacetBucketResponse(fb.key(), fb.label(), fb.count()))
                .toList();
    }

    private static ProductResponse toProductResponse(ProductReadModel model) {
        return ProductResponse.builder()
                .id(model.id())
                .sellerId(model.sellerId())
                .username(model.username())
                .userAvatar(model.userAvatar())
                .categoryId(model.categoryId())
                .categoryName(model.categoryName())
                .title(model.title())
                .description(model.description())
                .price(model.price())
                .originalPrice(model.originalPrice())
                .status(model.status())
                .statusDesc(model.statusDesc())
                .views(model.views())
                .condition(model.condition())
                .conditionDesc(model.conditionDesc())
                .location(model.location())
                .images(model.images())
                .mainImageUrl(model.mainImageUrl())
                .createTime(model.createTime())
                .build();
    }
}
