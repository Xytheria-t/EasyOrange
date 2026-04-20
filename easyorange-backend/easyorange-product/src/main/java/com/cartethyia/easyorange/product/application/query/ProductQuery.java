package com.cartethyia.easyorange.product.application.query;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ProductQuery {

    private Long id;

    private Long userId;

    private Long categoryId;

    private Integer status;

    private String keyword;

    private Integer conditionLevel;

    private BigDecimal minPrice;

    private BigDecimal maxPrice;

    private String sortBy;

    private String sortOrder;

    private Integer pageNum;

    private Integer pageSize;
}