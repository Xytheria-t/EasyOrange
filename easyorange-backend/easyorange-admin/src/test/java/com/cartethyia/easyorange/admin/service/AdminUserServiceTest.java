package com.cartethyia.easyorange.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.AdminUserQueryRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.UpdateStatusRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.AdminUserResponse;
import com.cartethyia.easyorange.admin.domain.port.AdminUserQueryPort;
import com.cartethyia.easyorange.admin.domain.port.AdminUserQueryPort.UserDetail;
import com.cartethyia.easyorange.admin.domain.port.AdminUserQueryPort.UserQueryCondition;
import com.cartethyia.easyorange.admin.domain.port.AdminUserQueryPort.UserQueryResult;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.result.PageResult;
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
@DisplayName("AdminUserService 单元测试")
class AdminUserServiceTest {

    @Mock
    private AdminUserQueryPort adminUserQueryPort;

    @InjectMocks
    private AdminUserService userService;

    private static final String USER_ID = "1";

    private UserDetail createTestUser() {
        return new UserDetail(
                USER_ID,
                "testuser",
                "测试用户",
                null,
                "test@example.com",
                "13800138000",
                null,
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
            when(adminUserQueryPort.queryUsers(any()))
                    .thenReturn(new UserQueryResult(List.of(createTestUser()), 1, 1, 20));

            PageResult<AdminUserResponse> result = userService.listUsers(new AdminUserQueryRequest());

            assertThat(result.records()).hasSize(1);
            assertThat(result.records().get(0).getUsername()).isEqualTo("testuser");
            assertThat(result.total()).isEqualTo(1);
        }

        @Test
        @DisplayName("带关键词搜索")
        void listUsers_withKeyword_filtersResults() {
            AdminUserQueryRequest request = new AdminUserQueryRequest();
            request.setKeyword("test");

            when(adminUserQueryPort.queryUsers(any()))
                    .thenReturn(new UserQueryResult(List.of(createTestUser()), 1, 1, 20));

            PageResult<AdminUserResponse> result = userService.listUsers(request);

            assertThat(result.records()).hasSize(1);
            verify(adminUserQueryPort).queryUsers(any(UserQueryCondition.class));
        }

        @Test
        @DisplayName("查询结果为空")
        void listUsers_noResults_returnsEmptyPage() {
            when(adminUserQueryPort.queryUsers(any())).thenReturn(new UserQueryResult(List.of(), 0, 1, 20));

            PageResult<AdminUserResponse> result = userService.listUsers(new AdminUserQueryRequest());

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
            when(adminUserQueryPort.getUserDetail(USER_ID)).thenReturn(createTestUser());

            AdminUserResponse vo = userService.getUserDetail(USER_ID);

            assertThat(vo).isNotNull();
            assertThat(vo.getUserId()).isEqualTo(USER_ID);
            assertThat(vo.getUsername()).isEqualTo("testuser");
        }

        @Test
        @DisplayName("用户不存在时抛出异常")
        void getUserDetail_notFound_throws() {
            when(adminUserQueryPort.getUserDetail(USER_ID)).thenReturn(null);

            assertThatThrownBy(() -> userService.getUserDetail(USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("用户不存在");
        }
    }

    @Nested
    @DisplayName("updateUserStatus")
    class UpdateUserStatusTests {

        @Test
        @DisplayName("更新用户状态委托端口")
        void updateUserStatus_success() {
            UpdateStatusRequest request = new UpdateStatusRequest();
            request.setStatus("DISABLED");

            userService.updateUserStatus(USER_ID, request);

            verify(adminUserQueryPort).updateUserStatus(USER_ID, "DISABLED");
        }
    }
}
