package com.cartethyia.easyorange.product.adapter.inbound.job;

import com.cartethyia.easyorange.product.application.command.ProductCommandService;
import com.cartethyia.easyorange.product.domain.aggregate.Product;
import com.cartethyia.easyorange.product.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 阶梯降价定时任务。
 * <p>
 * 每天凌晨 2 点对 AI 托管且上架中的商品执行阶梯降价检查，
 * 根据上架时间自动降低价格至对应阶梯等级。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductPriceAdjustTask {

    private final ProductRepository productRepository;
    private final ProductCommandService productCommandService;

    /**
     * 每天凌晨 2 点执行阶梯降价检查。
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void executePriceAdjustment() {
        List<Product> aiManagedProducts = productRepository.findAiManagedOnline();
        log.info("AI托管商品阶梯降价检查: 共{}个商品", aiManagedProducts.size());

        for (Product product : aiManagedProducts) {
            try {
                int expectedLevel = product.calculateExpectedPriceLevel();
                int currentLevel = product.getCurrentPriceLevel() != null
                        ? product.getCurrentPriceLevel() : 0;

                if (expectedLevel > currentLevel) {
                    productCommandService.adjustPrice(product.getId().value(), expectedLevel);
                    log.info("商品{}降价: 阶梯{}→{}", product.getId().value(), currentLevel, expectedLevel);
                }
            } catch (Exception e) {
                log.error("商品{}降价失败", product.getId().value(), e);
            }
        }
    }
}
