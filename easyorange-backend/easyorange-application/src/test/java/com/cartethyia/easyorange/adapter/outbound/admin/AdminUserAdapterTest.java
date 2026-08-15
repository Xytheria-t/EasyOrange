package com.cartethyia.easyorange.adapter.outbound.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.admin.domain.port.AdminUserPort.UserDetail;
import com.cartethyia.easyorange.admin.domain.port.AdminUserPort.UserInfo;
import com.cartethyia.easyorange.admin.domain.port.AdminUserPort.UserQueryCondition;
import com.cartethyia.easyorange.admin.domain.port.AdminUserPort.UserQueryResult;
import com.cartethyia.easyorange.user.domain.port.AdminUserManagementPort;
import com.cartethyia.easyorange.user.domain.port.AdminUserManagementPort.AdminRecentUser;
import com.cartethyia.easyorange.user.domain.port.AdminUserManagementPort.AdminUserDetail;
import com.cartethyia.easyorange.user.domain.port.AdminUserManagementPort.AdminUserInfo;
import com.cartethyia.easyorange.user.domain.port.AdminUserManagementPort.AdminUserPage;
import com.cartethyia.easyorange.user.domain.port.AdminUserManagementPort.AdminUserQuery;
import com.cartethyia.easyorange.user.domain.port.AdminUserManagementPort.AdminUserStats;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminUserAdapter 翻译层单元测试")
class AdminUserAdapterTest {

    @Mock
    private AdminUserManagementPort adminUserManagementPort;

    private AdminUserAdapter adapter;

    private static final String USER_ID = "1";

    @BeforeEach
    void setUp() {
        adapter = new AdminUserAdapter(adminUserManagementPort);
    }

    private AdminUserInfo info() {
        return new AdminUserInfo(USER_ID, "testuser", "小张", "/avatar/test.png", "13812345678");
    }

    private AdminUserDetail detail() {
        return new AdminUserDetail(
                USER_ID,
                "testuser",
                "小张",
                "/avatar/test.png",
                "test@example.com",
                "13812345678",
                "2024001",
                "张三",
                "01",
                "普通用户",
                "NORMAL",
                "正常",
                "127.0.0.1",
                LocalDateTime.of(2026, 8, 1, 10, 0),
                LocalDateTime.of(2026, 7, 1, 10, 0),
                LocalDateTime.of(2026, 8, 1, 11, 0));
    }

    @Nested
    @DisplayName("查询翻译")
    class QueryMappingTests {

        @Test
        @DisplayName("getUserInfo 委托并映射基础信息")
        void getUserInfo_maps() {
            when(adminUserManagementPort.getInfo(USER_ID)).thenReturn(info());

            UserInfo result = adapter.getUserInfo(USER_ID);

            assertThat(result.id()).isEqualTo(USER_ID);
            assertThat(result.username()).isEqualTo("testuser");
            assertThat(result.nickName()).isEqualTo("小张");
            assertThat(result.avatar()).isEqualTo("/avatar/test.png");
            assertThat(result.phone()).isEqualTo("13812345678");
        }

        @Test
        @DisplayName("getUserInfo 用户不存在时透传 null")
        void getUserInfo_nullPassthrough() {
            when(adminUserManagementPort.getInfo(USER_ID)).thenReturn(null);

            assertThat(adapter.getUserInfo(USER_ID)).isNull();
        }

        @Test
        @DisplayName("getUserInfos 委托并映射批量信息")
        void getUserInfos_maps() {
            when(adminUserManagementPort.getInfos(List.of(USER_ID))).thenReturn(Map.of(USER_ID, info()));

            var result = adapter.getUserInfos(List.of(USER_ID));

            assertThat(result).containsKey(USER_ID);
            assertThat(result.get(USER_ID).username()).isEqualTo("testuser");
        }

        @Test
        @DisplayName("getUserDetail 委托并映射全部字段")
        void getUserDetail_maps() {
            when(adminUserManagementPort.getDetail(USER_ID)).thenReturn(detail());

            UserDetail result = adapter.getUserDetail(USER_ID);

            assertThat(result.id()).isEqualTo(USER_ID);
            assertThat(result.username()).isEqualTo("testuser");
            assertThat(result.email()).isEqualTo("test@example.com");
            assertThat(result.studentId()).isEqualTo("2024001");
            assertThat(result.realName()).isEqualTo("张三");
            assertThat(result.userType()).isEqualTo("01");
            assertThat(result.userTypeDesc()).isEqualTo("普通用户");
            assertThat(result.status()).isEqualTo("NORMAL");
            assertThat(result.statusDesc()).isEqualTo("正常");
            assertThat(result.loginIp()).isEqualTo("127.0.0.1");
            assertThat(result.createTime()).isEqualTo(LocalDateTime.of(2026, 7, 1, 10, 0));
            assertThat(result.updateTime()).isEqualTo(LocalDateTime.of(2026, 8, 1, 11, 0));
        }

