package com.cartethyia.easyorange.user.event.aspect;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.user.event.EventExtractor;
import com.cartethyia.easyorange.user.event.annotation.PublishEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class EventPublishingAspect {

    private final DomainEventPublisher domainEventPublisher;
    private final ApplicationContext applicationContext;

    @Around("@annotation(publishEvent)")
    public Object around(ProceedingJoinPoint joinPoint, PublishEvent publishEvent) throws Throwable {
        Object result = joinPoint.proceed();
        
        if (result == null) {
            return result;
        }
        
        BaseDomainEvent event = extractEvent(joinPoint, publishEvent, result);
        
        if (event != null) {
            if (publishEvent.afterTransaction()) {
                TransactionSynchronizationManager.registerSynchronization(
                    new org.springframework.transaction.support.TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            domainEventPublisher.publish(event);
                            log.debug("Published event {} after transaction commit", event.eventType());
                        }
                    }
                );
            } else {
                domainEventPublisher.publish(event);
                log.debug("Published event {} immediately", event.eventType());
            }
        }
        
        return result;
    }

    @SuppressWarnings("unchecked")
    private BaseDomainEvent extractEvent(ProceedingJoinPoint joinPoint, PublishEvent publishEvent, Object result) {
        String extractorBeanName = publishEvent.extractor();
        
        if (extractorBeanName.isEmpty()) {
            log.warn("No extractor specified for event type {}", publishEvent.type());
            return null;
        }
        
        try {
            EventExtractor<Object, BaseDomainEvent> extractor = 
                (EventExtractor<Object, BaseDomainEvent>) applicationContext.getBean(extractorBeanName);
            return extractor.extract(result);
        } catch (Exception e) {
            log.error("Failed to extract event using extractor: {}", extractorBeanName, e);
            return null;
        }
    }
}
