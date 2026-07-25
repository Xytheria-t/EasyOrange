package com.cartethyia.easyorange.product.application.command;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateProductRatingCommand(
        @NotNull(message = "商品ID不能为空")
        String productId,

        @NotNull(message = "评分不能为空")
        @Min(value = 1, message = "评分最小为1")
        @Max(value = 5, message = "评分最大为5")
        Integer rating,

        @NotBlank(message = "评价内容不能为空")
        @Size(max = 2000, message = "评价内容最多2000字")
        String content
) implements ProductCommand {}
