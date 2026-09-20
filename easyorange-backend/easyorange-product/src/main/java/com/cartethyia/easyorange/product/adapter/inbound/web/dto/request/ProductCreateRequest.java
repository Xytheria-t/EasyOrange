package com.cartethyia.easyorange.product.adapter.inbound.web.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
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

        // 拍照识别给出的建议快照：只写不改，不参与定价逻辑，用于统计字段级采纳率
        @Valid AiSuggestionSnapshot aiSuggestion) {
    public ProductCreateRequest {
        if (stock == null) {
            stock = 1;
        }
    }

    /**
     * 建议快照 — 六个字段与 {@code AutoListingResult} 一一对应。
     * <p>
     * 只校验长度与价格下限（防脏数据撑爆统计），不做格式强校验：建议值不合规的正确表现是
     * 「这个字段没被采纳」（照统计口径算），而不是让整单发布失败。
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AiSuggestionSnapshot(
            @Size(max = 200, message = "AI 建议标题不能超过 200 个字符")
            String title,

            @Size(max = 2000, message = "AI 建议描述不能超过 2000 个字符")
            String description,

            @DecimalMin(value = "0.01", message = "AI 建议价必须大于 0")
            BigDecimal price,

            @Size(max = 50, message = "AI 建议类目不能超过 50 个字符") String categoryName,

            @Size(max = 2, message = "AI 建议成色不能超过 2 个字符") String conditionLevel,

            @Size(max = 100, message = "AI 建议地点不能超过 100 个字符")
            String location) {}
}
