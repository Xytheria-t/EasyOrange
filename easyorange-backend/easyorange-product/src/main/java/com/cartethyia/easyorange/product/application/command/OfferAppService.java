package com.cartethyia.easyorange.product.application.command;

import com.cartethyia.easyorange.common.domain.Money;
import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.product.domain.aggregate.Product;
import com.cartethyia.easyorange.product.domain.enums.ConsignmentMode;
import com.cartethyia.easyorange.product.domain.event.OfferAcceptedEvent;
import com.cartethyia.easyorange.product.domain.event.OfferCounteredEvent;
import com.cartethyia.easyorange.product.domain.event.OfferRejectedEvent;
import com.cartethyia.easyorange.product.domain.exception.ProductNotFoundException;
import com.cartethyia.easyorange.product.domain.port.NegotiationContext;
import com.cartethyia.easyorange.product.domain.port.NegotiationMessagePort;
import com.cartethyia.easyorange.product.domain.port.OrderCreationPort;
import com.cartethyia.easyorange.product.domain.repository.ProductRepository;
import com.cartethyia.easyorange.product.domain.service.OfferRuleEngine;
import com.cartethyia.easyorange.product.domain.valueobject.OfferDecision;
import com.cartethyia.easyorange.product.domain.valueobject.ProductId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 议价应用服务 — 处理买家出价，协调规则引擎决策、LLM 话术生成和订单创建。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class OfferAppService {

    private final ProductRepository productRepository;
    private final OfferRuleEngine offerRuleEngine;
    private final NegotiationMessagePort negotiationMessagePort;
    private final OrderCreationPort orderCreationPort;
    private final DomainEventPublisher domainEventPublisher;

    /**
     * 处理买家出价。
     *
     * @param buyerId    买家 ID
     * @param productId  商品 ID
     * @param offerPrice 出价金额
     * @return 议价处理结果
     * @throws ProductNotFoundException 商品不存在
     * @throws IllegalStateException    商品非 AI 托管、已下架或自购
     */
    public OfferResult processOffer(Long buyerId, Long productId, BigDecimal offerPrice) {
        Product product = productRepository.findById(ProductId.of(productId))
                .orElseThrow(() -> new ProductNotFoundException(ProductId.of(productId)));

        // 校验商品是 AI 托管
        if (product.getConsignmentMode() != ConsignmentMode.AI_MANAGED) {
            throw new IllegalStateException("该商品不支持AI议价");
        }
        if (!product.getStatus().isOnline()) {
            throw new IllegalStateException("商品已下架");
        }
        // 不能买自己的商品
        if (product.getSellerId().value().equals(buyerId)) {
            throw new IllegalStateException("不能购买自己的商品");
        }

        Money offer = Money.of(offerPrice);
        Money floorPrice = product.getFloorPrice();
        Money currentPrice = product.getPrice();

        // 规则引擎决策
        OfferDecision decision = offerRuleEngine.evaluate(
                offer, floorPrice, currentPrice,
                product.getCurrentPriceLevel() != null ? product.getCurrentPriceLevel() : 0);

        // LLM 生成话术
        NegotiationContext ctx = new NegotiationContext(
                decision.type().name(),
                decision.acceptedPrice(),
                decision.counterPrice(),
                decision.reason(),
                product.getTitle().value(),
                offerPrice
        );
        String message = negotiationMessagePort.generateMessage(ctx);

        // 根据决策执行
        Long orderId = null;
        if (decision.type() == OfferDecision.DecisionType.ACCEPT) {
            orderId = orderCreationPort.createOrder(
                    buyerId, productId, decision.acceptedPrice());
            domainEventPublisher.publish(new OfferAcceptedEvent(
                    productId, product.getSellerId().value(), buyerId,
                    decision.acceptedPrice(), orderId));
            log.info("action=offer_accepted productId={} buyerId={} price={} orderId={}",
                    productId, buyerId, decision.acceptedPrice(), orderId);
        } else if (decision.type() == OfferDecision.DecisionType.REJECT) {
            domainEventPublisher.publish(new OfferRejectedEvent(
                    productId, product.getSellerId().value(), buyerId, offerPrice));
            log.info("action=offer_rejected productId={} buyerId={} offerPrice={}",
                    productId, buyerId, offerPrice);
        } else {
            domainEventPublisher.publish(new OfferCounteredEvent(
                    productId, product.getSellerId().value(), buyerId,
                    offerPrice, decision.counterPrice()));
            log.info("action=offer_countered productId={} buyerId={} offerPrice={} counterPrice={}",
                    productId, buyerId, offerPrice, decision.counterPrice());
        }

        return new OfferResult(decision.type().name(), message, decision.counterPrice(), orderId);
    }
}
