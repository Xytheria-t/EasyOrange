package com.cartethyia.easyorange.user.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.user.domain.aggregate.User;
import com.cartethyia.easyorange.user.domain.aggregate.UserTestFixture;
import com.cartethyia.easyorange.user.domain.enums.UserStatus;
import com.cartethyia.easyorange.user.domain.enums.UserType;
import com.cartethyia.easyorange.user.domain.repository.UserRepository;
import com.cartethyia.easyorange.user.domain.valueobject.AuditInfo;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminUserManagementService 单元测试")
class AdminUserManagementServiceTest {

    @Mock
    private UserRepository userRepository;

    private AdminUserManagementService service;

    private static final String USER_ID = UserTestFixture.USER_ID;

    @BeforeEach
    void setUp() {
        service = new AdminUserManagementService(userRepository);
    }

    private User user(UserType userType, UserStatus status) {
        return UserTestFixture.aUser().userType(userType).status(status).build();
    }

    @Nested
    @DisplayName("updateStatus")
    class UpdateStatusTests {

        @Test
        @DisplayName("更新状态成功")
        void success() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user(UserType.NORMAL, UserStatus.NORMAL)));

            User updated = service.updateStatus(USER_ID, UserStatus.DISABLED);

            assertThat(updated.getStatus()).isEqualTo(UserStatus.DISABLED);
            assertThat(updated.getUserType()).isEqualTo(UserType.NORMAL);
        }

        @Test
        @DisplayName("用户不存在抛出业务异常")
        void notFound_throws() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateStatus(USER_ID, UserStatus.DISABLED))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("用户不存在");
        }

        @Test
        @DisplayName("已删除用户按不存在处理")
        void deleted_throws() {
            User deleted = UserTestFixture.aUser()
                    .auditInfo(new AuditInfo(null, null, null, null, AuditInfo.DELETED, 0))
                    .build();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(deleted));

            assertThatThrownBy(() -> service.updateStatus(USER_ID, UserStatus.DISABLED))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("用户不存在");
        }
    }

    @Nested
    @DisplayName("unlock")
    class UnlockTests {

        @Test
        @DisplayName("解锁锁定用户")
        void locked_setsNormal() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user(UserType.NORMAL, UserStatus.LOCKED)));

            User updated = service.unlock(USER_ID);

            assertThat(updated.getStatus()).isEqualTo(UserStatus.NORMAL);
        }

        @Test
        @DisplayName("解禁禁用用户")
        void disabled_setsNormal() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user(UserType.NORMAL, UserStatus.DISABLED)));

            User updated = service.unlock(USER_ID);

            assertThat(updated.getStatus()).isEqualTo(UserStatus.NORMAL);
        }

        @Test
        @DisplayName("正常用户无需解锁抛出业务异常")
        void normal_throws() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user(UserType.NORMAL, UserStatus.NORMAL)));

            assertThatThrownBy(() -> service.unlock(USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("未被锁定或禁用");
        }

        @Test
        @DisplayName("用户不存在抛出业务异常")
        void notFound_throws() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.unlock(USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("用户不存在");
        }
    }

    @Nested
    @DisplayName("changeUserType")
    class ChangeUserTypeTests {

        @Test
        @DisplayName("变更角色成功")
        void success() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user(UserType.NORMAL, UserStatus.NORMAL)));

            User updated = service.changeUserType(USER_ID, UserType.MANAGER);

            assertThat(updated.getUserType()).isEqualTo(UserType.MANAGER);
        }

        @Test
        @DisplayName("角色未变化抛出业务异常")
        void sameRole_throws() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user(UserType.NORMAL, UserStatus.NORMAL)));

            assertThatThrownBy(() -> service.changeUserType(USER_ID, UserType.NORMAL))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("已是该角色");
        }

        @Test
        @DisplayName("提升为管理员成功（非管理员用户不受最后一个管理员保护限制）")
        void promoteToAdmin_succeeds() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user(UserType.NORMAL, UserStatus.NORMAL)));

            User updated = service.changeUserType(USER_ID, UserType.ADMIN);

            assertThat(updated.getUserType()).isEqualTo(UserType.ADMIN);
        }

        @Test
        @DisplayName("降级最后一个管理员抛出业务异常")
        void demoteLastAdmin_throws() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user(UserType.ADMIN, UserStatus.NORMAL)));
            when(userRepository.countByUserType(UserType.ADMIN)).thenReturn(1L);

            assertThatThrownBy(() -> service.changeUserType(USER_ID, UserType.NORMAL))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("不能修改最后一个管理员的角色");
        }

        @Test
        @DisplayName("存在多个管理员时降级成功")
        void demoteWithMoreAdmins_succeeds() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user(UserType.ADMIN, UserStatus.NORMAL)));
            when(userRepository.countByUserType(UserType.ADMIN)).thenReturn(2L);

            User updated = service.changeUserType(USER_ID, UserType.NORMAL);

            assertThat(updated.getUserType()).isEqualTo(UserType.NORMAL);
        }

        @Test
        @DisplayName("用户不存在抛出业务异常")
        void notFound_throws() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.changeUserType(USER_ID, UserType.MANAGER))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("用户不存在");
        }
    }

    @Nested
    @DisplayName("resetPassword")
    class ResetPasswordTests {

        @Test
        @DisplayName("重置密码成功")
        void success() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user(UserType.NORMAL, UserStatus.NORMAL)));

            User updated = service.resetPassword(USER_ID, "$2a$10$newEncoded");

            assertThat(updated.getPassword()).isEqualTo("$2a$10$newEncoded");
        }

        @Test
        @DisplayName("用户不存在抛出业务异常")
        void notFound_throws() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.resetPassword(USER_ID, "$2a$10$newEncoded"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("用户不存在");
        }
    }
}
