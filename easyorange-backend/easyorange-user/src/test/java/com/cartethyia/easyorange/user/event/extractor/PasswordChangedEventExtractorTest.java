package com.cartethyia.easyorange.user.event.extractor;

import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.user.event.PasswordChangedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
class PasswordChangedEventExtractorTest {

    @Test
    void shouldExtractPasswordChangedEvent() {
        Long userId = 123L;
        try (MockedStatic<SecurityContextUtil> mockedSecurityContext = mockStatic(SecurityContextUtil.class)) {
            mockedSecurityContext.when(SecurityContextUtil::getCurrentUserIdOrThrow).thenReturn(userId);

            PasswordChangedEventExtractor extractor = new PasswordChangedEventExtractor();
            PasswordChangedEvent event = extractor.extract(456L);

            assertThat(event).isNotNull();
            assertThat(event.getUserId()).isEqualTo(userId);
            assertThat(event.eventType()).isEqualTo("PasswordChanged");
            assertThat(event.getAggregateType()).isEqualTo("User");
        }
    }

    @Test
    void shouldThrowExceptionWhenUserNotAuthenticated() {
        try (MockedStatic<SecurityContextUtil> mockedSecurityContext = mockStatic(SecurityContextUtil.class)) {
            mockedSecurityContext.when(SecurityContextUtil::getCurrentUserIdOrThrow)
                .thenThrow(new IllegalStateException("No authenticated user"));

            PasswordChangedEventExtractor extractor = new PasswordChangedEventExtractor();

            assertThatThrownBy(() -> extractor.extract(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No authenticated user");
        }
    }
}