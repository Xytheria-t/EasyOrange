package com.cartethyia.easyorange.product.application.query;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductVO {

    private String id;
    private String sellerId;
    private String username;
    private String userAvatar;
    private String categoryId;
    private String categoryName;
    private String title;
    private String description;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer stock;
    private String status;
    private String statusDesc;
    private Integer views;
    private String condition;
    private String conditionDesc;
    private String location;
    private String contactMethod;
    private List<String> images;
    private String mainImageUrl;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
