package com.cartethyia.easyorange.favorite.application.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RemoveFavoriteDTO {
    @NotNull(message = "商品ID不能为空")
    private Long productId;
}
