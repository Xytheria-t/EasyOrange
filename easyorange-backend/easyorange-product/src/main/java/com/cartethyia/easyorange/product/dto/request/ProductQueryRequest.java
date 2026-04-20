package com.cartethyia.easyorange.product.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductQueryRequest {

    private Long categoryId;

    private Long userId;

    private Integer status;

    private Integer conditionLevel;

    private String keyword;

    private BigDecimal minPrice;

    private BigDecimal maxPrice;

    private String sortBy = "createTime";

    private String sortOrder = "desc";

    @JsonProperty("page")
    @Min(value = 1, message = "页码最小为 1")
    private Integer pageNum = 1;

    @JsonProperty("size")
    @Min(value = 1, message = "每页条数最小为 1")
    @Max(value = 100, message = "每页条数最大为 100")
    private Integer pageSize = 10;
}
