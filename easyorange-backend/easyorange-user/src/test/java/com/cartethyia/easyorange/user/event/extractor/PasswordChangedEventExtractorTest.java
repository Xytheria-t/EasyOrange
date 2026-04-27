package com.cartethyia.easyorange.user.event.extractor;

import com.cartethyia.easyorange.user.event.PasswordChangedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordChangedEventExtractorTest {

    private PasswordChangedEventExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new PasswordChangedEventExtractor();
    }

    @Test
    @DisplayName("应正确提取密码修改事件")
    void shouldExtractPasswordChangedEvent() {
        // Arrange
        Long userId = 123L;
        extractor.setUserId(userId);

        Long result = 456L; // 返回值可以是任意值，主要依赖上下文中的 userId

        // Act
        PasswordChangedEvent event = extractor.extract(result);

        // Assert
        assertThat(event).isNotNull();
        assertThat(event.getUserId()).isEqualTo(userId);
        assertThat(event.eventType()).isEqualTo("PasswordChanged");
        assertThat(event.getAggregateType()).isEqualTo("User");
    }

    @Test
    @DisplayName("当用户 ID 上下文未设置时应抛出异常")
    void shouldThrowExceptionWhenUserIdContextNotSet() {
        // Arrange
        Long result = 789L;

        // Act & Assert
        assertThatThrownBy(() -> extractor.extract(result))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("User ID context not set");
    }

    @Test
    @DisplayName("提取后应清理上下文")
    void shouldCleanupContextAfterExtraction() {
        // Arrange
        Long userId = 999L;
        extractor.setUserId(userId);

        Long result = 111L;

        // Act
        extractor.extract(result);

        // Assert - 第二次提取应该失败，因为上下文已清理
        assertThatThrownBy(() -> extractor.extract(result))
            .isInstanceOf(IllegalStateException.class);
    }
}
