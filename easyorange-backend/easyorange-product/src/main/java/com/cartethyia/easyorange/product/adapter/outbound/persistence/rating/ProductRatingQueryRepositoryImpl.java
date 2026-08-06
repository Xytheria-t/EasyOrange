package com.cartethyia.easyorange.product.adapter.outbound.persistence.rating;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cartethyia.easyorange.common.repository.BaseRepository;
import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.product.application.port.query.ProductRatingQueryRepository;
import com.cartethyia.easyorange.product.domain.entity.ProductRating;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;

@Repository
public class ProductRatingQueryRepositoryImpl extends BaseRepository<ProductRatingMapper, ProductRatingDO>
        implements ProductRatingQueryRepository {

    public ProductRatingQueryRepositoryImpl(ProductRatingMapper mapper) {
        super(mapper);
    }

    @Override
    public PageResult<ProductRating> findByProductId(String productId, int pageNum, int pageSize) {
        Page<ProductRatingDO> page = new Page<>(pageNum, pageSize);
        Page<ProductRatingDO> resultPage = lambdaQuery()
                .eq(ProductRatingDO::getProductId, productId)
                .eq(ProductRatingDO::getStatus, 1)
                .orderByDesc(ProductRatingDO::getCreateTime)
                .page(page);

        List<ProductRating> ratings =
                resultPage.getRecords().stream().map(this::convertToDomain).toList();

        return PageResult.of(ratings, resultPage.getTotal(), pageNum, pageSize);
    }

    @Override
    public List<ProductRating> findAllByProductId(String productId) {
        return lambdaQuery()
                .eq(ProductRatingDO::getProductId, productId)
                .eq(ProductRatingDO::getStatus, 1)
                .list()
                .stream()
                .map(this::convertToDomain)
                .toList();
    }

    @Override
    public Map<Integer, Long> countByRatingGroup(String productId) {
        List<Map<String, Object>> results = mapper.countByRating(productId);

        Map<Integer, Long> distribution = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            distribution.put(i, 0L);
        }
        for (Map<String, Object> row : results) {
            Integer rating = ((Number) row.get("rating")).intValue();
            Long count = ((Number) row.get("count")).longValue();
            distribution.put(rating, count);
        }
        return distribution;
    }

    private ProductRating convertToDomain(ProductRatingDO do_) {
        if (do_ == null) return null;
        return ProductRating.reconstitute(
                do_.getId(),
                do_.getProductId(),
                do_.getUserId(),
                do_.getOrderId(),
                do_.getRating() != null ? do_.getRating() : 0,
                do_.getContent(),
                do_.getReplyContent(),
                do_.getReplyTime(),
                do_.getLikes() != null ? do_.getLikes() : 0,
                do_.getStatus() != null ? do_.getStatus() : 1,
                do_.getCreateTime(),
                do_.getUpdateTime());
    }
}
