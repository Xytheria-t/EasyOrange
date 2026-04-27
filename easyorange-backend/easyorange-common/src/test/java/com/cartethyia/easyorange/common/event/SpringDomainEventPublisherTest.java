package com.cartethyia.easyorange.common.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SpringDomainEventPublisher Tests")
class SpringDomainEventPublisherTest {

    private static class StubApplicationEventPublisher implements ApplicationEventPublisher {
        private final List<Object> publishedEvents = new ArrayList<>();

        @Override
        public void publishEvent(Object event) {
            publishedEvents.add(event);
        }

        public List<Object> getPublishedEvents() {
            return publishedEvents;
        }

        public void clear() {
            publishedEvents.clear();
        }
    }

    private SpringDomainEventPublisher createPublisher(StubApplicationEventPublisher stub) {
        return new SpringDomainEventPublisher(stub);
    }

    @Nested
    @DisplayName("Publish Tests")
    class PublishTests {

        @Test
        @DisplayName("publish with null event should throw NPE")
        void publish_withNullEvent_shouldThrowNPE() {
            StubApplicationEventPublisher stub = new StubApplicationEventPublisher();
            SpringDomainEventPublisher publisher = createPublisher(stub);

            assertThatThrownBy(() -> publisher.publish(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Event cannot be null");
        }

        @Test
        @DisplayName("publish with valid event should call publisher")
        void publish_withValidEvent_shouldCallPublisher() {
            StubApplicationEventPublisher stub = new StubApplicationEventPublisher();
            SpringDomainEventPublisher publisher = createPublisher(stub);
            TestEvent event = new TestEvent();

            publisher.publish(event);

            assertThat(stub.getPublishedEvents()).containsExactly(event);
        }

        @Test
        @DisplayName("publish should publish exactly one event")
        void publish_shouldPublishExactlyOneEvent() {
            StubApplicationEventPublisher stub = new StubApplicationEventPublisher();
            SpringDomainEventPublisher publisher = createPublisher(stub);
            TestEvent event = new TestEvent();

            publisher.publish(event);
            publisher.publish(event);

            assertThat(stub.getPublishedEvents()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("PublishAll Tests")
    class PublishAllTests {

        @Test
        @DisplayName("publishAll with null list should not throw")
        void publishAll_withNullList_shouldNotThrow() {
            StubApplicationEventPublisher stub = new StubApplicationEventPublisher();
            SpringDomainEventPublisher publisher = createPublisher(stub);

            org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> publisher.publishAll(null));
            assertThat(stub.getPublishedEvents()).isEmpty();
        }

        @Test
        @DisplayName("publishAll with empty list should not publish")
        void publishAll_withEmptyList_shouldNotPublish() {
            StubApplicationEventPublisher stub = new StubApplicationEventPublisher();
            SpringDomainEventPublisher publisher = createPublisher(stub);
            List<BaseDomainEvent> events = List.of();

            publisher.publishAll(events);

            assertThat(stub.getPublishedEvents()).isEmpty();
        }

        @Test
        @DisplayName("publishAll should publish each event")
        void publishAll_shouldPublishEachEvent() {
            StubApplicationEventPublisher stub = new StubApplicationEventPublisher();
            SpringDomainEventPublisher publisher = createPublisher(stub);
            TestEvent event1 = new TestEvent();
            TestEvent event2 = new TestEvent();
            List<BaseDomainEvent> events = List.of(event1, event2);

            publisher.publishAll(events);

            assertThat(stub.getPublishedEvents()).containsExactly(event1, event2);
        }

        @Test
        @DisplayName("publishAll with single event should publish")
        void publishAll_withSingleEvent_shouldPublish() {
            StubApplicationEventPublisher stub = new StubApplicationEventPublisher();
            SpringDomainEventPublisher publisher = createPublisher(stub);
            TestEvent event = new TestEvent();
            List<BaseDomainEvent> events = List.of(event);

            publisher.publishAll(events);

            assertThat(stub.getPublishedEvents()).containsExactly(event);
        }
    }

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