package com.cartethyia.easyorange.common.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BaseDomainEventTest {

    @Nested
    @DisplayName("eventType()")
    class EventTypeTests {

        @Test
        @DisplayName("strips Event suffix from class name")
        void stripsEventSuffix() {
            var event = new TestProductCreatedEvent();
            assertThat(event.eventType()).isEqualTo("TestProductCreated");
        }

        @Test
        @DisplayName("returns simple name for class without Event suffix")
        void noEventSuffix() {
            var event = new TestDomainNotification();
            assertThat(event.eventType()).isEqualTo("TestDomainNotification");
        }

        @Test
        @DisplayName("handles single-word class name")
        void singleWordClassName() {
            var event = new TestLogin();
            assertThat(event.eventType()).isEqualTo("TestLogin");
        }
    }

    @Nested
    @DisplayName("Base fields")
    class BaseFieldTests {

        @Test
        @DisplayName("eventId is non-null UUID")
        void eventIdIsNonNull() {
            var event = new TestProductCreatedEvent();
            assertThat(event.getEventId()).isNotNull();
            assertThat(event.getEventId()).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
        }

        @Test
        @DisplayName("occurredOn is non-null")
        void occurredOnIsNonNull() {
            var event = new TestProductCreatedEvent();
            assertThat(event.getOccurredOn()).isNotNull();
        }

        @Test
        @DisplayName("two events have different eventIds")
        void differentEventIds() {
            var event1 = new TestProductCreatedEvent();
            var event2 = new TestProductCreatedEvent();
            assertThat(event1.getEventId()).isNotEqualTo(event2.getEventId());
        }
    }

    // Test helper classes
    static class TestProductCreatedEvent extends BaseDomainEvent {}
    static class TestDomainNotification extends BaseDomainEvent {}
    static class TestLogin extends BaseDomainEvent {}
}
