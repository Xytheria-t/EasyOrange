package com.cartethyia.easyorange.favorite.adapter.inbound.web.dto.response;

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
public class FavoriteResponse {
    private String id;
    private String productId;
    private ProductDetailInfo product;
    private LocalDateTime createTime;
}