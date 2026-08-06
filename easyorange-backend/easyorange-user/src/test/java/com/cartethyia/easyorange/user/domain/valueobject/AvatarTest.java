package com.cartethyia.easyorange.user.domain.valueobject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.user.domain.enums.UserResultCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Avatar 值对象测试")
class AvatarTest {

    @Nested
    @DisplayName("validate")
    class ValidateTests {

        @Test
        @DisplayName("内容为 null — 抛 B1014")
        void nullContent() {
            assertThatThrownBy(() -> Avatar.validate(null))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getCode())
                    .isEqualTo(UserResultCode.AVATAR_EMPTY.getCode());
        }

        @Test
        @DisplayName("内容为空 — 抛 B1014")
        void emptyContent() {
            assertThatThrownBy(() -> Avatar.validate(new byte[0]))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getCode())
                    .isEqualTo(UserResultCode.AVATAR_EMPTY.getCode());
        }

        @Test
        @DisplayName("恰好 5MB — 通过(边界)")
        void exactlyMaxSize() {
            assertThatCode(() -> Avatar.validate(new byte[(int) Avatar.MAX_SIZE_BYTES]))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("超过 5MB — 抛 B1015")
        void tooLarge() {
            assertThatThrownBy(() -> Avatar.validate(new byte[(int) Avatar.MAX_SIZE_BYTES + 1]))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getCode())
                    .isEqualTo(UserResultCode.AVATAR_TOO_LARGE.getCode());
        }
    }

    @Nested
    @DisplayName("uploaded")
    class UploadedTests {

        @Test
        @DisplayName("记录 URL、大小与内容类型")
        void recordsAttributes() {
            byte[] content = {1, 2, 3};
            Avatar avatar = Avatar.uploaded("/avatar/a.png", content, "image/png");

            assertThat(avatar.url()).isEqualTo("/avatar/a.png");
            assertThat(avatar.sizeBytes()).isEqualTo(3);
            assertThat(avatar.contentType()).isEqualTo("image/png");
        }
    }
}
