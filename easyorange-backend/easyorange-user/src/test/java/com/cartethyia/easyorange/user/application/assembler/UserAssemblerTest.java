package com.cartethyia.easyorange.user.application.assembler;

import com.cartethyia.easyorange.user.adapter.inbound.web.dto.response.LoginResponse;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.response.UserProfileVO;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.response.UserVO;
import com.cartethyia.easyorange.user.domain.aggregate.User;
import com.cartethyia.easyorange.user.domain.valueobject.AuditInfo;
import com.cartethyia.easyorange.user.domain.valueobject.UserProfile;
import com.cartethyia.easyorange.user.domain.enums.Sex;
import com.cartethyia.easyorange.user.domain.enums.UserStatus;
import com.cartethyia.easyorange.user.domain.enums.UserType;
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
        userAssembler = new UserAssemblerImpl();
    }

    private User buildTestUser() {
        UserProfile profile = new UserProfile(
            "test@example.com",
            "13812345678",
            "张三",
            "小张",
            Sex.MALE,
            "/avatar/test.png",
            null
        );
        AuditInfo auditInfo = new AuditInfo(
            LocalDateTime.of(2024, 1, 1, 0, 0),
            LocalDateTime.of(2024, 6, 1, 0, 0),
            1L,
            1L,
            0,
            0
        );

        return User.builder()
            .id(1L)
            .username("testuser")
            .password("$2a$10$encoded")
            .userType(UserType.NORMAL)
            .status(UserStatus.NORMAL)
            .studentId("2024001")
            .profile(profile)
            .auditInfo(auditInfo)
            .build();
    }

    @Nested
    @DisplayName("toVo")
    class ToVoTests {

        @Test
        @DisplayName("应正确映射并脱敏")
        void shouldMapCorrectlyWithMasking() {
            User user = buildTestUser();

            UserVO vo = userAssembler.toVo(user);

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
        @DisplayName("null 用户应返回默认对象（RETURN_DEFAULT 策略）")
        void shouldReturnDefaultForNullUser() {
            UserVO vo = userAssembler.toVo(null);

            assertThat(vo).isNotNull();
            assertThat(vo.getUserId()).isNull();
        }

        @Test
        @DisplayName("status 为 null 时应返回 0")
        void shouldReturnZeroWhenStatusIsNull() {
            User user = User.builder()
                .id(1L)
                .username("testuser")
                .status(null)
                .build();

            UserVO vo = userAssembler.toVo(user);

            assertThat(vo.getStatus()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("toProfileVo")
    class ToProfileVoTests {

        @Test
        @DisplayName("应正确映射并脱敏")
        void shouldMapCorrectly() {
            User user = buildTestUser();
            Set<String> roles = Set.of("ROLE_USER");
            Set<String> permissions = Set.of("user:read");
            Long loginTime = System.currentTimeMillis();

            UserProfileVO vo = userAssembler.toProfileVo(user, roles, permissions, loginTime);

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
        @DisplayName("null 用户应返回默认对象（多参数方法 RETURN_DEFAULT 策略）")
        void shouldReturnDefaultForNullUser() {
            UserProfileVO vo = userAssembler.toProfileVo(null, Set.of(), Set.of(), 0L);

            assertThat(vo).isNotNull();
            assertThat(vo.getId()).isNull();
            assertThat(vo.getRoles()).isEmpty();
            assertThat(vo.getPermissions()).isEmpty();
        }
    }

    @Nested
    @DisplayName("toLoginResponse")
    class ToLoginResponseTests {

        @Test
        @DisplayName("应正确映射登录响应")
        void shouldMapCorrectly() {
            User user = buildTestUser();

            LoginResponse response = userAssembler.toLoginResponse(user, "access-token", "refresh-token");

            assertThat(response).isNotNull();
            assertThat(response.getToken()).isEqualTo("access-token");
            assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
            assertThat(response.getUser()).isNotNull();
            assertThat(response.getUser().getUserId()).isEqualTo(1L);
        }
    }
}
