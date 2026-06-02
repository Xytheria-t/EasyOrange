package com.cartethyia.easyorange.product.adapter.inbound.web.dto.request;

import com.cartethyia.easyorange.common.dto.PageRequest;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductSearchRequest extends PageRequest {

    private String keyword;

    private Long categoryId;

    private Long userId;

    private Integer status;

    private Integer conditionLevel;

    private BigDecimal minPrice;

    private BigDecimal maxPrice;

    private String location;

    private boolean aiEnhanced;
}
