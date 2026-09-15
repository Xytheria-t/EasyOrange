package com.cartethyia.easyorange.product.application.command;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.idgen.IdGenerator;
import com.cartethyia.easyorange.product.domain.entity.ProductRating;
import com.cartethyia.easyorange.product.domain.exception.ProductDomainException;
import com.cartethyia.easyorange.product.domain.port.CompletedOrderPort;
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

    @Mock
    private IdGenerator idGenerator;

    @Mock
    private CompletedOrderPort completedOrderPort;

    private ProductRatingCommandHandler commandHandler;

    @BeforeEach
    void setUp() {
        commandHandler = new ProductRatingCommandHandler(productRatingRepository, idGenerator, completedOrderPort);
    }

    @Test
    @DisplayName("创建评价应保存领域实体并返回 ID")
    void createReview_shouldCreateAndSave() {
        when(completedOrderPort.findCompletedOrderId("1", "10")).thenReturn(Optional.of("200"));
        when(idGenerator.generateId()).thenReturn("100");

        var command = new CreateProductRatingCommand("10", 5, "非常好的商品");

        String reviewId = commandHandler.createReview("1", command);

        assertThat(reviewId).isEqualTo("100");

        verify(productRatingRepository)
                .save(argThat(r -> "100".equals(r.getId())
                        && "200".equals(r.getOrderId())
                        && r.getProductId().equals("10")
                        && r.getUserId().equals("1")
                        && r.getRating().value() == 5
                        && r.getContent().value().equals("非常好的商品")));
    }

    @Test
    @DisplayName("没有已完成订单时拒绝评价")
    void createReview_withoutCompletedOrder_throws() {
        when(completedOrderPort.findCompletedOrderId("1", "10")).thenReturn(Optional.empty());

        var command = new CreateProductRatingCommand("10", 5, "非常好的商品");

        assertThatThrownBy(() -> commandHandler.createReview("1", command))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("仅可评价已完成订单中的资产");

        verify(productRatingRepository, never()).save(any());
    }

    @Test
    @DisplayName("同一订单重复评价时拒绝")
    void createReview_whenAlreadyReviewed_throws() {
        when(completedOrderPort.findCompletedOrderId("1", "10")).thenReturn(Optional.of("200"));
        when(productRatingRepository.existsByUserIdAndOrderId("1", "200")).thenReturn(true);

        var command = new CreateProductRatingCommand("10", 5, "非常好的商品");

        assertThatThrownBy(() -> commandHandler.createReview("1", command))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("该订单已评价");

        verify(productRatingRepository, never()).save(any());
    }

    @Test
    @DisplayName("删除自己的评价应调用软删除")
    void deleteReview_ownReview_shouldSoftDelete() {
        ProductRating rating = ProductRating.create("100", "10", "1", "200", 4, "不错");
        when(productRatingRepository.findById("100")).thenReturn(Optional.of(rating));

        commandHandler.deleteReview("1", "100");

        verify(productRatingRepository).update(argThat(r -> r.getStatus() == 0));
    }

    @Test
    @DisplayName("删除不存在的评价应抛出异常")
    void deleteReview_notFound_shouldThrow() {
        when(productRatingRepository.findById("999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commandHandler.deleteReview("1", "999"))
                .isInstanceOf(ProductDomainException.class)
                .hasMessageContaining("评价不存在");

        verify(productRatingRepository, never()).update(any());
    }

    @Test
    @DisplayName("删除他人的评价应抛出异常")
    void deleteReview_notOwner_shouldThrow() {
        ProductRating rating = ProductRating.create("100", "10", "1", "200", 4, "不错");
        when(productRatingRepository.findById("100")).thenReturn(Optional.of(rating));

        assertThatThrownBy(() -> commandHandler.deleteReview("2", "100"))
                .isInstanceOf(ProductDomainException.class)
                .hasMessageContaining("只能删除自己的评价");

        verify(productRatingRepository, never()).update(any());
    }

    @Test
    @DisplayName("点赞评价应增加点赞数")
    void likeReview_shouldIncrementLikes() {
        ProductRating rating = ProductRating.create("100", "10", "1", "200", 4, "不错");
        when(productRatingRepository.findById("100")).thenReturn(Optional.of(rating));

        commandHandler.likeReview("100");

        verify(productRatingRepository).update(argThat(r -> r.getLikes() == 1));
    }
}
