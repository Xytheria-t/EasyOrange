package com.cartethyia.easyorange.product.application.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewStatsVO {

    private String productId;

    private Long totalCount;

    private BigDecimal averageRating;

    private Map<Integer, Long> ratingDistribution;
}