        @Test
        @DisplayName("getUserDetail 用户不存在时透传 null")
        void getUserDetail_nullPassthrough() {
            when(adminUserManagementPort.getDetail(USER_ID)).thenReturn(null);

            assertThat(adapter.getUserDetail(USER_ID)).isNull();
        }

        @Test
        @DisplayName("queryUsers 翻译查询条件与结果")
        void queryUsers_translates() {
            AdminUserQuery expectedQuery = new AdminUserQuery("test", "01", "NORMAL", null, null, 2, 10);
            when(adminUserManagementPort.query(expectedQuery))
                    .thenReturn(new AdminUserPage(List.of(detail()), 1L, 2, 10));

            UserQueryResult result =
                    adapter.queryUsers(new UserQueryCondition("test", "01", "NORMAL", null, null, 2, 10));

            assertThat(result.total()).isEqualTo(1L);
            assertThat(result.pageNum()).isEqualTo(2);
            assertThat(result.pageSize()).isEqualTo(10);
            assertThat(result.records()).hasSize(1);
            assertThat(result.records().get(0).username()).isEqualTo("testuser");
        }

        @Test
        @DisplayName("getUserAuth 委托并映射认证信息")
        void getUserAuth_maps() {
            when(adminUserManagementPort.getAuth(USER_ID))
                    .thenReturn(new AdminUserManagementPort.AdminUserAuth("01", "LOCKED"));

            var result = adapter.getUserAuth(USER_ID);

            assertThat(result.userType()).isEqualTo("01");
            assertThat(result.status()).isEqualTo("LOCKED");
        }

        @Test
        @DisplayName("getUserAuth 用户不存在时透传 null")
        void getUserAuth_nullPassthrough() {
            when(adminUserManagementPort.getAuth(USER_ID)).thenReturn(null);

            assertThat(adapter.getUserAuth(USER_ID)).isNull();
        }
    }

    @Nested
    @DisplayName("操作委托")
    class WriteDelegationTests {

        @Test
        @DisplayName("updateUserStatus 原样委托")
        void updateUserStatus_delegates() {
            adapter.updateUserStatus(USER_ID, "DISABLED");

            verify(adminUserManagementPort).updateStatus(USER_ID, "DISABLED");
        }

        @Test
        @DisplayName("unlockUser 原样委托")
        void unlockUser_delegates() {
            adapter.unlockUser(USER_ID);

            verify(adminUserManagementPort).unlock(USER_ID);
        }

        @Test
        @DisplayName("setUserType 原样委托")
        void setUserType_delegates() {
            adapter.setUserType(USER_ID, "02");

            verify(adminUserManagementPort).setUserType(USER_ID, "02");
        }

        @Test
        @DisplayName("setPassword 原样委托")
        void setPassword_delegates() {
            adapter.setPassword(USER_ID, "$2a$10$encoded");

            verify(adminUserManagementPort).setPassword(USER_ID, "$2a$10$encoded");
        }
    }

    @Nested
    @DisplayName("仪表板翻译")
    class DashboardMappingTests {

        @Test
        @DisplayName("getUserStats 委托并映射统计")
        void getUserStats_maps() {
            when(adminUserManagementPort.getStats()).thenReturn(new AdminUserStats(100L, 5L));

            var result = adapter.getUserStats();

            assertThat(result.totalUsers()).isEqualTo(100L);
            assertThat(result.todayNewUsers()).isEqualTo(5L);
        }

        @Test
        @DisplayName("getRecentUsers 委托并映射最近注册用户")
        void getRecentUsers_maps() {
            AdminRecentUser recent = new AdminRecentUser(
                    USER_ID,
                    "testuser",
                    "小张",
                    "/avatar/test.png",
                    "test@example.com",
                    "13812345678",
                    "01",
                    "普通用户",
                    "NORMAL",
                    "正常",
                    LocalDateTime.of(2026, 8, 15, 9, 0));
            when(adminUserManagementPort.getRecentUsers(5)).thenReturn(List.of(recent));

            var result = adapter.getRecentUsers(5);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).id()).isEqualTo(USER_ID);
            assertThat(result.get(0).username()).isEqualTo("testuser");
            assertThat(result.get(0).userTypeDesc()).isEqualTo("普通用户");
        }
    }
}
