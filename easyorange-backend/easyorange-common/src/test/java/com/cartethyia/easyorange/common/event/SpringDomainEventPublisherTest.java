package com.cartethyia.easyorange.common.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * {@link SpringDomainEventPublisher} 单元测试
 *
 * @author cartethyia
 */
@DisplayName("SpringDomainEventPublisher Tests")
class SpringDomainEventPublisherTest {

    private final ApplicationEventPublisher mockPublisher = mock(ApplicationEventPublisher.class);
    private final SpringDomainEventPublisher publisher = new SpringDomainEventPublisher(mockPublisher);

    @Nested
    @DisplayName("Publish Tests")
    class PublishTests {

        @Test
        @DisplayName("publish with null event should throw NPE")
        void publish_withNullEvent_shouldThrowNPE() {
            // Act & Assert
            assertThatThrownBy(() -> publisher.publish(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Event cannot be null");
        }

        @Test
        @DisplayName("publish with valid event should call publisher")
        void publish_withValidEvent_shouldCallPublisher() {
            // Arrange
            TestEvent event = new TestEvent();

            // Act
            publisher.publish(event);

            // Assert
            verify(mockPublisher, times(1)).publishEvent(event);
        }

        @Test
        @DisplayName("publish should publish exactly one event")
        void publish_shouldPublishExactlyOneEvent() {
            // Arrange
            TestEvent event = new TestEvent();

            // Act
            publisher.publish(event);
            publisher.publish(event);

            // Assert
            verify(mockPublisher, times(2)).publishEvent(any(BaseDomainEvent.class));
        }
    }

    @Nested
    @DisplayName("PublishAll Tests")
    class PublishAllTests {

        @Test
        @DisplayName("publishAll with null list should not throw")
        void publishAll_withNullList_shouldNotThrow() {
            // Act & Assert
            org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> publisher.publishAll(null));
            verify(mockPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("publishAll with empty list should not publish")
        void publishAll_withEmptyList_shouldNotPublish() {
            // Arrange
            List<BaseDomainEvent> events = List.of();

            // Act
            publisher.publishAll(events);

            // Assert
            verify(mockPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("publishAll should publish each event")
        void publishAll_shouldPublishEachEvent() {
            // Arrange
            TestEvent event1 = new TestEvent();
            TestEvent event2 = new TestEvent();
            List<BaseDomainEvent> events = List.of(event1, event2);

            // Act
            publisher.publishAll(events);

            // Assert
            verify(mockPublisher, times(1)).publishEvent(event1);
            verify(mockPublisher, times(1)).publishEvent(event2);
        }

        @Test
        @DisplayName("publishAll with single event should publish")
        void publishAll_withSingleEvent_shouldPublish() {
            // Arrange
            TestEvent event = new TestEvent();
            List<BaseDomainEvent> events = List.of(event);

            // Act
            publisher.publishAll(events);

            // Assert
            verify(mockPublisher, times(1)).publishEvent(event);
        }
    }

    /**
     * 测试用的领域事件实现
     */
    private static class TestEvent extends BaseDomainEvent {
        public TestEvent() {
            super(TestEvent.class);
        }

        @Override
        public String eventType() {
            return "TEST_EVENT";
        }
    }
}
