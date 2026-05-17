package com.cartethyia.easyorange.adapter.outbound.elasticsearch;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "products")
public class ProductDocument {

    @Id
    private String id;

    private Long userId;
    private String name;
    private String description;
    private Integer categoryId;
    private String categoryName;

    @Field(type = FieldType.Double)
    private Double price;

    @Field(type = FieldType.Double)
    private Double originalPrice;

    private Byte conditionLevel;
    private Byte status;
    private Integer viewCount;
    private Integer stock;
    private String location;
    private List<String> tags;
    private String mainImage;
    private List<String> images;
    private String sellerName;
    private String sellerAvatar;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
