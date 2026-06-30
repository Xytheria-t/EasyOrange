package com.cartethyia.easyorange.product.application.command;

import com.cartethyia.easyorange.framework.util.TestSecurityUtil;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.dataobject.ProductReviewDO;

import com.cartethyia.easyorange.product.adapter.outbound.persistence.mapper.ProductReviewMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
        TestSecurityUtil.setSecurityContext(1L);
        try {
            doAnswer(invocation -> {
                ProductReviewDO review = invocation.getArgument(0);
                review.setId("100");
                return 1;
            }).when(reviewMapper).insert(any(ProductReviewDO.class));

            CreateProductReviewCommand command = CreateProductReviewCommand.builder()
                    .productId("10")
                    .rating(5)
                    .content("非常好的商品")
                    .build();

            String reviewId = commandService.createReview(command);

            assertThat(reviewId).isEqualTo("100");

            ArgumentCaptor<ProductReviewDO> captor = ArgumentCaptor.forClass(ProductReviewDO.class);
            verify(reviewMapper).insert(captor.capture());
            ProductReviewDO captured = captor.getValue();
            assertThat(captured.getProductId()).isEqualTo("10");
            assertThat(captured.getUserId()).isEqualTo("1");
            assertThat(captured.getRating()).isEqualTo(5);
            assertThat(captured.getContent()).isEqualTo("非常好的商品");
            assertThat(captured.getLikes()).isEqualTo(0);
            assertThat(captured.getStatus()).isEqualTo(1);
        } finally {
            TestSecurityUtil.clearSecurityContext();
        }
    }

    @Test
    @DisplayName("删除自己的评价应成功")
    void deleteReview_ownReview_shouldDelete() {
        TestSecurityUtil.setSecurityContext(1L);
        try {
            ProductReviewDO existing = new ProductReviewDO();
            existing.setId("100");
            existing.setProductId("10");
            existing.setUserId("1");
            existing.setDelFlag(0);
            when(reviewMapper.selectById("100")).thenReturn(existing);

            commandService.deleteReview("100");

            verify(reviewMapper).deleteById("100");
        } finally {
            TestSecurityUtil.clearSecurityContext();
        }
    }

    @Test
    @DisplayName("删除不存在的评价应抛出异常")
    void deleteReview_notFound_shouldThrow() {
        TestSecurityUtil.setSecurityContext(1L);
        try {
            when(reviewMapper.selectById("999")).thenReturn(null);

            assertThatThrownBy(() -> commandService.deleteReview("999"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("评价不存在");

            verify(reviewMapper, never()).deleteById(anyString());
        } finally {
            TestSecurityUtil.clearSecurityContext();
        }
    }

    @Test
    @DisplayName("删除已被逻辑删除的评价应抛出异常")
    void deleteReview_alreadyDeleted_shouldThrow() {
        TestSecurityUtil.setSecurityContext(1L);
        try {
            ProductReviewDO existing = new ProductReviewDO();
            existing.setId("100");
            existing.setUserId("1");
            existing.setDelFlag(2);
            when(reviewMapper.selectById("100")).thenReturn(existing);

            assertThatThrownBy(() -> commandService.deleteReview("100"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("评价不存在");

            verify(reviewMapper, never()).deleteById(anyString());
        } finally {
            TestSecurityUtil.clearSecurityContext();
        }
    }

    @Test
    @DisplayName("删除他人的评价应抛出异常")
    void deleteReview_notOwner_shouldThrow() {
        TestSecurityUtil.setSecurityContext(2L);
        try {
            ProductReviewDO existing = new ProductReviewDO();
            existing.setId("100");
            existing.setUserId("1");
            existing.setDelFlag(0);
            when(reviewMapper.selectById("100")).thenReturn(existing);

            assertThatThrownBy(() -> commandService.deleteReview("100"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("只能删除自己的评价");

            verify(reviewMapper, never()).deleteById(anyString());
        } finally {
            TestSecurityUtil.clearSecurityContext();
        }
    }

    @Test
    @DisplayName("点赞评价应增加点赞数")
    void likeReview_shouldIncrementLikes() {
        commandService.likeReview("100");

        verify(reviewMapper).incrementLikes("100");
    }
}
