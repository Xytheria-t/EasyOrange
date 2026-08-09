package com.cartethyia.easyorange.adapter.outbound.elasticsearch;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "products")
public class ProductDocument {

    @Id
    private String id;

    private String userId;
    private String name;
    private String description;
    private String categoryId;
    private String categoryName;

    @Field(type = FieldType.Double)
    private Double price;

    @Field(type = FieldType.Double)
    private Double originalPrice;

    private String conditionLevel;
    private String status;
    private Integer viewCount;
    private Integer stock;
    private String location;
    private List<String> tags;
    private String mainImage;
    private List<String> images;
    private String sellerName;
    private String sellerAvatar;

    @Field(type = FieldType.Dense_Vector)
    private List<Float> nameEmbedding;

    /**
     * 时间以 epoch 毫秒（long）存储：Spring Data ES 6 对 LocalDateTime 的默认序列化不可靠
     * （无注解→ISO-8601 且按 UTC 转换，丢墙钟语义；注解 epoch_millis 又要求带时区类型，直接抛异常）。
     * 以 Long 落库由适配器在 DO↔readModel 边界显式转换，语义与 MySQL DATETIME（本地墙钟）一致，
     * 且 {@code product-mapping.json} 的 date/epoch_millis 格式与之对齐，排序/过滤按数字天然成立。
     */
    private Long createTime;

    private Long updateTime;
}
