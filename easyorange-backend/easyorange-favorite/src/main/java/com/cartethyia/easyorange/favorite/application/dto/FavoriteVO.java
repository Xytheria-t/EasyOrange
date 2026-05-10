package com.cartethyia.easyorange.favorite.application.dto;

import com.cartethyia.easyorange.favorite.domain.valueobject.ProductDetailInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteVO {
    private Long id;
    private Long productId;
    private ProductDetailInfo product;
    private LocalDateTime createTime;
}
