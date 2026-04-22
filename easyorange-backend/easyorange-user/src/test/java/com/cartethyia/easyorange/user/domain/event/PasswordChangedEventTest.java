package com.cartethyia.easyorange.user.domain.event;

import com.cartethyia.easyorange.user.domain.valueobject.UserId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PasswordChangedEvent 领域事件测试")
class PasswordChangedEventTest {

    @Nested
    @DisplayName("构造函数")
    class ConstructorTests {

        @Test
        @DisplayName("构造函数正确创建事件")
        void constructor_createsEventWithCorrectData() {
            UserId userId = UserId.of(1L);

            PasswordChangedEvent event = new PasswordChangedEvent(userId);

            assertThat(event.getUserId()).isEqualTo(userId);
        }

        @Test
        @DisplayName("事件自动生成 eventId")
        void constructor_generatesEventId() {
            PasswordChangedEvent event = new PasswordChangedEvent(UserId.of(1L));

            assertThat(event.getEventId()).isNotNull();
            assertThat(event.getEventId()).isNotEmpty();
        }

        @Test
        @DisplayName("事件自动生成 occurredOn 时间戳")
        void constructor_generatesOccurredOn() {
            PasswordChangedEvent event = new PasswordChangedEvent(UserId.of(1L));

            assertThat(event.getOccurredOn()).isNotNull();
        }

        @Test
        @DisplayName("事件聚合类型为 User")
        void constructor_setsAggregateTypeToUser() {
            PasswordChangedEvent event = new PasswordChangedEvent(UserId.of(1L));

            assertThat(event.getAggregateType()).isEqualTo("User");
        }
    }

    @Nested
    @DisplayName("eventType")
    class EventTypeTests {

        @Test
        @DisplayName("eventType 返回 PasswordChanged")
        void eventType_returnsPasswordChanged() {
            PasswordChangedEvent event = new PasswordChangedEvent(UserId.of(1L));

            assertThat(event.eventType()).isEqualTo("PasswordChanged");
        }
    }
}