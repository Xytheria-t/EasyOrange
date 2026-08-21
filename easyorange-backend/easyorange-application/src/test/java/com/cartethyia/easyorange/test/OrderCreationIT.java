package com.cartethyia.easyorange.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.order.application.command.CreateOrderCommand;
import com.cartethyia.easyorange.order.application.command.CreateOrderCommand.CreateOrderItem;
import com.cartethyia.easyorange.order.application.command.CreateOrderResult;
import com.cartethyia.easyorange.order.application.command.OrderCommandHandler;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * 下单主链路集成测试 —— 真实 MySQL（订单/库存/支付单原子提交）+ Redis（Redisson 分布式锁）+ RabbitMQ（Outbox）。
 * <p>
 * 兜住 ADR-0007「拒绝 Saga：本地单事务 + 分布式锁 + Outbox」的关键不变量：
 * <ul>
 *   <li>下单成功：订单/订单项/支付单同事务落库，库存同事务扣减，OrderCreatedEvent 进入 EVENT_PUBLICATION</li>
 *   <li>任一步失败整体回滚：库存不足时不留订单残留</li>
 *   <li>并发防超卖：库存 1 件、8 个买家并发，恰好成交 1 单</li>
 * </ul>
 */
@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("it")
class OrderCreationIT {

    @Autowired
    private OrderCommandHandler orderCommandHandler;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String sellerId;
    private String buyerId;
    private String productId;
    private String categoryId;

    @AfterEach
    void cleanup() {
        if (buyerId != null) {
            List<String> orderIds =
                    jdbcTemplate.queryForList("SELECT id FROM eo_order WHERE buyer_id = ?", String.class, buyerId);
            for (String orderId : orderIds) {
                jdbcTemplate.update("DELETE FROM eo_payment WHERE order_id = ?", orderId);
                jdbcTemplate.update("DELETE FROM eo_order_item WHERE order_id = ?", orderId);
                jdbcTemplate.update("DELETE FROM EVENT_PUBLICATION WHERE serialized_event LIKE ?", "%" + orderId + "%");
            }
            jdbcTemplate.update("DELETE FROM eo_order WHERE buyer_id = ?", buyerId);
        }
        if (productId != null) {
            jdbcTemplate.update("DELETE FROM eo_product_detail WHERE product_id = ?", productId);
            jdbcTemplate.update("DELETE FROM eo_product WHERE id = ?", productId);
            jdbcTemplate.update("DELETE FROM eo_category WHERE id = ?", categoryId);
        }
        if (sellerId != null) {
            jdbcTemplate.update("DELETE FROM eo_user WHERE user_id = ?", sellerId);
        }
        if (buyerId != null) {
            jdbcTemplate.update("DELETE FROM eo_user WHERE user_id = ?", buyerId);
        }
    }

