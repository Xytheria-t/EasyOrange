package com.cartethyia.easyorange.adapter.outbound.favorite;

import com.cartethyia.easyorange.favorite.domain.port.PriceDropNotificationPort;
import com.cartethyia.easyorange.message.application.command.MessageCommandHandler;
import com.cartethyia.easyorange.message.application.command.SendSystemMessageCommand;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 收藏降价通知适配器 — favorite 模块的 {@link PriceDropNotificationPort} 落地为站内信。
 * <p>
 * 收藏模块不感知 message 模块，跨模块通知统一经本适配器（ACL），与 ADR-0006 端口通信约定一致。
 */
@Component
@RequiredArgsConstructor
public class FavoritePriceDropNotificationAdapter implements PriceDropNotificationPort {

    private final MessageCommandHandler messageCommandHandler;

    @Override
    public void sendPriceDropNotification(
            String userId, String productId, String productName, BigDecimal oldPrice, BigDecimal newPrice) {
        String content = "你收藏的《%s》降价了：¥%s → ¥%s（省 ¥%s）"
                .formatted(productName, plain(oldPrice), plain(newPrice), plain(oldPrice.subtract(newPrice)));
        messageCommandHandler.handle(new SendSystemMessageCommand(userId, "收藏降价提醒", content, productId));
    }

    private static String plain(BigDecimal value) {
        return value.setScale(2).stripTrailingZeros().toPlainString();
    }
}
