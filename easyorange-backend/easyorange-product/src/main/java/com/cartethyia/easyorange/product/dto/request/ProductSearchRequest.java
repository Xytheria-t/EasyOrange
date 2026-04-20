package com.cartethyia.easyorange.product.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductSearchRequest {

    private String keyword;

    private Long categoryId;

    private Long userId;

    private Integer status;

    private Integer conditionLevel;

    private BigDecimal minPrice;

    private BigDecimal maxPrice;

    private String location;

    private String sortBy = "relevance";

    private String sortOrder = "desc";

    @Min(value = 1, message = "页码最小为 1")
    private Integer pageNum = 1;

    @Min(value = 1, message = "每页条数最小为 1")
    @Max(value = 50, message = "每页条数最大为 50")
    private Integer pageSize = 10;
}
