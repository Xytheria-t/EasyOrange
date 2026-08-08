package com.cartethyia.easyorange.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.UserRoleRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.ResetPasswordResponse;
import com.cartethyia.easyorange.admin.domain.port.AdminUserQueryPort;
import com.cartethyia.easyorange.admin.domain.port.AdminUserQueryPort.UserAuth;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.framework.auth.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminUserSecurityService 单元测试")
class AdminUserSecurityServiceTest {

    @Mock
    private AdminUserQueryPort adminUserQueryPort;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @Mock
    private TokenService tokenService;

    private AdminUserSecurityService service;

    private static final String USER_ID = "1";

    @BeforeEach
    void setUp() {
        service = new AdminUserSecurityService(adminUserQueryPort, passwordEncoder, tokenService);
    }

    @Nested
    @DisplayName("unlockUser")
    class UnlockUserTests {

        @Test
        @DisplayName("解锁委托端口")
        void unlockUser_delegatesToPort() {
            service.unlockUser(USER_ID);

            verify(adminUserQueryPort).unlockUser(USER_ID);
        }
    }

    @Nested
    @DisplayName("resetPassword")
    class ResetPasswordTests {

        @Test
        @DisplayName("重置密码成功并返回新密码")
        void resetPassword_success() {
            when(adminUserQueryPort.getUserAuth(USER_ID)).thenReturn(new UserAuth("01", "NORMAL"));
            when(passwordEncoder.encode(anyString())).thenReturn("encoded");

            ResetPasswordResponse response = service.resetPassword(USER_ID);

            assertThat(response.newPassword()).hasSize(12);
            verify(adminUserQueryPort).setPassword(USER_ID, "encoded");
        }

        @Test
        @DisplayName("用户不存在抛出异常")
        void resetPassword_notFound_throws() {
            when(adminUserQueryPort.getUserAuth(USER_ID)).thenReturn(null);

            assertThatThrownBy(() -> service.resetPassword(USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("用户不存在");
        }
    }

    @Nested
    @DisplayName("forceLogout")
    class ForceLogoutTests {

        @Test
        @DisplayName("强制下线成功并吊销全部令牌")
        void forceLogout_success() {
            when(adminUserQueryPort.getUserAuth(USER_ID)).thenReturn(new UserAuth("01", "NORMAL"));

            service.forceLogout(USER_ID);

            verify(tokenService).revokeAllUserSessions(USER_ID);
        }

        @Test
        @DisplayName("用户不存在抛出异常")
        void forceLogout_notFound_throws() {
            when(adminUserQueryPort.getUserAuth(USER_ID)).thenReturn(null);

            assertThatThrownBy(() -> service.forceLogout(USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("用户不存在");
        }
    }

    @Nested
    @DisplayName("changeUserRole")
    class ChangeUserRoleTests {

        @Test
        @DisplayName("变更角色委托端口")
        void changeUserRole_delegatesToPort() {
            UserRoleRequest request = new UserRoleRequest();
            request.setRole("01");

            service.changeUserRole(USER_ID, request);

            verify(adminUserQueryPort).setUserType(USER_ID, "01");
        }
    }
}
