package com.cartethyia.easyorange.user.domain.event;

import com.cartethyia.easyorange.user.domain.valueobject.UserId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserRegisteredEvent 领域事件测试")
class UserRegisteredEventTest {

    @Nested
    @DisplayName("构造函数")
    class ConstructorTests {

        @Test
        @DisplayName("构造函数正确创建事件")
        void constructor_createsEventWithCorrectData() {
            UserId userId = UserId.of(1L);
            String username = "testuser";

            UserRegisteredEvent event = new UserRegisteredEvent(userId, username);

            assertThat(event.getUserId()).isEqualTo(userId);
            assertThat(event.getUsername()).isEqualTo(username);
        }

        @Test
        @DisplayName("事件自动生成 eventId")
        void constructor_generatesEventId() {
            UserRegisteredEvent event = new UserRegisteredEvent(UserId.of(1L), "user");

            assertThat(event.getEventId()).isNotNull();
            assertThat(event.getEventId()).isNotEmpty();
        }

        @Test
        @DisplayName("事件自动生成 occurredOn 时间戳")
        void constructor_generatesOccurredOn() {
            UserRegisteredEvent event = new UserRegisteredEvent(UserId.of(1L), "user");

            assertThat(event.getOccurredOn()).isNotNull();
        }

        @Test
        @DisplayName("事件聚合类型为 User")
        void constructor_setsAggregateTypeToUser() {
            UserRegisteredEvent event = new UserRegisteredEvent(UserId.of(1L), "user");

            assertThat(event.getAggregateType()).isEqualTo("User");
        }
    }

    @Nested
    @DisplayName("eventType")
    class EventTypeTests {

        @Test
        @DisplayName("eventType 返回 UserRegistered")
        void eventType_returnsUserRegistered() {
            UserRegisteredEvent event = new UserRegisteredEvent(UserId.of(1L), "user");

            assertThat(event.eventType()).isEqualTo("UserRegistered");
        }
    }
}