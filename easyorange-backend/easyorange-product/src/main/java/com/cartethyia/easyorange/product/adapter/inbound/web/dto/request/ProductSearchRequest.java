package com.cartethyia.easyorange.product.adapter.inbound.web.dto.request;

import com.cartethyia.easyorange.common.dto.PageRequest;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductSearchRequest extends PageRequest {

    private String keyword;

    private String categoryId;

    private String userId;

    private String status;

    @Pattern(regexp = "^[1-4]$", message = "成色等级必须为 1-4")
    private String conditionLevel;

    private BigDecimal minPrice;

    private BigDecimal maxPrice;

    private String location;

    /** 语义检索开关：打开且按相关度排序时向量化查询词，启用 kNN + BM25 + RRF 混合召回 */
    private boolean aiEnhanced;
}
