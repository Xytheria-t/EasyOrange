package com.cartethyia.easyorange.product.application.command;

import com.cartethyia.easyorange.framework.util.TestSecurityUtil;
import com.cartethyia.easyorange.product.application.command.CreateProductRatingCommand;
import com.cartethyia.easyorange.product.domain.entity.ProductRating;
import com.cartethyia.easyorange.product.domain.repository.ProductRatingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductRatingCommandService 测试")
class ProductRatingCommandServiceTest {

    @Mock
    private ProductRatingRepository productRatingRepository;

    private ProductRatingCommandService commandService;

    @BeforeEach
    void setUp() {
        commandService = new ProductRatingCommandService(productRatingRepository);
    }

    @Test
    @DisplayName("创建评价应保存领域实体并返回 ID")
    void createReview_shouldCreateAndSave() {
        TestSecurityUtil.setSecurityContext("1");
        try {
            doAnswer(invocation -> {
                ProductRating rating = invocation.getArgument(0);
                // 模拟仓储生成 ID
                var field = ProductRating.class.getDeclaredField("id");
                field.setAccessible(true);
                field.set(rating, "100");
                return null;
            }).when(productRatingRepository).save(any(ProductRating.class));

            var command = new CreateProductRatingCommand("10", 5, "非常好的商品");

            String reviewId = commandService.createReview(command);

            assertThat(reviewId).isEqualTo("100");

            verify(productRatingRepository).save(argThat(r ->
                    r.getProductId().equals("10") &&
                    r.getUserId().equals("1") &&
                    r.getRating().value() == 5 &&
                    r.getContent().value().equals("非常好的商品")
            ));
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            TestSecurityUtil.clearSecurityContext();
        }
    }

    @Test
    @DisplayName("删除自己的评价应调用软删除")
    void deleteReview_ownReview_shouldSoftDelete() {
        TestSecurityUtil.setSecurityContext("1");
        try {
            ProductRating rating = ProductRating.create("10", "1", 4, "不错");
            when(productRatingRepository.findById("100")).thenReturn(Optional.of(rating));

            commandService.deleteReview("100");

            verify(productRatingRepository).update(argThat(r -> r.getStatus() == 0));
        } finally {
            TestSecurityUtil.clearSecurityContext();
        }
    }

    @Test
    @DisplayName("删除不存在的评价应抛出异常")
    void deleteReview_notFound_shouldThrow() {
        TestSecurityUtil.setSecurityContext("1");
        try {
            when(productRatingRepository.findById("999")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> commandService.deleteReview("999"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("评价不存在");

            verify(productRatingRepository, never()).update(any());
        } finally {
            TestSecurityUtil.clearSecurityContext();
        }
    }

    @Test
    @DisplayName("删除他人的评价应抛出异常")
    void deleteReview_notOwner_shouldThrow() {
        TestSecurityUtil.setSecurityContext("2");
        try {
            ProductRating rating = ProductRating.create("10", "1", 4, "不错");
            when(productRatingRepository.findById("100")).thenReturn(Optional.of(rating));

            assertThatThrownBy(() -> commandService.deleteReview("100"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("只能删除自己的评价");

            verify(productRatingRepository, never()).update(any());
        } finally {
            TestSecurityUtil.clearSecurityContext();
        }
    }

    @Test
    @DisplayName("点赞评价应增加点赞数")
    void likeReview_shouldIncrementLikes() {
        TestSecurityUtil.setSecurityContext("1");
        try {
            ProductRating rating = ProductRating.create("10", "1", 4, "不错");
            when(productRatingRepository.findById("100")).thenReturn(Optional.of(rating));

            commandService.likeReview("100");

            verify(productRatingRepository).update(argThat(r -> r.getLikes() == 1));
        } finally {
            TestSecurityUtil.clearSecurityContext();
        }
    }
}
