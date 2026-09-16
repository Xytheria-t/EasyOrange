package com.cartethyia.easyorange.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.product.application.command.CreateProductRatingCommand;
import com.cartethyia.easyorange.product.application.command.ProductRatingCommandHandler;
import com.cartethyia.easyorange.product.application.query.ProductRatingQueryHandler;
import com.cartethyia.easyorange.product.domain.entity.ProductRating;
import com.cartethyia.easyorange.product.domain.repository.ProductRatingRepository;
import java.math.BigDecimal;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 评价写路径落库集成测试 —— 兜住全 Mockito 单测覆盖不到的链路：
 * {@code eo_product_review.order_id} 为 NOT NULL 且有 {@code (user_id, order_id)} 唯一键，
 * 因此评价必须绑定真实成交订单（应用层经 ACL 端口按「买家 + 资产」反查订单号），
 * 且主键由应用层在持久化前生成（{@code IdType.INPUT} 不回填）。
 */
@DisplayName("评价写路径落库集成测试（真实 MySQL）")
class ReviewPersistenceIT extends AbstractIntegrationTest {

    @Autowired
    private ProductRatingCommandHandler productRatingCommandHandler;

    @Autowired
    private ProductRatingRepository productRatingRepository;

    @Autowired
    private ProductRatingQueryHandler productRatingQueryHandler;

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbc;

    @BeforeEach
    void setUpJdbc() {
        jdbc = new JdbcTemplate(dataSource);
    }

    @Test
    @DisplayName("已完成订单可评价：主键由应用层生成，评价绑定到反查到的订单")
    void reviewOnCompletedOrder_persistedWithGeneratedId() {
        OrderFixture order = insertOrderWithItem("COMPLETED");

        String reviewId = productRatingCommandHandler.createReview(
                order.buyerId(), new CreateProductRatingCommand(order.productId(), 5, "集成测试评价内容"));

        assertThat(UUID.fromString(reviewId).version()).as("主键为 UUID v7").isEqualTo(7);

        ProductRating saved = productRatingRepository.findById(reviewId).orElseThrow();
        assertThat(saved.getOrderId()).as("评价绑定到成交订单").isEqualTo(order.orderId());
        assertThat(saved.getProductId()).isEqualTo(order.productId());
        assertThat(saved.getUserId()).isEqualTo(order.buyerId());
    }

    @Test
    @DisplayName("订单未完成时拒绝评价")
    void reviewOnUnfinishedOrder_rejected() {
        OrderFixture order = insertOrderWithItem("SHIPPED");

        assertThatThrownBy(() -> productRatingCommandHandler.createReview(
                        order.buyerId(), new CreateProductRatingCommand(order.productId(), 5, "集成测试评价内容")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("仅可评价已完成订单中的资产");
    }

    @Test
    @DisplayName("同一订单重复评价被拒绝")
    void duplicateReview_rejected() {
        OrderFixture order = insertOrderWithItem("COMPLETED");
        productRatingCommandHandler.createReview(
                order.buyerId(), new CreateProductRatingCommand(order.productId(), 5, "集成测试评价内容"));

        assertThatThrownBy(() -> productRatingCommandHandler.createReview(
                        order.buyerId(), new CreateProductRatingCommand(order.productId(), 4, "集成测试二次评价")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("该订单已评价");
    }

    @Test
    @DisplayName("资格查询：成交后可评价，评价过即不可再评")
    void canReview_followsOrderAndReviewState() {
        OrderFixture order = insertOrderWithItem("COMPLETED");

        assertThat(productRatingQueryHandler.canReview(order.buyerId(), order.productId()))
                .as("有已完成订单且未评价时可评价")
                .isTrue();

        productRatingCommandHandler.createReview(
                order.buyerId(), new CreateProductRatingCommand(order.productId(), 5, "集成测试评价内容"));

        assertThat(productRatingQueryHandler.canReview(order.buyerId(), order.productId()))
                .as("已评价后入口应关闭")
                .isFalse();
    }

    private OrderFixture insertOrderWithItem(String status) {
        String orderId = UUID.randomUUID().toString();
        String productId = UUID.randomUUID().toString();
        String buyerId = UUID.randomUUID().toString();
        var price = new BigDecimal("9.90");

        jdbc.update(
                "INSERT INTO eo_order (id, order_no, buyer_id, seller_id, total_amount, status, payment_status)"
                        + " VALUES (?, ?, ?, ?, ?, ?, ?)",
                orderId,
                "IT" + orderId,
                buyerId,
                UUID.randomUUID().toString(),
                price,
                status,
                "UNPAID");
        jdbc.update(
                "INSERT INTO eo_order_item (id, order_id, product_id, product_snapshot, unit_price, subtotal)"
                        + " VALUES (?, ?, ?, ?, ?, ?)",
                UUID.randomUUID().toString(),
                orderId,
                productId,
                // 与下单写路径同形：订单列表按留痕快照展示名称与图片
                "{\"productId\":\"" + productId + "\",\"name\":\"IT 商品\",\"image\":\"\",\"price\":9.90}",
                price,
                price);
        return new OrderFixture(orderId, productId, buyerId);
    }

    private record OrderFixture(String orderId, String productId, String buyerId) {}
}
