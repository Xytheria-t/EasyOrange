package com.cartethyia.easyorange.user.application.service;

import com.cartethyia.easyorange.common.dto.AuthUser;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.request.ChangePasswordRequest;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.request.UpdateUserRequest;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.response.UserProfileVO;
import com.cartethyia.easyorange.user.adapter.inbound.web.dto.response.UserVO;
import com.cartethyia.easyorange.user.application.assembler.UserAssembler;
import com.cartethyia.easyorange.user.domain.aggregate.User;
import com.cartethyia.easyorange.user.domain.valueobject.UserProfile;
import com.cartethyia.easyorange.user.domain.repository.UserRepository;
import com.cartethyia.easyorange.user.domain.service.PasswordDomainService;
import com.cartethyia.easyorange.user.domain.port.AvatarFilePort;
import com.cartethyia.easyorange.user.domain.port.UserEventPort;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserAppService 测试")
class UserAppServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordDomainService passwordDomainService;

    @Mock
    private AvatarFilePort avatarFilePort;

    @Mock
    private UserAssembler userAssembler;

    @Mock
    private UserEventPort userEventPort;

    private UserAppService userAppService;

    @BeforeEach
    void setUp() {
        userAppService = new UserAppService(
            userRepository,
            passwordDomainService,
            avatarFilePort,
            userAssembler,
            userEventPort
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setSecurityContext(Long userId) {
        AuthUser authUser = AuthUser.builder()
            .userId(userId)
            .username("testuser")
            .roles(Set.of("ROLE_USER"))
            .permissions(Set.of("user:read"))
            .loginTime(System.currentTimeMillis())
            .build();
        UsernamePasswordAuthenticationToken auth =
            new UsernamePasswordAuthenticationToken(authUser, null, Set.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private User buildTestUser() {
        UserProfile profile = new UserProfile(
            "test@example.com",
            "13812345678",
            "张三",
            null,
            com.cartethyia.easyorange.user.domain.shared.enums.Sex.MALE,
            "/avatar/old.png",
            null
        );

        return User.builder()
            .id(1L)
            .username("testuser")
            .password("$2a$10$encoded")
            .userType(com.cartethyia.easyorange.user.domain.shared.enums.UserType.NORMAL)
            .status(com.cartethyia.easyorange.user.domain.shared.enums.UserStatus.NORMAL)
            .profile(profile)
            .build();
    }

    @Nested
    @DisplayName("getUserInfo")
    class GetUserInfoTests {

        @Test
        @DisplayName("应返回 UserProfileVO")
        void shouldReturnUserProfileVO() {
            setSecurityContext(1L);
            User user = buildTestUser();
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            UserProfileVO profileVO = UserProfileVO.builder()
                .id(1L)
                .username("testuser")
                .build();
            when(userAssembler.toProfileVo(any(), any(), any(), anyLong())).thenReturn(profileVO);

            UserProfileVO result = userAppService.getUserInfo();

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getUsername()).isEqualTo("testuser");
        }

        @Test
        @DisplayName("用户不存在时应抛出异常")
        void shouldThrowWhenUserNotFound() {
            setSecurityContext(999L);
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userAppService.getUserInfo())
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户不存在");
        }

        @Test
        @DisplayName("未登录时应抛出异常")
        void shouldThrowWhenNotAuthenticated() {
            SecurityContextHolder.clearContext();

            assertThatThrownBy(() -> userAppService.getUserInfo())
                .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("updateUserInfo")
    class UpdateUserInfoTests {

        @Test
        @DisplayName("应更新并返回 UserVO")
        void shouldUpdateAndReturnUserVO() {
            setSecurityContext(1L);
            User user = buildTestUser();
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(userRepository.update(any(User.class))).thenReturn(true);

            UserVO userVO = UserVO.builder().userId(1L).username("testuser").build();
            when(userAssembler.toVo(any(User.class))).thenReturn(userVO);

            UpdateUserRequest request = new UpdateUserRequest("new@example.com", "13999999999", 1);

            UserVO result = userAppService.updateUserInfo(request);

            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo(1L);
            verify(userRepository).update(any(User.class));
        }

        @Test
        @DisplayName("没有需要更新的字段时应抛出异常")
        void shouldThrowWhenNoUpdateFields() {
            setSecurityContext(1L);
            User user = buildTestUser();
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            UpdateUserRequest request = new UpdateUserRequest(null, null, null);

            assertThatThrownBy(() -> userAppService.updateUserInfo(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("没有需要更新的字段");

            verify(userRepository, never()).update(any());
        }
    }

    @Nested
    @DisplayName("changePassword")
    class ChangePasswordTests {

        @Test
        @DisplayName("应验证并更新密码")
        void shouldValidateAndUpdatePassword() {
            setSecurityContext(1L);
            User user = buildTestUser();
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(passwordDomainService.matches("OldPassword123", "$2a$10$encoded")).thenReturn(true);
            when(passwordDomainService.encode("NewPassword456")).thenReturn("$2a$10$newEncoded");
            when(userRepository.updatePassword(1L, "$2a$10$newEncoded")).thenReturn(true);

            ChangePasswordRequest request = new ChangePasswordRequest("OldPassword123", "NewPassword456");

            userAppService.changePassword(request);

            verify(passwordDomainService).validateDifferentPassword("OldPassword123", "NewPassword456");
            verify(passwordDomainService).matches("OldPassword123", "$2a$10$encoded");
            verify(passwordDomainService).encode("NewPassword456");
            verify(userRepository).updatePassword(1L, "$2a$10$newEncoded");
            verify(userEventPort).publishPasswordChanged(1L);
        }

        @Test
        @DisplayName("旧密码错误时应抛出异常")
        void shouldThrowWhenOldPasswordWrong() {
            setSecurityContext(1L);
            User user = buildTestUser();
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(passwordDomainService.matches("WrongOldPassword", "$2a$10$encoded")).thenReturn(false);

            ChangePasswordRequest request = new ChangePasswordRequest("WrongOldPassword", "NewPassword456");

            assertThatThrownBy(() -> userAppService.changePassword(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("密码错误");

            verify(userRepository, never()).updatePassword(any(), any());
            verify(userEventPort, never()).publishPasswordChanged(any());
        }

        @Test
        @DisplayName("新旧密码相同时应抛出异常")
        void shouldThrowWhenPasswordsAreSame() {
            setSecurityContext(1L);
            User user = buildTestUser();
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            doThrow(BusinessException.of("新密码不能与旧密码相同"))
                .when(passwordDomainService).validateDifferentPassword("SamePassword", "SamePassword");

            ChangePasswordRequest request = new ChangePasswordRequest("SamePassword", "SamePassword");

            assertThatThrownBy(() -> userAppService.changePassword(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("新密码不能与旧密码相同");
        }
    }

    @Nested
    @DisplayName("uploadAvatar")
    class UploadAvatarTests {

        @Test
        @DisplayName("应上传并更新头像")
        void shouldUploadAndUpdateAvatar() {
            setSecurityContext(1L);
            User user = buildTestUser();
            UserProfile newProfile = new UserProfile(
                "test@example.com",
                "13812345678",
                "张三",
                null,
                com.cartethyia.easyorange.user.domain.shared.enums.Sex.MALE,
                "/avatar/new.png",
                null
            );
            User updatedUser = User.builder()
                .id(1L)
                .username("testuser")
                .password("$2a$10$encoded")
                .userType(com.cartethyia.easyorange.user.domain.shared.enums.UserType.NORMAL)
                .status(com.cartethyia.easyorange.user.domain.shared.enums.UserStatus.NORMAL)
                .profile(newProfile)
                .build();

            MockMultipartFile file = new MockMultipartFile(
                "avatar", "new.jpg", "image/jpeg", "fake-image".getBytes());

            when(userRepository.findById(1L)).thenReturn(Optional.of(user)).thenReturn(Optional.of(updatedUser));
            when(avatarFilePort.uploadAvatar(file, 1L)).thenReturn("/avatar/new.png");
            when(userRepository.update(any(User.class))).thenReturn(true);

            UserVO userVO = UserVO.builder().userId(1L).username("testuser").avatar("/avatar/new.png").build();
            when(userAssembler.toVo(updatedUser)).thenReturn(userVO);

            UserVO result = userAppService.uploadAvatar(file);

            assertThat(result).isNotNull();
            assertThat(result.getAvatar()).isEqualTo("/avatar/new.png");
            verify(avatarFilePort).deleteIfExists("/avatar/old.png");
            verify(avatarFilePort).uploadAvatar(file, 1L);
        }

        @Test
        @DisplayName("头像为 null 时应抛出异常")
        void shouldThrowWhenAvatarIsNull() {
            assertThatThrownBy(() -> userAppService.uploadAvatar(null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("头像不能为空");
        }

        @Test
        @DisplayName("头像文件为空时应抛出异常")
        void shouldThrowWhenAvatarIsEmpty() {
            MockMultipartFile emptyFile = new MockMultipartFile(
                "avatar", "empty.jpg", "image/jpeg", new byte[0]);

            assertThatThrownBy(() -> userAppService.uploadAvatar(emptyFile))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("头像不能为空");
        }
    }
}
