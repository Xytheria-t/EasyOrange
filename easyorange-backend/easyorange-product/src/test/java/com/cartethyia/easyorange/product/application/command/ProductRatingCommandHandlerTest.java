package com.cartethyia.easyorange.product.application.command;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.cartethyia.easyorange.product.domain.entity.ProductRating;
import com.cartethyia.easyorange.product.domain.repository.ProductRatingRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductRatingCommandHandler 测试")
class ProductRatingCommandHandlerTest {

    @Mock
    private ProductRatingRepository productRatingRepository;

    private ProductRatingCommandHandler commandHandler;

    @BeforeEach
    void setUp() {
        commandHandler = new ProductRatingCommandHandler(productRatingRepository);
    }

    @Test
    @DisplayName("创建评价应保存领域实体并返回 ID")
    void createReview_shouldCreateAndSave() {
        try {
            doAnswer(invocation -> {
                        ProductRating rating = invocation.getArgument(0);
                        // 模拟仓储生成 ID
                        var field = ProductRating.class.getDeclaredField("id");
                        field.setAccessible(true);
                        field.set(rating, "100");
                        return null;
                    })
                    .when(productRatingRepository)
                    .save(any(ProductRating.class));

            var command = new CreateProductRatingCommand("10", 5, "非常好的商品");

            String reviewId = commandHandler.createReview("1", command);

            assertThat(reviewId).isEqualTo("100");

            verify(productRatingRepository)
                    .save(argThat(r -> r.getProductId().equals("10")
                            && r.getUserId().equals("1")
                            && r.getRating().value() == 5
                            && r.getContent().value().equals("非常好的商品")));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("删除自己的评价应调用软删除")
    void deleteReview_ownReview_shouldSoftDelete() {
        ProductRating rating = ProductRating.create("10", "1", 4, "不错");
        when(productRatingRepository.findById("100")).thenReturn(Optional.of(rating));

        commandHandler.deleteReview("1", "100");

        verify(productRatingRepository).update(argThat(r -> r.getStatus() == 0));
    }

    @Test
    @DisplayName("删除不存在的评价应抛出异常")
    void deleteReview_notFound_shouldThrow() {
        when(productRatingRepository.findById("999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commandHandler.deleteReview("1", "999"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("评价不存在");

        verify(productRatingRepository, never()).update(any());
    }

    @Test
    @DisplayName("删除他人的评价应抛出异常")
    void deleteReview_notOwner_shouldThrow() {
        ProductRating rating = ProductRating.create("10", "1", 4, "不错");
        when(productRatingRepository.findById("100")).thenReturn(Optional.of(rating));

        assertThatThrownBy(() -> commandHandler.deleteReview("2", "100"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("只能删除自己的评价");

        verify(productRatingRepository, never()).update(any());
    }

    @Test
    @DisplayName("点赞评价应增加点赞数")
    void likeReview_shouldIncrementLikes() {
        ProductRating rating = ProductRating.create("10", "1", 4, "不错");
        when(productRatingRepository.findById("100")).thenReturn(Optional.of(rating));

        commandHandler.likeReview("100");

        verify(productRatingRepository).update(argThat(r -> r.getLikes() == 1));
    }
}
