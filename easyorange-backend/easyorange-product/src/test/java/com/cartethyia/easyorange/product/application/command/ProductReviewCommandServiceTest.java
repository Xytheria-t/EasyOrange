package com.cartethyia.easyorange.product.application.command;

import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject.ProductReviewDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.mapper.ProductReviewMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.Serializable;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductReviewCommandService 测试")
class ProductReviewCommandServiceTest {

    @Mock
    private ProductReviewMapper reviewMapper;

    private ProductReviewCommandService commandService;

    @BeforeEach
    void setUp() {
        commandService = new ProductReviewCommandService(reviewMapper);
    }

    @Test
    @DisplayName("创建评价应插入数据库并返回 ID")
    void createReview_shouldInsertAndReturnId() {
        try (var mocked = mockStatic(SecurityContextUtil.class)) {
            mocked.when(SecurityContextUtil::getCurrentUserIdOrThrow).thenReturn(1L);

            doAnswer(invocation -> {
                ProductReviewDO review = invocation.getArgument(0);
                review.setId(100L);
                return 1;
            }).when(reviewMapper).insert(any(ProductReviewDO.class));

            CreateProductReviewCommand command = CreateProductReviewCommand.builder()
                    .productId(10L)
                    .rating(5)
                    .content("非常好的商品")
                    .build();

            Long reviewId = commandService.createReview(command);

            assertThat(reviewId).isEqualTo(100L);

            ArgumentCaptor<ProductReviewDO> captor = ArgumentCaptor.forClass(ProductReviewDO.class);
            verify(reviewMapper).insert(captor.capture());
            ProductReviewDO captured = captor.getValue();
            assertThat(captured.getProductId()).isEqualTo(10L);
            assertThat(captured.getUserId()).isEqualTo(1L);
            assertThat(captured.getRating()).isEqualTo(5);
            assertThat(captured.getContent()).isEqualTo("非常好的商品");
            assertThat(captured.getLikes()).isEqualTo(0);
            assertThat(captured.getStatus()).isEqualTo(1);
        }
    }

    @Test
    @DisplayName("删除自己的评价应成功")
    void deleteReview_ownReview_shouldDelete() {
        try (var mocked = mockStatic(SecurityContextUtil.class)) {
            mocked.when(SecurityContextUtil::getCurrentUserIdOrThrow).thenReturn(1L);

            ProductReviewDO existing = new ProductReviewDO();
            existing.setId(100L);
            existing.setProductId(10L);
            existing.setUserId(1L);
            existing.setDelFlag(0);
            when(reviewMapper.selectById(100L)).thenReturn(existing);

            commandService.deleteReview(100L);

            verify(reviewMapper).deleteById((Serializable) 100L);
        }
    }

    @Test
    @DisplayName("删除不存在的评价应抛出异常")
    void deleteReview_notFound_shouldThrow() {
        try (var mocked = mockStatic(SecurityContextUtil.class)) {
            mocked.when(SecurityContextUtil::getCurrentUserIdOrThrow).thenReturn(1L);

            when(reviewMapper.selectById(999L)).thenReturn(null);

            assertThatThrownBy(() -> commandService.deleteReview(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("评价不存在");

            verify(reviewMapper, never()).deleteById(any(Serializable.class));
        }
    }

    @Test
    @DisplayName("删除已被逻辑删除的评价应抛出异常")
    void deleteReview_alreadyDeleted_shouldThrow() {
        try (var mocked = mockStatic(SecurityContextUtil.class)) {
            mocked.when(SecurityContextUtil::getCurrentUserIdOrThrow).thenReturn(1L);

            ProductReviewDO existing = new ProductReviewDO();
            existing.setId(100L);
            existing.setUserId(1L);
            existing.setDelFlag(2);
            when(reviewMapper.selectById(100L)).thenReturn(existing);

            assertThatThrownBy(() -> commandService.deleteReview(100L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("评价不存在");

            verify(reviewMapper, never()).deleteById(any(Serializable.class));
        }
    }

    @Test
    @DisplayName("删除他人的评价应抛出异常")
    void deleteReview_notOwner_shouldThrow() {
        try (var mocked = mockStatic(SecurityContextUtil.class)) {
            mocked.when(SecurityContextUtil::getCurrentUserIdOrThrow).thenReturn(2L);

            ProductReviewDO existing = new ProductReviewDO();
            existing.setId(100L);
            existing.setUserId(1L);
            existing.setDelFlag(0);
            when(reviewMapper.selectById(100L)).thenReturn(existing);

            assertThatThrownBy(() -> commandService.deleteReview(100L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("只能删除自己的评价");

            verify(reviewMapper, never()).deleteById(any(Serializable.class));
        }
    }

    @Test
    @DisplayName("点赞评价应增加点赞数")
    void likeReview_shouldIncrementLikes() {
        commandService.likeReview(100L);

        verify(reviewMapper).incrementLikes(100L);
    }
}
