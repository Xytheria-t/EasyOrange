package com.cartethyia.easyorange.common.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
            var event = new TestRecordEvent("foo");
            assertThat(event.eventType()).isEqualTo("TestRecord");
            assertThat(event.value()).isEqualTo("foo");
        }
    }

    // Test helper classes
    static class TestProductCreatedEvent implements DomainEvent {}
    static class TestDomainNotification implements DomainEvent {}
    static class TestLogin implements DomainEvent {}

    record TestRecordEvent(String value) implements DomainEvent {}
}
