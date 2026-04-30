package com.cartethyia.easyorange.user.application.assembler;

import com.cartethyia.easyorange.user.domain.model.User;
import com.cartethyia.easyorange.user.dto.response.LoginResponse;
import com.cartethyia.easyorange.user.dto.vo.UserProfileVO;
import com.cartethyia.easyorange.user.dto.vo.UserVO;
import com.cartethyia.easyorange.user.common.enums.Sex;
import com.cartethyia.easyorange.user.common.enums.UserStatus;
import com.cartethyia.easyorange.user.common.enums.UserType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserAssembler 测试")
class UserAssemblerTest {

    private UserAssembler userAssembler;

    @BeforeEach
    void setUp() {
        userAssembler = new UserAssembler();
    }

    private User buildTestUser() {
        return User.builder()
            .id(1L)
            .username("testuser")
            .password("$2a$10$encoded")
            .userType(UserType.NORMAL)
            .status(UserStatus.NORMAL)
            .email("test@example.com")
            .phone("13812345678")
            .studentId("2024001")
            .realName("张三")
            .sex(Sex.MALE)
            .avatar("/avatar/test.png")
            .createTime(LocalDateTime.of(2024, 1, 1, 0, 0))
            .updateTime(LocalDateTime.of(2024, 6, 1, 0, 0))
            .build();
    }

    @Nested
    @DisplayName("toVo")
    class ToVoTests {

        @Test
        @DisplayName("应正确映射并脱敏")
        void shouldMapCorrectlyWithMasking() {
            // Arrange
            User user = buildTestUser();

            // Act
            UserVO vo = userAssembler.toVo(user);

            // Assert
            assertThat(vo).isNotNull();
            assertThat(vo.getUserId()).isEqualTo(1L);
            assertThat(vo.getUsername()).isEqualTo("testuser");
            assertThat(vo.getEmail()).contains("****");
            assertThat(vo.getPhone()).contains("****");
            assertThat(vo.getStudentId()).isEqualTo("2024001");
            assertThat(vo.getRealName()).isEqualTo("张*");
            assertThat(vo.getAvatar()).isEqualTo("/avatar/test.png");
            assertThat(vo.getStatus()).isEqualTo(0);
            assertThat(vo.getCreateTime()).isEqualTo(LocalDateTime.of(2024, 1, 1, 0, 0));
            assertThat(vo.getUpdateTime()).isEqualTo(LocalDateTime.of(2024, 6, 1, 0, 0));
        }

        @Test
        @DisplayName("null 用户应返回 null")
        void shouldReturnNullForNullUser() {
            // Act
            UserVO vo = userAssembler.toVo(null);

            // Assert
            assertThat(vo).isNull();
        }

        @Test
        @DisplayName("status 为 null 时应返回 0")
        void shouldReturnZeroWhenStatusIsNull() {
            // Arrange
            User user = User.builder()
                .id(1L)
                .username("testuser")
                .status(null)
                .build();

            // Act
            UserVO vo = userAssembler.toVo(user);

            // Assert
            assertThat(vo.getStatus()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("toProfileVo")
    class ToProfileVoTests {

        @Test
        @DisplayName("应正确映射并脱敏")
        void shouldMapCorrectly() {
            // Arrange
            User user = buildTestUser();
            Set<String> roles = Set.of("ROLE_USER");
            Set<String> permissions = Set.of("user:read");
            Long loginTime = System.currentTimeMillis();

            // Act
            UserProfileVO vo = userAssembler.toProfileVo(user, roles, permissions, loginTime);

            // Assert
            assertThat(vo).isNotNull();
            assertThat(vo.getId()).isEqualTo(1L);
            assertThat(vo.getUsername()).isEqualTo("testuser");
            assertThat(vo.getEmail()).contains("****");
            assertThat(vo.getPhone()).contains("****");
            assertThat(vo.getStudentId()).isEqualTo("2024001");
            assertThat(vo.getRealName()).isEqualTo("张*");
            assertThat(vo.getStatus()).isEqualTo(0);
            assertThat(vo.getStatusDesc()).isEqualTo("正常");
            assertThat(vo.getGender()).isEqualTo(Integer.parseInt(Sex.MALE.getCode()));
            assertThat(vo.getUserType()).isEqualTo("普通用户");
            assertThat(vo.getAvatar()).isEqualTo("/avatar/test.png");
            assertThat(vo.getRoles()).isEqualTo(roles);
            assertThat(vo.getPermissions()).isEqualTo(permissions);
            assertThat(vo.getLoginTime()).isEqualTo(loginTime);
        }

        @Test
        @DisplayName("null 用户应返回 null")
        void shouldReturnNullForNullUser() {
            // Act
            UserProfileVO vo = userAssembler.toProfileVo(null, Set.of(), Set.of(), 0L);

            // Assert
            assertThat(vo).isNull();
        }
    }

    @Nested
    @DisplayName("toLoginResponse")
    class ToLoginResponseTests {

        @Test
        @DisplayName("应正确映射登录响应")
        void shouldMapCorrectly() {
            // Arrange
            User user = buildTestUser();

            // Act
            LoginResponse response = userAssembler.toLoginResponse(user, "access-token", "refresh-token");

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getToken()).isEqualTo("access-token");
            assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
            assertThat(response.getUser()).isNotNull();
            assertThat(response.getUser().getUserId()).isEqualTo(1L);
        }
    }
}
