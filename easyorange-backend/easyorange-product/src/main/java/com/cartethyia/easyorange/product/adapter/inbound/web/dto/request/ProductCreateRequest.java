package com.cartethyia.easyorange.product.adapter.inbound.web.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductCreateRequest(
        @NotNull(message = "分类 ID 不能为空") String categoryId,

        @NotBlank(message = "商品名称不能为空") @Size(max = 200, message = "商品名称不能超过 200 个字符")
        String name,

        @Size(max = 2000, message = "商品描述不能超过 2000 个字符") String description,

        @NotNull(message = "商品价格不能为空") @DecimalMin(value = "0.01", message = "商品价格必须大于 0")
        BigDecimal price,

        @DecimalMin(value = "0.01", message = "商品原价必须大于 0") BigDecimal originalPrice,
        Integer stock,

        @NotNull(message = "新旧程度不能为空") @Pattern(regexp = "^[1-4]$", message = "成色等级必须为 1-4")
        String conditionLevel,

        @Size(max = 100, message = "交易地点不能超过 100 个字符") String location,
        @Size(max = 50, message = "联系方式不能超过 50 个字符") String contactMethod,
        @Size(max = 9, message = "图片数量不能超过 9 张") List<String> imageUrls,

        // 拍照识别给出的建议价：只写不改，不参与定价逻辑，用于统计 AI 建议的采纳率与偏离度
        @DecimalMin(value = "0.01", message = "AI 建议价必须大于 0")
        BigDecimal aiSuggestedPrice) {
    public ProductCreateRequest {
        if (stock == null) {
            stock = 1;
        }
    }
}
