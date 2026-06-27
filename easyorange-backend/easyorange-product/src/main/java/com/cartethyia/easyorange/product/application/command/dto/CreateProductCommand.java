package com.cartethyia.easyorange.product.application.command.dto;

import com.cartethyia.easyorange.product.adapter.inbound.web.dto.request.ProductCreateRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

    public static CreateProductCommand from(ProductCreateRequest request) {
        return CreateProductCommand.builder()
                .categoryId(request.getCategoryId())
                .name(request.getName())
                .price(request.getPrice())
                .originalPrice(request.getOriginalPrice())
                .stock(request.getStock())
                .conditionLevel(request.getConditionLevel())
                .location(request.getLocation())
                .contactMethod(request.getContactMethod())
                .description(request.getDescription())
                .imageUrls(request.getImageUrls())
                .build();
    }
}
