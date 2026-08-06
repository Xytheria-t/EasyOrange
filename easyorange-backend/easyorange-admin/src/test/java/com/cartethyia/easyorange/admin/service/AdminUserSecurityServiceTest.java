package com.cartethyia.easyorange.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.UserRoleRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.ResetPasswordResponse;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.framework.auth.TokenService;
import com.cartethyia.easyorange.user.adapter.outbound.persistence.UserDO;
import com.cartethyia.easyorange.user.adapter.outbound.persistence.UserMapper;
import com.cartethyia.easyorange.user.domain.enums.UserStatus;
import com.cartethyia.easyorange.user.domain.enums.UserType;
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
    private UserMapper userMapper;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @Mock
    private TokenService tokenService;

    private AdminUserSecurityService service;

    private static final String USER_ID = "1";

    @BeforeEach
    void setUp() {
        service = new AdminUserSecurityService(userMapper, passwordEncoder, tokenService);
    }

    private UserDO user(UserStatus status) {
        UserDO user = UserDO.builder()
                .id(USER_ID)
                .username("testuser")
                .userType(UserType.NORMAL)
                .status(status)
                .build();
        user.setDelFlag(0);
        return user;
    }

    @Nested
    @DisplayName("unlockUser")
    class UnlockUserTests {

        @Test
        @DisplayName("解锁锁定用户")
        void unlockUser_locked_setsNormal() {
            UserDO user = user(UserStatus.LOCKED);
            when(userMapper.selectById(USER_ID)).thenReturn(user);

            service.unlockUser(USER_ID);

            assertThat(user.getStatus()).isEqualTo(UserStatus.NORMAL);
            verify(userMapper).updateById(user);
        }

        @Test
        @DisplayName("解禁禁用用户")
        void unlockUser_disabled_setsNormal() {
            UserDO user = user(UserStatus.DISABLED);
            when(userMapper.selectById(USER_ID)).thenReturn(user);

            service.unlockUser(USER_ID);

            assertThat(user.getStatus()).isEqualTo(UserStatus.NORMAL);
        }

        @Test
        @DisplayName("正常用户无需解锁抛出异常")
        void unlockUser_normal_throws() {
            when(userMapper.selectById(USER_ID)).thenReturn(user(UserStatus.NORMAL));

            assertThatThrownBy(() -> service.unlockUser(USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("未被锁定或禁用");
        }

        @Test
        @DisplayName("用户不存在抛出异常")
        void unlockUser_notFound_throws() {
            when(userMapper.selectById(USER_ID)).thenReturn(null);

            assertThatThrownBy(() -> service.unlockUser(USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("用户不存在");
        }
    }

    @Nested
    @DisplayName("resetPassword")
    class ResetPasswordTests {

        @Test
        @DisplayName("重置密码成功并返回新密码")
        void resetPassword_success() {
            UserDO user = user(UserStatus.NORMAL);
            when(userMapper.selectById(USER_ID)).thenReturn(user);
            when(passwordEncoder.encode(anyString())).thenReturn("encoded");

            ResetPasswordResponse response = service.resetPassword(USER_ID);

            assertThat(response.newPassword()).hasSize(12);
            assertThat(user.getPassword()).isEqualTo("encoded");
            verify(userMapper).updateById(user);
        }
    }

    @Nested
    @DisplayName("forceLogout")
    class ForceLogoutTests {

        @Test
        @DisplayName("强制下线成功并吊销全部令牌")
        void forceLogout_success() {
            when(userMapper.selectById(USER_ID)).thenReturn(user(UserStatus.NORMAL));

            service.forceLogout(USER_ID);

            verify(tokenService).revokeAllUserSessions(USER_ID);
        }

        @Test
        @DisplayName("用户不存在抛出异常")
        void forceLogout_notFound_throws() {
            when(userMapper.selectById(USER_ID)).thenReturn(null);

            assertThatThrownBy(() -> service.forceLogout(USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("用户不存在");
        }
    }

    @Nested
    @DisplayName("changeUserRole")
    class ChangeUserRoleTests {

        @Test
        @DisplayName("角色未变化抛出异常")
        void changeUserRole_sameRole_throws() {
            UserDO user = user(UserStatus.NORMAL);
            user.setUserType(UserType.NORMAL);
            when(userMapper.selectById(USER_ID)).thenReturn(user);

            UserRoleRequest request = new UserRoleRequest();
            request.setRole(UserType.NORMAL.getCode());

            assertThatThrownBy(() -> service.changeUserRole(USER_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("已是该角色");
        }

        @Test
        @DisplayName("改为普通用户成功")
        void changeUserRole_toNormal_success() {
            UserDO user = user(UserStatus.NORMAL);
            user.setUserType(UserType.MANAGER);
            when(userMapper.selectById(USER_ID)).thenReturn(user);

            UserRoleRequest request = new UserRoleRequest();
            request.setRole(UserType.NORMAL.getCode());

            service.changeUserRole(USER_ID, request);

            assertThat(user.getUserType()).isEqualTo(UserType.NORMAL);
            verify(userMapper).updateById(user);
        }

        @Test
        @DisplayName("已删除用户抛出异常")
        void changeUserRole_deleted_throws() {
            UserDO user = user(UserStatus.NORMAL);
            user.setDelFlag(2);
            when(userMapper.selectById(USER_ID)).thenReturn(user);

            UserRoleRequest request = new UserRoleRequest();
            request.setRole(UserType.ADMIN.getCode());

            assertThatThrownBy(() -> service.changeUserRole(USER_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("用户不存在");
        }
    }
}
