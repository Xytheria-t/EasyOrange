package com.cartethyia.easyorange.common.event;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class DomainEventTest {

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
    @DisplayName("Interface default method behavior")
    class DefaultMethodTests {

        @Test
        @DisplayName("record implementing DomainEvent gets correct eventType")
        void recordImplementsDomainEvent() {
            var event = new TestRecordEvent("evt-1", "foo");
            assertThat(event.eventType()).isEqualTo("TestRecord");
            assertThat(event.value()).isEqualTo("foo");
        }
    }

    // Test helper classes
    static class TestProductCreatedEvent implements DomainEvent {
        @Override
        public String eventId() {
            return "evt-1";
        }

        @Override
        public String aggregateId() {
            return "agg-1";
        }
    }

    static class TestDomainNotification implements DomainEvent {
        @Override
        public String eventId() {
            return "evt-2";
        }

        @Override
        public String aggregateId() {
            return "agg-2";
        }
    }

    static class TestLogin implements DomainEvent {
        @Override
        public String eventId() {
            return "evt-3";
        }

        @Override
        public String aggregateId() {
            return "user-1";
        }
    }

    record TestRecordEvent(String eventId, String value) implements DomainEvent {
        @Override
        public String aggregateId() {
            return value;
        }
    }
}