    @Test
    @DisplayName("下单主链路：订单/订单项/支付单同事务落库，库存扣减，Outbox 事件写入")
    void createOrder_happyPath_atomicWriteAcrossTables() {
        seedFixture(new BigDecimal("199.00"), 5);

        CreateOrderResult result = orderCommandHandler.handle(
                buyerId,
                new CreateOrderCommand(
                        List.of(new CreateOrderItem(productId, 2)), "北京市海淀区", "13800138000", "IT", "WECHAT"));

        var order = jdbcTemplate.queryForMap(
                "SELECT status, total_amount, seller_id FROM eo_order WHERE id = ?", result.orderId());
        assertThat(order.get("status")).isEqualTo("PENDING_PAYMENT");
        assertThat((BigDecimal) order.get("total_amount")).isEqualByComparingTo("398.00");
        assertThat(order.get("seller_id")).isEqualTo(sellerId);

        var itemCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM eo_order_item WHERE order_id = ?", Integer.class, result.orderId());
        assertThat(itemCount).isEqualTo(1);

        var payment =
                jdbcTemplate.queryForMap("SELECT status, amount FROM eo_payment WHERE order_id = ?", result.orderId());
        assertThat(payment.get("status")).isEqualTo("PENDING");
        assertThat((BigDecimal) payment.get("amount")).isEqualByComparingTo("398.00");

        assertThat(stock()).isEqualTo(3);

        var outboxCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM EVENT_PUBLICATION WHERE event_type LIKE '%OrderCreatedEvent%' AND serialized_event LIKE ?",
                Integer.class, "%" + result.orderId() + "%");
        assertThat(outboxCount).as("OrderCreatedEvent 必须进入 Outbox（与应用事务同原子）").isEqualTo(1);
    }

    @Test
    @DisplayName("库存不足：领域异常抛出且不留任何订单/支付残留（本地事务整体回滚）")
    void createOrder_insufficientStock_rollsBackEverything() {
        seedFixture(new BigDecimal("99.00"), 1);

        assertThatThrownBy(() -> orderCommandHandler.handle(
                        buyerId,
                        new CreateOrderCommand(
                                List.of(new CreateOrderItem(productId, 2)), "北京市海淀区", "13800138000", null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("库存");

        assertThat(jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM eo_order WHERE buyer_id = ?", Integer.class, buyerId))
                .isEqualTo(0);
        assertThat(jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM eo_payment WHERE user_id = ?", Integer.class, buyerId))
                .isEqualTo(0);
        assertThat(stock()).isEqualTo(1);
    }

    @Test
    @DisplayName("并发防超卖：库存 1 件、8 个买家并发下单，恰好成交 1 单")
    void createOrder_concurrentBuyers_noOversell() throws Exception {
        seedFixture(new BigDecimal("499.00"), 1);
        int buyers = 8;
        List<String> buyerIds = new java.util.ArrayList<>();
        for (int i = 0; i < buyers; i++) {
            String id = "it-b-" + UUID.randomUUID().toString().substring(0, 8) + "-" + i;
            jdbcTemplate.update(
                    "INSERT INTO eo_user (user_id, username, password) VALUES (?, ?, ?)",
                    id,
                    "itbuyer" + i,
                    "$2a$10$ittest");
            buyerIds.add(id);
        }

        ExecutorService pool = Executors.newFixedThreadPool(buyers);
        CountDownLatch ready = new CountDownLatch(buyers);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        try {
            for (String bid : buyerIds) {
                pool.submit(() -> {
                    ready.countDown();
                    try {
                        start.await();
                        orderCommandHandler.handle(
                                bid,
                                new CreateOrderCommand(
                                        List.of(new CreateOrderItem(productId, 1)),
                                        "上海市浦东新区",
                                        "13900139000",
                                        null,
                                        null));
                        success.incrementAndGet();
                    } catch (Exception e) {
                        rejected.incrementAndGet();
                    }
                    return null;
                });
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            pool.shutdown();
            assertThat(pool.awaitTermination(60, TimeUnit.SECONDS))
                    .as("并发下单必须全部结束")
                    .isTrue();
        } finally {
            pool.shutdownNow();
        }

        assertThat(success.get()).as("恰好 1 个买家成交").isEqualTo(1);
        assertThat(rejected.get()).isEqualTo(buyers - 1);
        assertThat(stock()).isEqualTo(0);
        int orderCount = 0;
        for (String bid : buyerIds) {
            orderCount +=
                    jdbcTemplate.queryForObject("SELECT COUNT(*) FROM eo_order WHERE buyer_id = ?", Integer.class, bid);
        }
        assertThat(orderCount).isEqualTo(1);

        // 并发买家的订单/支付残留清理
        for (String bid : buyerIds) {
            List<String> orderIds =
                    jdbcTemplate.queryForList("SELECT id FROM eo_order WHERE buyer_id = ?", String.class, bid);
            for (String orderId : orderIds) {
                jdbcTemplate.update("DELETE FROM eo_payment WHERE order_id = ?", orderId);
                jdbcTemplate.update("DELETE FROM eo_order_item WHERE order_id = ?", orderId);
                jdbcTemplate.update("DELETE FROM EVENT_PUBLICATION WHERE serialized_event LIKE ?", "%" + orderId + "%");
            }
            jdbcTemplate.update("DELETE FROM eo_order WHERE buyer_id = ?", bid);
            jdbcTemplate.update("DELETE FROM eo_user WHERE user_id = ?", bid);
        }
    }

    @Test
    @DisplayName("买家即资产方：领域不变量拒绝且无残留")
    void createOrder_buyerEqualsSeller_rejected() {
        seedFixture(new BigDecimal("59.00"), 3);

        assertThatThrownBy(() -> orderCommandHandler.handle(
                        sellerId,
                        new CreateOrderCommand(
                                List.of(new CreateOrderItem(productId, 1)), "北京市海淀区", "13800138000", null, null)))
                .isInstanceOf(Exception.class);
        assertThat(jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM eo_order WHERE buyer_id = ?", Integer.class, sellerId))
                .isEqualTo(0);
        assertThat(stock()).isEqualTo(3);
    }

    // ==================== fixtures ====================

    private void seedFixture(BigDecimal price, int stock) {
        String run = UUID.randomUUID().toString().substring(0, 8);
        sellerId = "it-seller-" + run;
        buyerId = "it-buyer-" + run;
        productId = "it-prod-" + run;
        categoryId = "it-cat-" + run;

        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update(
                "INSERT INTO eo_user (user_id, username, password, create_time, update_time) VALUES (?, ?, ?, ?, ?)",
                sellerId,
                "itseller" + run,
                "$2a$10$ittest)",
                now,
                now);
        jdbcTemplate.update(
                "INSERT INTO eo_user (user_id, username, password, create_time, update_time) VALUES (?, ?, ?, ?, ?)",
                buyerId,
                "itbuyer" + run,
                "$2a$10$ittest)",
                now,
                now);
        jdbcTemplate.update(
                "INSERT INTO eo_category (id, name, create_time, update_time) VALUES (?, ?, ?, ?)",
                categoryId,
                "IT 分类-" + run,
                now,
                now);
        jdbcTemplate.update(
                "INSERT INTO eo_product (id, user_id, category_id, name, price, stock, status, create_time, update_time) "
                        + "VALUES (?, ?, ?, ?, ?, ?, 'ONLINE', ?, ?)",
                productId,
                sellerId,
                categoryId,
                "IT 下单链路测试资产-" + run,
                price,
                stock,
                now,
                now);
        jdbcTemplate.update(
                "INSERT INTO eo_product_detail (product_id, description) VALUES (?, ?)", productId, "IT 集成测试商品描述");
    }

    private int stock() {
        return jdbcTemplate.queryForObject("SELECT stock FROM eo_product WHERE id = ?", Integer.class, productId);
    }
}
