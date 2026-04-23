package com.cartethyia.easyorange.user.event.extractor;

import com.cartethyia.easyorange.user.entity.User;
import com.cartethyia.easyorange.user.event.UserRegisteredEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserRegisteredEventExtractorTest {

    private UserRegisteredEventExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new UserRegisteredEventExtractor();
    }

    @Test
    @DisplayName("应正确提取用户注册事件")
    void shouldExtractUserRegisteredEvent() {
        // Arrange
        User user = new User();
        user.setUsername("testuser");
        extractor.setUser(user);

        Long userId = 123L;

        // Act
        UserRegisteredEvent event = extractor.extract(userId);

        // Assert
        assertThat(event).isNotNull();
        assertThat(event.getUserId()).isEqualTo(userId);
        assertThat(event.getUsername()).isEqualTo("testuser");
        assertThat(event.eventType()).isEqualTo("UserRegistered");
        assertThat(event.getAggregateType()).isEqualTo("User");
    }

    @Test
    @DisplayName("当用户上下文未设置时应抛出异常")
    void shouldThrowExceptionWhenUserContextNotSet() {
        // Arrange
        Long userId = 456L;

        // Act & Assert
        assertThatThrownBy(() -> extractor.extract(userId))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("User context not set");
    }

    @Test
    @DisplayName("提取后应清理上下文")
    void shouldCleanupContextAfterExtraction() {
        // Arrange
        User user = new User();
        user.setUsername("cleanup_test");
        extractor.setUser(user);

        Long userId = 789L;

        // Act
        extractor.extract(userId);

        // Assert - 第二次提取应该失败，因为上下文已清理
        assertThatThrownBy(() -> extractor.extract(userId))
            .isInstanceOf(IllegalStateException.class);
    }
}
