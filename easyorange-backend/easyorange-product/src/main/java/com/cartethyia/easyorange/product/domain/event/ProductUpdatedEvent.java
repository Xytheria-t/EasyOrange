package com.cartethyia.easyorange.product.domain.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.List;

@SuperBuilder(toBuilder = true)
@Data
@EqualsAndHashCode(callSuper = true)
public class ProductUpdatedEvent extends BaseDomainEvent {

    private static final String AGGREGATE_TYPE = "Product";

    private Long productId;
    private Long userId;
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

    public ProductUpdatedEvent(Long productId, Long userId, Long categoryId, String name,
                              BigDecimal price, BigDecimal originalPrice, Integer stock,
                              Integer conditionLevel, String location, String contactMethod,
                              String description, List<String> imageUrls) {
        super(AGGREGATE_TYPE);
        this.productId = productId;
        this.userId = userId;
        this.categoryId = categoryId;
        this.name = name;
        this.price = price;
        this.originalPrice = originalPrice;
        this.stock = stock;
        this.conditionLevel = conditionLevel;
        this.location = location;
        this.contactMethod = contactMethod;
        this.description = description;
        this.imageUrls = imageUrls;
    }
}
