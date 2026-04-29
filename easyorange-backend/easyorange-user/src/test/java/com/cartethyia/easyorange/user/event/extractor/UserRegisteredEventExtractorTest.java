package com.cartethyia.easyorange.user.event.extractor;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cartethyia.easyorange.user.entity.User;
import com.cartethyia.easyorange.user.event.UserRegisteredEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserRegisteredEventExtractorTest {

    @Mock
    private BaseMapper<User> userMapper;

    private UserRegisteredEventExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new UserRegisteredEventExtractor(userMapper);
    }

    @Test
    @DisplayName("应正确提取用户注册事件")
    void shouldExtractUserRegisteredEvent() {
        Long userId = 123L;
        User user = new User();
        user.setUsername("testuser");
        when(userMapper.selectById(userId)).thenReturn(user);

        UserRegisteredEvent event = extractor.extract(userId);

        assertThat(event).isNotNull();
        assertThat(event.getUserId()).isEqualTo(userId);
        assertThat(event.getUsername()).isEqualTo("testuser");
        assertThat(event.eventType()).isEqualTo("UserRegistered");
        assertThat(event.getAggregateType()).isEqualTo("User");
    }

    @Test
    @DisplayName("当用户不存在时应抛出异常")
    void shouldThrowExceptionWhenUserNotFound() {
        Long userId = 456L;
        when(userMapper.selectById(userId)).thenReturn(null);

        assertThatThrownBy(() -> extractor.extract(userId))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("User not found");
    }
}