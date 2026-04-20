package com.cartethyia.easyorange.product.application.command;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class CreateProductCommand {

    private Long categoryId;

    private String name;

    private BigDecimal price;

    private BigDecimal originalPrice;

    private Integer stock;

    private Integer conditionLevel;

    private String location;

    private String contactMethod;

    private String description;

    private List<String> imageUrls;
}