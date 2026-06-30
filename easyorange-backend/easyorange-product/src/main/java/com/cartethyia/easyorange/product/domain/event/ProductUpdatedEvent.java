package com.cartethyia.easyorange.product.domain.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;

import java.math.BigDecimal;
import java.util.List;

public class ProductUpdatedEvent extends BaseDomainEvent {

    private final String productId;
    private final String userId;
    private final String categoryId;
    private final String name;
    private final BigDecimal price;
    private final BigDecimal originalPrice;
    private final Integer stock;
    private final Integer conditionLevel;
    private final String location;
    private final String contactMethod;
    private final String description;
    private final List<String> imageUrls;

    public ProductUpdatedEvent(String productId, String userId, String categoryId, String name,
                              BigDecimal price, BigDecimal originalPrice, Integer stock,
                              Integer conditionLevel, String location, String contactMethod,
                              String description, List<String> imageUrls) {
        super();
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
        this.imageUrls = imageUrls != null ? List.copyOf(imageUrls) : List.of();
    }

    public String getProductId() { return productId; }
    public String getUserId() { return userId; }
    public String getCategoryId() { return categoryId; }
    public String getName() { return name; }
    public BigDecimal getPrice() { return price; }
    public BigDecimal getOriginalPrice() { return originalPrice; }
    public Integer getStock() { return stock; }
    public Integer getConditionLevel() { return conditionLevel; }
    public String getLocation() { return location; }
    public String getContactMethod() { return contactMethod; }
    public String getDescription() { return description; }
    public List<String> getImageUrls() { return imageUrls; }

}
