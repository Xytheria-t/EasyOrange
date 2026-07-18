package com.cartethyia.easyorange.product.adapter.inbound.web.dto.request;

import com.cartethyia.easyorange.product.application.command.ProductRatingCommandService.CreateProductRatingCommand;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRatingRequest {

    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分最小为1")
    @Max(value = 5, message = "评分最大为5")
    private Integer rating;

    @NotBlank(message = "评价内容不能为空")
    @Size(max = 2000, message = "评价内容最多2000字")
    private String content;

    public CreateProductRatingCommand toCommand(String productId) {
        return new CreateProductRatingCommand(productId, rating, content);
    }
}
