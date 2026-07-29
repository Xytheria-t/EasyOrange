package com.cartethyia.easyorange.product.adapter.outbound.persistence.rating;

import com.cartethyia.easyorange.common.repository.BaseRepository;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.rating.ProductRatingDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.rating.ProductRatingMapper;
import com.cartethyia.easyorange.product.domain.entity.ProductRating;
import com.cartethyia.easyorange.product.domain.repository.ProductRatingRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Primary
@Repository
public class ProductRatingRepositoryImpl extends BaseRepository<ProductRatingMapper, ProductRatingDO>
        implements ProductRatingRepository {

    public ProductRatingRepositoryImpl(ProductRatingMapper mapper) {
        super(mapper);
    }

    @Override
    public Optional<ProductRating> findById(String id) {
        ProductRatingDO do_ = mapper.selectById(id);
        return Optional.ofNullable(convertToDomain(do_));
    }

    @Override
    public void save(ProductRating rating) {
        ProductRatingDO do_ = convertToDO(rating);
        mapper.insert(do_);
        rating.assignId(do_.getId());
    }

    @Override
    public void update(ProductRating rating) {
        ProductRatingDO do_ = convertToDO(rating);
        mapper.updateById(do_);
    }

    @Override
    public void deleteById(String id) {
        mapper.deleteById(id);
    }

    @Override
    public void incrementLikes(String id) {
        mapper.incrementLikes(id);
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
                do_.getUpdateTime()
        );
    }

    private ProductRatingDO convertToDO(ProductRating rating) {
        var builder = ProductRatingDO.builder()
                .productId(rating.getProductId())
                .userId(rating.getUserId())
                .orderId(rating.getOrderId())
                .rating(rating.getRating().value())
                .content(rating.getContent().value())
                .replyContent(rating.getReplyContent())
                .replyTime(rating.getReplyTime())
                .likes(rating.getLikes())
                .status(rating.getStatus());

        if (rating.getId() != null) {
            builder.id(rating.getId());
        }

        return builder.build();
    }
}
