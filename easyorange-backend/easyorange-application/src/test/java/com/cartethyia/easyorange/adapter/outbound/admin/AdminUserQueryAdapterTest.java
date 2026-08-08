package com.cartethyia.easyorange.adapter.outbound.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.user.adapter.outbound.persistence.UserDO;
import com.cartethyia.easyorange.user.adapter.outbound.persistence.UserMapper;
import com.cartethyia.easyorange.user.domain.enums.UserStatus;
import com.cartethyia.easyorange.user.domain.enums.UserType;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminUserQueryAdapter 单元测试")
class AdminUserQueryAdapterTest {

    @Mock
    private UserMapper userMapper;

    private AdminUserQueryAdapter adapter;

    private static final String USER_ID = "1";

    @BeforeEach
    void setUp() {
        adapter = new AdminUserQueryAdapter(userMapper);
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
    @DisplayName("updateUserStatus")
    class UpdateUserStatusTests {

        @Test
        @DisplayName("更新状态成功")
        void success() {
            UserDO user = user(UserStatus.NORMAL);
            when(userMapper.selectById(USER_ID)).thenReturn(user);

            adapter.updateUserStatus(USER_ID, "DISABLED");

            assertThat(user.getStatus()).isEqualTo(UserStatus.DISABLED);
            verify(userMapper).updateById(user);
        }

        @Test
        @DisplayName("非法状态码抛出业务异常")
        void invalidCode_throws() {
            when(userMapper.selectById(USER_ID)).thenReturn(user(UserStatus.NORMAL));

            assertThatThrownBy(() -> adapter.updateUserStatus(USER_ID, "999"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("无效的用户状态");
        }

        @Test
        @DisplayName("用户不存在抛出业务异常")
        void notFound_throws() {
            when(userMapper.selectById(USER_ID)).thenReturn(null);

            assertThatThrownBy(() -> adapter.updateUserStatus(USER_ID, "DISABLED"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("用户不存在");
        }
    }

    @Nested
    @DisplayName("unlockUser")
    class UnlockUserTests {

        @Test
        @DisplayName("解锁锁定用户")
        void locked_setsNormal() {
            UserDO user = user(UserStatus.LOCKED);
            when(userMapper.selectById(USER_ID)).thenReturn(user);

            adapter.unlockUser(USER_ID);

            assertThat(user.getStatus()).isEqualTo(UserStatus.NORMAL);
            verify(userMapper).updateById(user);
        }

        @Test
        @DisplayName("解禁禁用用户")
        void disabled_setsNormal() {
            UserDO user = user(UserStatus.DISABLED);
            when(userMapper.selectById(USER_ID)).thenReturn(user);

            adapter.unlockUser(USER_ID);

            assertThat(user.getStatus()).isEqualTo(UserStatus.NORMAL);
        }

        @Test
        @DisplayName("正常用户无需解锁抛出业务异常")
        void normal_throws() {
            when(userMapper.selectById(USER_ID)).thenReturn(user(UserStatus.NORMAL));

            assertThatThrownBy(() -> adapter.unlockUser(USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("未被锁定或禁用");
        }
    }

    @Nested
    @DisplayName("setUserType")
    class SetUserTypeTests {

        @Test
        @DisplayName("变更角色成功")
        void success() {
            UserDO user = user(UserStatus.NORMAL);
            when(userMapper.selectById(USER_ID)).thenReturn(user);

            adapter.setUserType(USER_ID, "02");

            assertThat(user.getUserType()).isEqualTo(UserType.MANAGER);
            verify(userMapper).updateById(user);
        }

        @Test
        @DisplayName("角色未变化抛出业务异常")
        void sameRole_throws() {
            when(userMapper.selectById(USER_ID)).thenReturn(user(UserStatus.NORMAL));

            assertThatThrownBy(() -> adapter.setUserType(USER_ID, "01"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("已是该角色");
        }

        @Test
        @DisplayName("非法角色码抛出业务异常")
        void invalidCode_throws() {
            when(userMapper.selectById(USER_ID)).thenReturn(user(UserStatus.NORMAL));

            assertThatThrownBy(() -> adapter.setUserType(USER_ID, "99"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("无效的用户角色");
        }

        @Test
        @DisplayName("提升为管理员成功（非管理员用户不受最后一个管理员保护限制）")
        void promoteToAdmin_succeeds() {
            UserDO user = user(UserStatus.NORMAL);
            when(userMapper.selectById(USER_ID)).thenReturn(user);

            adapter.setUserType(USER_ID, "00");

            assertThat(user.getUserType()).isEqualTo(UserType.ADMIN);
            verify(userMapper).updateById(user);
        }
    }

    @Nested
    @DisplayName("getUserAuth / getUserDetail")
    class QueryTests {

        @Test
        @DisplayName("查询用户认证信息")
        void getUserAuth_returnsCodes() {
            when(userMapper.selectById(USER_ID)).thenReturn(user(UserStatus.LOCKED));

            var auth = adapter.getUserAuth(USER_ID);

            assertThat(auth).isNotNull();
            assertThat(auth.userType()).isEqualTo("01");
            assertThat(auth.status()).isEqualTo("LOCKED");
        }

        @Test
        @DisplayName("已删除用户返回 null")
        void deletedUser_returnsNull() {
            UserDO user = user(UserStatus.NORMAL);
            user.setDelFlag(2);
            when(userMapper.selectById(USER_ID)).thenReturn(user);

            assertThat(adapter.getUserAuth(USER_ID)).isNull();
            assertThat(adapter.getUserDetail(USER_ID)).isNull();
        }

        @Test
        @DisplayName("批量查询用户信息")
        void getUserInfos_returnsMap() {
            when(userMapper.selectByIds(List.of(USER_ID))).thenReturn(List.of(user(UserStatus.NORMAL)));

            var map = adapter.getUserInfos(List.of(USER_ID));

            assertThat(map).containsKey(USER_ID);
            assertThat(map.get(USER_ID).username()).isEqualTo("testuser");
        }
    }
}
