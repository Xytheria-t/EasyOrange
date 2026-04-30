package com.cartethyia.easyorange.product.interfaces.rest.dto.request;

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

    private Long categoryId;

    private Long userId;

    private Integer status;

    private Integer conditionLevel;

    private String keyword;

    private BigDecimal minPrice;

    private BigDecimal maxPrice;

    @Pattern(regexp = "^(default|price_asc|price_desc|newest|view)?$", message = "排序方式必须为 default、price_asc、price_desc、newest 或 view")
    private String sort;
}
