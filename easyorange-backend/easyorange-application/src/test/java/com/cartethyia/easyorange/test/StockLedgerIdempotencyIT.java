package com.cartethyia.easyorange.test;

import static org.assertj.core.api.Assertions.assertThat;

import com.cartethyia.easyorange.product.application.command.CreateProductCommand;
import com.cartethyia.easyorange.product.application.command.ProductCommandHandler;
import com.cartethyia.easyorange.product.domain.enums.StockChangeType;
import com.cartethyia.easyorange.product.domain.repository.StockLedgerRepository;
import com.cartethyia.easyorange.product.domain.valueobject.StockDrift;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 库存流水幂等集成测试 —— 真实 MySQL，验证「重复投递只生效一次」这条不变量落在数据库唯一键上。
 * <p>
 * 覆盖三个此前会静默出错的场景：
 * <ul>
 *   <li>同一订单重复扣减 / 恢复：库存不被二次加减（流水唯一键抢占落账权）</li>
 *   <li>恢复数量取下单明细而非固定 1：多件订单取消后库存能回到原值</li>
 *   <li>对账能发现绕过流水落账的库存变更（余额与流水快照比对）</li>
 * </ul>
 */
@DisplayName("库存流水幂等集成测试")
class StockLedgerIdempotencyIT extends AbstractIntegrationTest {

    @Autowired
    private ProductCommandHandler productCommandHandler;

    @Autowired
    private StockLedgerRepository stockLedgerRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String sellerId;
    private String productId;

    @AfterEach
    void cleanup() {
        if (productId != null) {
            jdbcTemplate.update("DELETE FROM EVENT_PUBLICATION WHERE serialized_event LIKE ?", "%" + productId + "%");
            jdbcTemplate.update("DELETE FROM eo_stock_ledger WHERE product_id = ?", productId);
            jdbcTemplate.update("DELETE FROM eo_product_detail WHERE product_id = ?", productId);
            jdbcTemplate.update("DELETE FROM eo_product_image WHERE product_id = ?", productId);
            jdbcTemplate.update("DELETE FROM eo_product WHERE id = ?", productId);
        }
        if (sellerId != null) {
            jdbcTemplate.update("DELETE FROM eo_user WHERE user_id = ?", sellerId);
        }
    }

    @Test
    @DisplayName("同一订单重复扣减/恢复只生效一次，且恢复数量取下单明细")
    void duplicateStockChanges_applyExactlyOnce() {
        seedProduct(10);
        String orderId = UUID.randomUUID().toString();

        productCommandHandler.decrementStock(orderId, productId, 2);
        productCommandHandler.decrementStock(orderId, productId, 2);

        assertThat(stockOf(productId)).isEqualTo(8);
        assertThat(ledgerCount(orderId, productId, StockChangeType.DECREASE)).isEqualTo(1);

        productCommandHandler.restoreStock(orderId, productId, 2);
        productCommandHandler.restoreStock(orderId, productId, 2);

        assertThat(stockOf(productId)).isEqualTo(10);
        assertThat(ledgerCount(orderId, productId, StockChangeType.RESTORE)).isEqualTo(1);
        assertThat(stockLedgerRepository.findDrifts(500))
                .extracting(StockDrift::productId)
                .doesNotContain(productId);
    }

    @Test
    @DisplayName("绕过流水直接改库存会被对账扫出来（对账不是恒过的空检查）")
    void reconcile_reportsDrift_whenStockChangedOutsideLedger() {
        seedProduct(10);

        jdbcTemplate.update("UPDATE eo_product SET stock = 4 WHERE id = ?", productId);

        assertThat(stockLedgerRepository.findDrifts(500))
                .extracting(StockDrift::productId)
                .contains(productId);
    }

    private void seedProduct(int stock) {
        // user_id / product_id 均为 VARCHAR(36)，测试数据直接用 UUID 本体，不加前缀（超长会被 MySQL 截断报错）
        sellerId = UUID.randomUUID().toString();
        productId = productCommandHandler.createProduct(
                sellerId,
                new CreateProductCommand(
                        "2",
                        "IT 库存流水资产",
                        new BigDecimal("10.00"),
                        null,
                        stock,
                        "1",
                        "北京",
                        "微信",
                        "IT",
                        List.of("http://img/it.jpg")));
    }

    private int stockOf(String id) {
        return jdbcTemplate.queryForObject("SELECT stock FROM eo_product WHERE id = ?", Integer.class, id);
    }

    private int ledgerCount(String orderId, String id, StockChangeType changeType) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM eo_stock_ledger WHERE biz_id = ? AND product_id = ? AND change_type = ?",
                Integer.class,
                orderId,
                id,
                changeType.getCode());
    }
}
