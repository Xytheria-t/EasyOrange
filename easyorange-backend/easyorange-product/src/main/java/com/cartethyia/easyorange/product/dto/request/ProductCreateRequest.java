package com.cartethyia.easyorange.product.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductCreateRequest {

    @NotNull(message = "分类ID不能为空")
    private Long categoryId;

    @NotBlank(message = "商品名称不能为空")
    @Size(max = 200, message = "商品名称不能超过200个字符")
    private String name;

    @Size(max = 2000, message = "商品描述不能超过2000个字符")
    private String description;

    @NotNull(message = "商品价格不能为空")
    @DecimalMin(value = "0.01", message = "商品价格必须大于0")
    private BigDecimal price;

    @DecimalMin(value = "0.01", message = "商品原价必须大于0")
    private BigDecimal originalPrice;

    private Integer stock = 1;

    @NotNull(message = "新旧程度不能为空")
    private Integer conditionLevel;

    @Size(max = 100, message = "交易地点不能超过100个字符")
    private String location;

    @Size(max = 50, message = "联系方式不能超过50个字符")
    private String contactMethod;

    @Size(max = 9, message = "图片数量不能超过9张")
    private List<String> imageUrls;
}
