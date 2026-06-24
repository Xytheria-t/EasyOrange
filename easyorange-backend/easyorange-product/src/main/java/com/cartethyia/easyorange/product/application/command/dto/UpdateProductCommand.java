package com.cartethyia.easyorange.product.application.command.dto;

import com.cartethyia.easyorange.product.adapter.inbound.web.dto.request.ProductUpdateRequest;
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
public class UpdateProductCommand {

    private Long id;
    private Long categoryId;
    private String name;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer stock;
    private Integer conditionLevel;
    private Integer consignmentMode;
    private BigDecimal floorPrice;
    private String location;
    private String contactMethod;
    private String description;
    private List<String> imageUrls;

    public static UpdateProductCommand from(Long id, ProductUpdateRequest request) {
        return UpdateProductCommand.builder()
                .id(id)
                .categoryId(request.getCategoryId())
                .name(request.getName())
                .price(request.getPrice())
                .originalPrice(request.getOriginalPrice())
                .stock(request.getStock())
                .conditionLevel(request.getConditionLevel())
                .consignmentMode(request.getConsignmentMode())
                .floorPrice(request.getFloorPrice())
                .location(request.getLocation())
                .contactMethod(request.getContactMethod())
                .description(request.getDescription())
                .imageUrls(request.getImageUrls())
                .build();
    }
}
