package com.cartethyia.easyorange.product.application.event;

import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.product.domain.entity.ProductAuditLog;
import com.cartethyia.easyorange.product.domain.enums.AuditAction;
import com.cartethyia.easyorange.product.domain.event.ProductEvent;
import com.cartethyia.easyorange.product.domain.event.ProductSubmittedForReviewEvent;
import com.cartethyia.easyorange.product.domain.port.ProductCacheEvictionPort;
import com.cartethyia.easyorange.product.domain.repository.ProductAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 商品领域事件同步监听器 — 与发布者同事务、同线程，用于需要立即生效的副作用：
 * <ul>
 *   <li>缓存失效（读后写一致性）</li>
 *   <li>审核日志持久化</li>
 * </ul>
 * <p>
 * 异步投影（CQRS / ES 索引 / 站内信等）由 {@link ProductEventConsumer} 处理。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductDomainEventListener {

    private final ProductCacheEvictionPort productCachePort;
    private final ProductAuditLogRepository auditLogRepository;

    @EventListener
    public void evictProductCache(ProductEvent event) {
        productCachePort.evictProductCache(event.productId());
    }

    @EventListener
    public void onProductSubmittedForReview(ProductSubmittedForReviewEvent event) {
        var context = SecurityContextUtil.getUserContextOrThrow();
        var auditLog = ProductAuditLog.builder()
                .productId(event.productId())
                .operatorId(event.operatorId())
                .operatorName(context.username())
                .action(AuditAction.RESUBMIT.getCode())
                .beforeStatus(event.beforeStatus().getCode())
                .afterStatus(event.afterStatus().getCode())
                .build();
        auditLogRepository.save(auditLog);
    }
}
