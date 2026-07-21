package com.cartethyia.easyorange.adapter.event;

import com.cartethyia.easyorange.ai.dto.CopyGenerationResult;
import com.cartethyia.easyorange.ai.dto.PricingSuggestion;
import com.cartethyia.easyorange.ai.service.AiCopyGenerationService;
import com.cartethyia.easyorange.ai.service.AiPricingService;
import com.cartethyia.easyorange.common.event.DomainEvent;
import com.cartethyia.easyorange.framework.event.core.AbstractDomainEventConsumer;
import com.cartethyia.easyorange.framework.event.idempotency.EventIdempotencyChecker;
import com.cartethyia.easyorange.framework.event.metrics.EventMetricsService;
import com.cartethyia.easyorange.framework.event.metadata.EventMetadata;
import com.cartethyia.easyorange.framework.messaging.config.RabbitMQConfig;
import com.cartethyia.easyorange.product.domain.event.ProductCreatedEvent;
import com.cartethyia.easyorange.product.domain.event.ProductMarkedSoldEvent;
import com.cartethyia.easyorange.product.domain.event.ProductUpdatedEvent;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * AI 商品事件消费者 — 异步响应商品生命周期事件并触发 AI 处理。
 * <p>
 * 监听 {@code ProductCreatedEvent} / {@code ProductUpdatedEvent} / {@code ProductMarkedSoldEvent}，
 * 分别触发智能估值、营销文案生成、缓存清理等 AI 操作。
 * 结果通过 Redis 缓存供 API 层快速读取。
 * <p>
 * 核心设计原则：
 * <ul>
 *   <li>AI 操作天然幂等（重复估值/文案生成不影响结果），所以关闭框架级幂等检查</li>
 *   <li>Redis 缓存 TTL 统一 24h，售出后立即清理释放内存</li>
 *   <li>LLM 调用失败不抛异常（降级为静默跳过），不影响主流程</li>
 * </ul>
 */
@Component
@ConditionalOnProperty(prefix = "easyorange.rabbitmq", name = "enabled", havingValue = "true", matchIfMissing = true)
@RabbitListener(queues = RabbitMQConfig.QUEUE_AI_PRODUCT, containerFactory = "domainEventContainerFactory")
public class AiProductEventConsumer extends AbstractDomainEventConsumer {

    private final AiPricingService pricingService;
    private final AiCopyGenerationService copyGenerationService;
    private final RedisTemplate<Object, Object> redisTemplate;

    public AiProductEventConsumer(EventIdempotencyChecker idempotencyChecker,
                                  EventMetricsService metricsService,
                                  AiPricingService pricingService,
                                  AiCopyGenerationService copyGenerationService,
                                  RedisTemplate<Object, Object> redisTemplate) {
        super(idempotencyChecker, metricsService, false);
        this.pricingService = pricingService;
        this.copyGenerationService = copyGenerationService;
        this.redisTemplate = redisTemplate;
    }

    @RabbitHandler
    public void onProductCreated(ProductCreatedEvent event, Message message) {
        handle(event, message);
    }

    @RabbitHandler
    public void onProductUpdated(ProductUpdatedEvent event, Message message) {
        handle(event, message);
    }

    @RabbitHandler
    public void onProductMarkedSold(ProductMarkedSoldEvent event, Message message) {
        handle(event, message);
    }

    @Override
    protected void doHandle(DomainEvent event, EventMetadata metadata) {
        switch (event) {
            case ProductCreatedEvent e -> handleProductCreated(e);
            case ProductUpdatedEvent e -> handleProductUpdated(e);
            case ProductMarkedSoldEvent e -> handleProductMarkedSold(e);
            default -> throw new IllegalStateException("不支持的事件: " + event.getClass());
        }
    }

    private void handleProductCreated(ProductCreatedEvent e) {
        var suggestion = pricingService.suggestPrice(
                e.name(), e.description(), null, e.conditionLevel(), e.originalPrice());
        if (suggestion != null) {
            redisTemplate.opsForValue().set(
                    "eo:ai:valuation:" + e.productId(), suggestion, 24, TimeUnit.HOURS);
            log.info("event=product_created_valuated productId={} suggestedPrice={}",
                    e.productId(), suggestion.suggestedPrice());
        }
    }

    private void handleProductUpdated(ProductUpdatedEvent e) {
        var copyResult = copyGenerationService.generateCopy(
                e.name(), null, e.conditionLevel(),
                e.originalPrice() != null ? e.originalPrice().toString() : null,
                "standard");
        if (copyResult != null) {
            redisTemplate.opsForValue().set(
                    "eo:ai:copy:" + e.productId(), copyResult, 24, TimeUnit.HOURS);
            log.info("event=product_updated_copy_generated productId={} copyTitle={}",
                    e.productId(), copyResult.title());
        }
        // 商品更新后旧估值结果不再准确，清理缓存
        redisTemplate.delete("eo:ai:valuation:" + e.productId());
    }

    private void handleProductMarkedSold(ProductMarkedSoldEvent e) {
        log.info("event=product_marked_sold productId={} sellerId={} action=record_sale_price",
                e.productId(), e.sellerId());
        // 售出后清理所有 AI 缓存
        redisTemplate.delete("eo:ai:valuation:" + e.productId());
        redisTemplate.delete("eo:ai:copy:" + e.productId());
    }
}
