package com.cartethyia.easyorange.user.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventPublisher {

    private final DomainEventPublisher domainEventPublisher;

    public <T, E extends BaseDomainEvent> void publishAfterTransaction(T result, EventExtractor<T, E> extractor) {
        E event = extractor.extract(result);
        publishAfterCommit(event);
    }

    public void publishAfterCommit(BaseDomainEvent event) {
        TransactionSynchronizationManager.registerSynchronization(
            new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    domainEventPublisher.publish(event);
                    log.debug("Published event {} after transaction commit", event.eventType());
                }
            }
        );
    }

    public void publishImmediately(BaseDomainEvent event) {
        domainEventPublisher.publish(event);
        log.debug("Published event {} immediately", event.eventType());
    }

    private interface TransactionSynchronization extends org.springframework.transaction.support.TransactionSynchronization {
        @Override
        default void suspend() {}
        
        @Override
        default void resume() {}
        
        @Override
        default void beforeCommit(boolean readOnly) {}
        
        @Override
        default void beforeCompletion() {}
        
        @Override
        default void afterCompletion(int status) {}
    }
}
