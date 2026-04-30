package com.cartethyia.easyorange.favorite.service.dto;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FavoritePageQuery {
    @Min(1)
    @Builder.Default
    private Integer pageNum = 1;

    @Min(1)
    @Builder.Default
    private Integer pageSize = 10;
}
