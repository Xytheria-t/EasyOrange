package com.cartethyia.easyorange.adapter.outbound.product;

import com.cartethyia.easyorange.message.application.command.MessageCommandHandler;
import com.cartethyia.easyorange.message.application.command.SendSystemMessageCommand;
import com.cartethyia.easyorange.product.domain.port.ProductNotificationPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductNotificationAdapter implements ProductNotificationPort {

    private final MessageCommandHandler messageCommandHandler;

    @Override
    public void notifyProductCreated(String productId, String userId) {
        try {
            SendSystemMessageCommand command = SendSystemMessageCommand.builder()
                    .receiverId(userId)
                    .title("商品发布成功")
                    .content("您的商品（ID: " + productId + "）已成功发布，等待管理员审核。审核通过后将自动上架。")
                    .businessId(productId)
                    .build();
            messageCommandHandler.handle(command);
            log.info("action=notify_product_created productId={} userId={}", productId, userId);
        } catch (Exception e) {
            log.error("action=notify_product_created_failed productId={} userId={}", productId, userId, e);
        }
    }

    @Override
    public void notifyProductMarkedSold(String productId, String userId) {
        try {
            SendSystemMessageCommand command = SendSystemMessageCommand.builder()
                    .receiverId(userId)
                    .title("商品已售出")
                    .content("您的商品（ID: " + productId + "）已被标记为已售出。")
                    .businessId(productId)
                    .build();
            messageCommandHandler.handle(command);
            log.info("action=notify_product_sold productId={} userId={}", productId, userId);
        } catch (Exception e) {
            log.error("action=notify_product_sold_failed productId={} userId={}", productId, userId, e);
        }
    }

    @Override
    public void notifyLowStock(String productId, String sellerId, int currentStock) {
        try {
            SendSystemMessageCommand command = SendSystemMessageCommand.builder()
                    .receiverId(sellerId)
                    .title("库存不足预警")
                    .content("您的商品（ID: " + productId + "）当前库存仅剩 " + currentStock + " 件，低于安全库存阈值，请及时补货。")
                    .businessId(productId)
                    .build();
            messageCommandHandler.handle(command);
            log.info("action=notify_low_stock productId={} sellerId={} currentStock={}", productId, sellerId, currentStock);
        } catch (Exception e) {
            log.error("action=notify_low_stock_failed productId={} sellerId={} currentStock={}", productId, sellerId, currentStock, e);
        }
    }
}