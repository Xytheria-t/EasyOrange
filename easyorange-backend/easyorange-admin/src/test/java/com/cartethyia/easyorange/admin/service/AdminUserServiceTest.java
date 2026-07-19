package com.cartethyia.easyorange.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.AdminUserQueryRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.request.UpdateStatusRequest;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.AdminUserResponse;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.user.adapter.outbound.persistence.UserDO;
import com.cartethyia.easyorange.user.adapter.outbound.persistence.UserMapper;
import com.cartethyia.easyorange.user.domain.enums.UserStatus;
import com.cartethyia.easyorange.user.domain.enums.UserType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminUserService 单元测试")
class AdminUserServiceTest {

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private AdminUserService userService;

    private static final String USER_ID = "1";

    private UserDO createTestUser() {
        UserDO user = UserDO.builder()
                .id(USER_ID)
                .username("testuser")
                .nickName("测试用户")
                .email("test@example.com")
                .phone("13800138000")
                .userType(UserType.fromCode("01"))
                .status(UserStatus.NORMAL)
                .createTime(LocalDateTime.now())
                .build();
        user.setDelFlag(0);
        return user;
    }

    @Nested
    @DisplayName("listUsers")
    class ListUsersTests {

        @Test
        @DisplayName("分页查询用户列表")
        void listUsers_defaultParams_returnsPage() {
            AdminUserQueryRequest request = new AdminUserQueryRequest();
            UserDO user = createTestUser();
            Page<UserDO> page = new Page<>(1, 20);

            // must use spy or return real page with records
            when(userMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenAnswer(invocation -> {
                        Page<UserDO> p = invocation.getArgument(0);
                        p.setRecords(List.of(user));
                        p.setTotal(1);
                        return p;
                    });

            PageResult<AdminUserResponse> result = userService.listUsers(request);

            assertThat(result.records()).hasSize(1);
            assertThat(result.records().get(0).getUsername()).isEqualTo("testuser");
            assertThat(result.total()).isEqualTo(1);
        }

        @Test
        @DisplayName("带关键词搜索")
        void listUsers_withKeyword_filtersResults() {
            AdminUserQueryRequest request = new AdminUserQueryRequest();
            request.setKeyword("test");

            when(userMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenAnswer(invocation -> {
                        Page<UserDO> p = invocation.getArgument(0);
                        p.setRecords(List.of(createTestUser()));
                        p.setTotal(1);
                        return p;
                    });

            PageResult<AdminUserResponse> result = userService.listUsers(request);

            assertThat(result.records()).hasSize(1);
        }

        @Test
        @DisplayName("查询结果为空")
        void listUsers_noResults_returnsEmptyPage() {
            AdminUserQueryRequest request = new AdminUserQueryRequest();

            when(userMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenAnswer(invocation -> {
                        Page<UserDO> p = invocation.getArgument(0);
                        p.setRecords(List.of());
                        p.setTotal(0);
                        return p;
                    });

            PageResult<AdminUserResponse> result = userService.listUsers(request);

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
            when(userMapper.selectById(USER_ID)).thenReturn(createTestUser());

            AdminUserResponse vo = userService.getUserDetail(USER_ID);

            assertThat(vo).isNotNull();
            assertThat(vo.getUserId()).isEqualTo(USER_ID);
            assertThat(vo.getUsername()).isEqualTo("testuser");
        }

        @Test
        @DisplayName("用户不存在时抛出异常")
        void getUserDetail_notFound_throws() {
            when(userMapper.selectById(USER_ID)).thenReturn(null);

            assertThatThrownBy(() -> userService.getUserDetail(USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("用户不存在");
        }

        @Test
        @DisplayName("已删除用户抛出异常")
        void getUserDetail_deleted_throws() {
            UserDO deleted = createTestUser();
            deleted.setDelFlag(2);
            when(userMapper.selectById(USER_ID)).thenReturn(deleted);

            assertThatThrownBy(() -> userService.getUserDetail(USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("用户不存在");
        }
    }

    @Nested
    @DisplayName("updateUserStatus")
    class UpdateUserStatusTests {

        @Test
        @DisplayName("更新用户状态成功")
        void updateUserStatus_success() {
            UserDO user = createTestUser();
            when(userMapper.selectById(USER_ID)).thenReturn(user);

            UpdateStatusRequest request = new UpdateStatusRequest();
            request.setStatus(1);

            userService.updateUserStatus(USER_ID, request);

            assertThat(user.getStatus()).isEqualTo(UserStatus.DISABLED);
            verify(userMapper).updateById(user);
        }

        @Test
        @DisplayName("更新不存在的用户抛出异常")
        void updateUserStatus_notFound_throws() {
            when(userMapper.selectById(USER_ID)).thenReturn(null);

            UpdateStatusRequest request = new UpdateStatusRequest();
            request.setStatus(1);

            assertThatThrownBy(() -> userService.updateUserStatus(USER_ID, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("用户不存在");
        }
    }
}
