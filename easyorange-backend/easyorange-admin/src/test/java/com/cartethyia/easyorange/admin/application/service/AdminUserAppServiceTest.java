package com.cartethyia.easyorange.admin.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.admin.domain.exception.AdminDomainException;
import com.cartethyia.easyorange.admin.domain.port.AdminUserPort;
import com.cartethyia.easyorange.admin.domain.port.AdminUserPort.UserDetail;
import com.cartethyia.easyorange.admin.domain.port.AdminUserPort.UserQueryCondition;
import com.cartethyia.easyorange.admin.domain.port.AdminUserPort.UserQueryResult;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminUserAppService 单元测试")
class AdminUserAppServiceTest {

    @Mock
    private AdminUserPort adminUserPort;

    @InjectMocks
    private AdminUserAppService userService;

    private static final String USER_ID = "1";
    private static final String REASON = "风控核查";
    private static final String OPERATOR = "admin-1";

    /** 默认查询条件（关键词/类型/状态/时间全空，默认分页）—— 与 web 层的默认值口径一致。 */
    private static UserQueryCondition condition() {
        return new UserQueryCondition(null, null, null, null, null, 1, 20);
    }

    private UserDetail createTestUser() {
        return new UserDetail(
                USER_ID,
                "testuser",
                "测试用户",
                null,
                "test@example.com",
                "13800138000",
                null,
                "01",
                "普通用户",
                "NORMAL",
                "正常",
                null,
                null,
                LocalDateTime.now(),
                LocalDateTime.now());
    }

    @Nested
    @DisplayName("listUsers")
    class ListUsersTests {

        @Test
        @DisplayName("分页查询用户列表")
        void listUsers_defaultParams_returnsPage() {
            when(adminUserPort.queryUsers(any())).thenReturn(new UserQueryResult(List.of(createTestUser()), 1, 1, 20));

            UserQueryResult result = userService.listUsers(condition());

            assertThat(result.records()).hasSize(1);
            assertThat(result.records().get(0).username()).isEqualTo("testuser");
            assertThat(result.total()).isEqualTo(1);
        }

        @Test
        @DisplayName("带关键词搜索")
        void listUsers_withKeyword_filtersResults() {
            when(adminUserPort.queryUsers(any())).thenReturn(new UserQueryResult(List.of(createTestUser()), 1, 1, 20));

            UserQueryResult result =
                    userService.listUsers(new UserQueryCondition("test", null, null, null, null, 1, 20));

            assertThat(result.records()).hasSize(1);
            // 条件原样透传：关键词归并与分页都由端口负责，服务不改写
            verify(adminUserPort).queryUsers(new UserQueryCondition("test", null, null, null, null, 1, 20));
        }

        @Test
        @DisplayName("查询结果为空")
        void listUsers_noResults_returnsEmptyPage() {
            when(adminUserPort.queryUsers(any())).thenReturn(new UserQueryResult(List.of(), 0, 1, 20));

            UserQueryResult result = userService.listUsers(condition());

            assertThat(result.records()).isEmpty();
            assertThat(result.total()).isZero();
        }
    }

    @Nested
    @DisplayName("getUserDetail")
    class GetUserDetailTests {

        @Test
        @DisplayName("获取用户详情成功")
        void getUserDetail_success() {
            when(adminUserPort.getUserDetail(USER_ID)).thenReturn(createTestUser());

            UserDetail detail = userService.getUserDetail(USER_ID);

            assertThat(detail).isNotNull();
            assertThat(detail.id()).isEqualTo(USER_ID);
            assertThat(detail.username()).isEqualTo("testuser");
        }

        @Test
        @DisplayName("用户不存在时抛出异常")
        void getUserDetail_notFound_throws() {
            when(adminUserPort.getUserDetail(USER_ID)).thenReturn(null);

            assertThatThrownBy(() -> userService.getUserDetail(USER_ID))
                    .isInstanceOf(AdminDomainException.class)
                    .hasMessageContaining("用户不存在");
        }
    }

    @Nested
    @DisplayName("updateUserStatus")
    class UpdateUserStatusTests {

        @Test
        @DisplayName("更新用户状态委托端口")
        void updateUserStatus_success() {
            userService.updateUserStatus(USER_ID, "DISABLED", REASON, OPERATOR);

            verify(adminUserPort).updateUserStatus(USER_ID, "DISABLED", REASON, OPERATOR);
        }
    }
}
