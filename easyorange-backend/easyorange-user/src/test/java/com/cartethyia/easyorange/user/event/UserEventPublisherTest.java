package com.cartethyia.easyorange.user.event;

import com.cartethyia.easyorange.common.event.BaseDomainEvent;
import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserEventPublisherTest {

    @Mock
    private DomainEventPublisher domainEventPublisher;

    private UserEventPublisher userEventPublisher;

    @BeforeEach
    void setUp() {
        userEventPublisher = new UserEventPublisher(domainEventPublisher);
    }

    @Test
    @DisplayName("应立即发布事件")
    void shouldPublishEventImmediately() {
        BaseDomainEvent mockEvent = mock(BaseDomainEvent.class);
        when(mockEvent.eventType()).thenReturn("TestEvent");

        userEventPublisher.publishImmediately(mockEvent);

        verify(domainEventPublisher).publish(mockEvent);
    }
}
