package com.cartethyia.easyorange.product.adapter.inbound.web.dto.request;

import com.cartethyia.easyorange.common.dto.PageRequest;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductQueryRequest extends PageRequest {

    private String categoryId;

    private String userId;

    private String status;

    private String conditionLevel;

    private String keyword;

    private BigDecimal minPrice;

    private BigDecimal maxPrice;

    @Pattern(regexp = "^(default|price_asc|price_desc|newest|view|popular)?$", message = "排序方式必须为 default、price_asc、price_desc、newest、view 或 popular")
    private String sort;

    private Boolean hasDiscount;
}
