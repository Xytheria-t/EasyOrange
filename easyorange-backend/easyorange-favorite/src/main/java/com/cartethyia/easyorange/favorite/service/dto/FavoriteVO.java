package com.cartethyia.easyorange.favorite.service.dto;

import com.cartethyia.easyorange.product.application.query.dto.ProductVO;
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
    private ProductVO product;
    private LocalDateTime createTime;
}
