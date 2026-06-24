package com.cartethyia.easyorange.adapter.outbound.message;

import com.cartethyia.easyorange.message.domain.port.OfferProcessingPort;
import com.cartethyia.easyorange.product.application.command.OfferAppService;
import com.cartethyia.easyorange.product.application.command.OfferResult;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 议价处理适配器 — 实现 {@link OfferProcessingPort}。
 * <p>
 * 将 message 模块的出价请求转发到 product 模块的 {@link OfferAppService}。
 */
@Primary
@Component
@RequiredArgsConstructor
public class OfferProcessingAdapter implements OfferProcessingPort {

    private final OfferAppService offerAppService;

    @Override
    public OfferProcessingPort.OfferResult processOffer(Long buyerId, Long productId, BigDecimal offerPrice) {
        com.cartethyia.easyorange.product.application.command.OfferResult result =
                offerAppService.processOffer(buyerId, productId, offerPrice);
        return new OfferProcessingPort.OfferResult(
                result.decisionType(),
                result.message(),
                result.counterPrice(),
                result.orderId()
        );
    }
}
