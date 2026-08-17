package com.cartethyia.easyorange.favorite.adapter.inbound.web.dto.response;

import com.cartethyia.easyorange.favorite.domain.valueobject.ProductDetailInfo;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteResponse {
    private String id;
    private String productId;
    private BigDecimal priceSnapshot;
    private ProductDetailInfo product;
    private LocalDateTime createTime;
}
