package com.cartethyia.easyorange.product.application.query.dto;

import java.math.BigDecimal;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RatingStatsVO {

    private String productId;

    private Long totalCount;

    private BigDecimal averageRating;

    private Map<Integer, Long> ratingDistribution;
}
