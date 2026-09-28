package com.cartethyia.easyorange.admin.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.admin.domain.exception.AdminDomainException;
import com.cartethyia.easyorange.admin.domain.port.AdminUserPort;
import com.cartethyia.easyorange.admin.domain.port.AdminUserPort.UserAuth;
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
@DisplayName("AdminUserSecurityAppService 单元测试")
class AdminUserSecurityAppServiceTest {

    @Mock
    private AdminUserPort adminUserPort;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @Mock
    private TokenService tokenService;

    private AdminUserSecurityAppService service;

    private static final String USER_ID = "1";
    private static final String REASON = "违规操作核查";
    private static final String OPERATOR = "admin-1";

    @BeforeEach
    void setUp() {
        service = new AdminUserSecurityAppService(adminUserPort, passwordEncoder, tokenService);
    }

    @Nested
    @DisplayName("unlockUser")
    class UnlockUserTests {

        @Test
        @DisplayName("解锁委托端口")
        void unlockUser_delegatesToPort() {
            service.unlockUser(USER_ID, OPERATOR);

            verify(adminUserPort).unlockUser(USER_ID, OPERATOR);
        }
    }

    @Nested
    @DisplayName("resetPassword")
    class ResetPasswordTests {

        @Test
        @DisplayName("重置密码成功并返回新密码")
        void resetPassword_success() {
            when(adminUserPort.getUserAuth(USER_ID)).thenReturn(new UserAuth("01", "NORMAL"));
            when(passwordEncoder.encode(anyString())).thenReturn("encoded");

            String newPassword = service.resetPassword(USER_ID, REASON, OPERATOR);

            assertThat(newPassword).hasSize(12);
            verify(adminUserPort).setPassword(USER_ID, "encoded", REASON, OPERATOR);
        }

        @Test
        @DisplayName("用户不存在抛出异常")
        void resetPassword_notFound_throws() {
            when(adminUserPort.getUserAuth(USER_ID)).thenReturn(null);

            assertThatThrownBy(() -> service.resetPassword(USER_ID, REASON, OPERATOR))
                    .isInstanceOf(AdminDomainException.class)
                    .hasMessageContaining("用户不存在");
        }
    }

    @Nested
    @DisplayName("forceLogout")
    class ForceLogoutTests {

        @Test
        @DisplayName("强制下线成功并吊销全部令牌")
        void forceLogout_success() {
            when(adminUserPort.getUserAuth(USER_ID)).thenReturn(new UserAuth("01", "NORMAL"));

            service.forceLogout(USER_ID);

            verify(tokenService).revokeAllUserSessions(USER_ID);
        }

        @Test
        @DisplayName("用户不存在抛出异常")
        void forceLogout_notFound_throws() {
            when(adminUserPort.getUserAuth(USER_ID)).thenReturn(null);

            assertThatThrownBy(() -> service.forceLogout(USER_ID))
                    .isInstanceOf(AdminDomainException.class)
                    .hasMessageContaining("用户不存在");
        }
    }

    @Nested
    @DisplayName("changeUserRole")
    class ChangeUserRoleTests {

        @Test
        @DisplayName("变更角色委托端口")
        void changeUserRole_delegatesToPort() {
            service.changeUserRole(USER_ID, "01", REASON, OPERATOR);

            verify(adminUserPort).setUserType(USER_ID, "01", REASON, OPERATOR);
        }
    }
}
