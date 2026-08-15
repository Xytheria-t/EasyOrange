package com.cartethyia.easyorange.user.adapter.outbound.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.user.adapter.outbound.persistence.UserDO;
import com.cartethyia.easyorange.user.adapter.outbound.persistence.UserMapper;
import com.cartethyia.easyorange.user.domain.aggregate.User;
import com.cartethyia.easyorange.user.domain.aggregate.UserTestFixture;
import com.cartethyia.easyorange.user.domain.enums.UserStatus;
import com.cartethyia.easyorange.user.domain.enums.UserType;
import com.cartethyia.easyorange.user.domain.port.AdminUserManagementPort.AdminUserPage;
import com.cartethyia.easyorange.user.domain.port.AdminUserManagementPort.AdminUserQuery;
import com.cartethyia.easyorange.user.domain.repository.UserRepository;
import com.cartethyia.easyorange.user.domain.service.AdminUserManagementService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminUserManagementAdapter 单元测试")
class AdminUserManagementAdapterTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AdminUserManagementService adminUserManagementService;

    private AdminUserManagementAdapter adapter;

    private static final String USER_ID = "1";

    @BeforeEach
    void setUp() {
        adapter = new AdminUserManagementAdapter(userMapper, userRepository, adminUserManagementService);
    }

    private User updatedUser() {
        return UserTestFixture.aUser().build();
    }

    private UserDO userDO(UserStatus status) {
        UserDO user = UserDO.builder()
                .id(USER_ID)
                .username("testuser")
                .nickName("小张")
                .email("test@example.com")
                .phone("13812345678")
                .userType(UserType.NORMAL)
                .status(status)
                .build();
        user.setDelFlag(0);
        return user;
    }

    @Nested
    @DisplayName("updateStatus")
    class UpdateStatusTests {

        @Test
        @DisplayName("解析状态码后委托领域服务并持久化")
        void success() {
            User updated = updatedUser();
            when(adminUserManagementService.updateStatus(USER_ID, UserStatus.DISABLED))
                    .thenReturn(updated);

            adapter.updateStatus(USER_ID, "DISABLED");

            verify(userRepository).update(updated);
        }

        @Test
        @DisplayName("非法状态码抛出业务异常且不触达领域服务")
        void invalidCode_throws() {
            assertThatThrownBy(() -> adapter.updateStatus(USER_ID, "999"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("无效的用户状态");
            verify(adminUserManagementService, never()).updateStatus(any(), any());
        }
    }

    @Nested
    @DisplayName("unlock")
    class UnlockTests {

        @Test
        @DisplayName("委托领域服务并持久化")
        void success() {
            User updated = updatedUser();
            when(adminUserManagementService.unlock(USER_ID)).thenReturn(updated);

            adapter.unlock(USER_ID);

            verify(userRepository).update(updated);
        }
    }

    @Nested
    @DisplayName("setUserType")
    class SetUserTypeTests {

        @Test
        @DisplayName("解析角色码后委托领域服务并持久化")
        void success() {
            User updated = updatedUser();
            when(adminUserManagementService.changeUserType(USER_ID, UserType.MANAGER))
                    .thenReturn(updated);

            adapter.setUserType(USER_ID, "02");

            verify(userRepository).update(updated);
        }

        @Test
        @DisplayName("非法角色码抛出业务异常且不触达领域服务")
        void invalidCode_throws() {
            assertThatThrownBy(() -> adapter.setUserType(USER_ID, "99"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("无效的用户角色");
            verify(adminUserManagementService, never()).changeUserType(any(), any());
        }
    }

    @Nested
    @DisplayName("setPassword")
    class SetPasswordTests {

        @Test
        @DisplayName("委托领域服务并持久化")
        void success() {
            User updated = updatedUser();
            when(adminUserManagementService.resetPassword(USER_ID, "$2a$10$newEncoded"))
                    .thenReturn(updated);

            adapter.setPassword(USER_ID, "$2a$10$newEncoded");

            verify(userRepository).update(updated);
        }
    }

    @Nested
    @DisplayName("getAuth / getDetail / getInfo")
    class QueryTests {

        @Test
        @DisplayName("查询用户认证信息")
        void getAuth_returnsCodes() {
            when(userMapper.selectById(USER_ID)).thenReturn(userDO(UserStatus.LOCKED));

            var auth = adapter.getAuth(USER_ID);

            assertThat(auth).isNotNull();
            assertThat(auth.userType()).isEqualTo("01");
            assertThat(auth.status()).isEqualTo("LOCKED");
        }

        @Test
        @DisplayName("已删除用户返回 null")
        void deletedUser_returnsNull() {
            UserDO user = userDO(UserStatus.NORMAL);
            user.setDelFlag(2);
            when(userMapper.selectById(USER_ID)).thenReturn(user);

            assertThat(adapter.getAuth(USER_ID)).isNull();
            assertThat(adapter.getDetail(USER_ID)).isNull();
            assertThat(adapter.getInfo(USER_ID)).isNotNull();
        }

        @Test
        @DisplayName("批量查询用户信息")
        void getInfos_returnsMap() {
            when(userMapper.selectByIds(List.of(USER_ID))).thenReturn(List.of(userDO(UserStatus.NORMAL)));

            var map = adapter.getInfos(List.of(USER_ID));

            assertThat(map).containsKey(USER_ID);
            assertThat(map.get(USER_ID).username()).isEqualTo("testuser");
        }
    }

    @Nested
    @DisplayName("query / getStats / getRecentUsers")
    class PageQueryTests {

        @Test
        @DisplayName("分页查询返回映射结果")
        void query_returnsPage() {
            Page<UserDO> page = new Page<>(1, 20, 1);
            page.setRecords(List.of(userDO(UserStatus.NORMAL)));
            when(userMapper.selectPage(any(), any())).thenReturn(page);

            AdminUserPage result = adapter.query(new AdminUserQuery("test", null, null, null, null, 1, 20));

            assertThat(result.total()).isEqualTo(1);
            assertThat(result.records()).hasSize(1);
            assertThat(result.records().get(0).username()).isEqualTo("testuser");
            assertThat(result.records().get(0).status()).isEqualTo("NORMAL");
        }

        @Test
        @DisplayName("统计用户总数与今日新增")
        void getStats_returnsCounts() {
            when(userMapper.selectCount(any())).thenReturn(100L, 5L);

            var stats = adapter.getStats();

            assertThat(stats.totalUsers()).isEqualTo(100L);
            assertThat(stats.todayNewUsers()).isEqualTo(5L);
        }

        @Test
        @DisplayName("查询最近注册用户")
        void getRecentUsers_returnsList() {
            Page<UserDO> page = new Page<>(1, 10, 1);
            page.setRecords(List.of(userDO(UserStatus.NORMAL)));
            when(userMapper.selectPage(any(), any())).thenReturn(page);

            var result = adapter.getRecentUsers(10);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).username()).isEqualTo("testuser");
        }
    }
}
