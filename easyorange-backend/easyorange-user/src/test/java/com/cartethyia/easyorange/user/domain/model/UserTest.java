package com.cartethyia.easyorange.user.domain.model;

import com.cartethyia.easyorange.user.common.enums.UserStatus;
import com.cartethyia.easyorange.user.common.enums.UserType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("User 领域模型测试")
class UserTest {

    @Nested
    @DisplayName("changePassword")
    class ChangePasswordTests {

        @Test
        @DisplayName("应更新密码和密码修改时间")
        void shouldUpdatePasswordAndPwdUpdateDate() {
            // Arrange
            LocalDateTime beforeChange = LocalDateTime.now().minusSeconds(1);
            User user = User.builder()
                .id(1L)
                .username("testuser")
                .password("oldEncodedPassword")
                .build();

            // Act
            user.changePassword("newEncodedPassword");

            // Assert
            assertThat(user.getPassword()).isEqualTo("newEncodedPassword");
            assertThat(user.getPwdUpdateDate()).isNotNull();
            assertThat(user.getPwdUpdateDate()).isAfter(beforeChange);
        }

        @Test
        @DisplayName("多次修改密码应覆盖旧密码")
        void shouldOverridePreviousPassword() {
            // Arrange
            User user = User.builder()
                .id(1L)
                .username("testuser")
                .password("firstPassword")
                .build();

            // Act
            user.changePassword("secondPassword");
            user.changePassword("thirdPassword");

            // Assert
            assertThat(user.getPassword()).isEqualTo("thirdPassword");
        }
    }

    @Nested
    @DisplayName("updateLoginInfo")
    class UpdateLoginInfoTests {

        @Test
        @DisplayName("应更新登录IP和登录时间")
        void shouldUpdateLoginIpAndLoginDate() {
            // Arrange
            LocalDateTime beforeUpdate = LocalDateTime.now().minusSeconds(1);
            User user = User.builder()
                .id(1L)
                .username("testuser")
                .build();

            // Act
            user.updateLoginInfo("192.168.1.1");

            // Assert
            assertThat(user.getLoginIp()).isEqualTo("192.168.1.1");
            assertThat(user.getLoginDate()).isNotNull();
            assertThat(user.getLoginDate()).isAfter(beforeUpdate);
        }

        @Test
        @DisplayName("多次更新应覆盖之前的登录信息")
        void shouldOverridePreviousLoginInfo() {
            // Arrange
            User user = User.builder()
                .id(1L)
                .username("testuser")
                .build();

            // Act
            user.updateLoginInfo("10.0.0.1");
            user.updateLoginInfo("10.0.0.2");

            // Assert
            assertThat(user.getLoginIp()).isEqualTo("10.0.0.2");
        }
    }

    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("Builder 应正确创建 User 对象")
        void shouldCreateUserCorrectly() {
            // Act
            User user = User.builder()
                .id(1L)
                .username("testuser")
                .password("encodedPassword")
                .userType(UserType.NORMAL)
                .status(UserStatus.NORMAL)
                .email("test@example.com")
                .phone("13812345678")
                .realName("张三")
                .nickName("小张")
                .avatar("/avatar/test.png")
                .build();

            // Assert
            assertThat(user.getId()).isEqualTo(1L);
            assertThat(user.getUsername()).isEqualTo("testuser");
            assertThat(user.getPassword()).isEqualTo("encodedPassword");
            assertThat(user.getUserType()).isEqualTo(UserType.NORMAL);
            assertThat(user.getStatus()).isEqualTo(UserStatus.NORMAL);
            assertThat(user.getEmail()).isEqualTo("test@example.com");
            assertThat(user.getPhone()).isEqualTo("13812345678");
            assertThat(user.getRealName()).isEqualTo("张三");
            assertThat(user.getNickName()).isEqualTo("小张");
            assertThat(user.getAvatar()).isEqualTo("/avatar/test.png");
        }

        @Test
        @DisplayName("无参构造应创建空 User 对象")
        void shouldCreateEmptyUserWithNoArgsConstructor() {
            // Act
            User user = new User();

            // Assert
            assertThat(user.getId()).isNull();
            assertThat(user.getUsername()).isNull();
            assertThat(user.getPassword()).isNull();
        }

        @Test
        @DisplayName("Setter 应正确设置字段值")
        void shouldSetFieldsCorrectly() {
            // Arrange
            User user = new User();

            // Act
            user.setId(99L);
            user.setUsername("setterUser");
            user.setEmail("setter@example.com");

            // Assert
            assertThat(user.getId()).isEqualTo(99L);
            assertThat(user.getUsername()).isEqualTo("setterUser");
            assertThat(user.getEmail()).isEqualTo("setter@example.com");
        }
    }
}
