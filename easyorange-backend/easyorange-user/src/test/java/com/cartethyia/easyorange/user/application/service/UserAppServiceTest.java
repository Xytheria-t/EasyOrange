package com.cartethyia.easyorange.user.application.service;

import com.cartethyia.easyorange.common.dto.AuthUser;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.user.application.assembler.UserAssembler;
import com.cartethyia.easyorange.user.domain.model.User;
import com.cartethyia.easyorange.user.domain.repository.UserRepository;
import com.cartethyia.easyorange.user.domain.service.PasswordDomainService;
import com.cartethyia.easyorange.user.dto.request.ChangePasswordRequest;
import com.cartethyia.easyorange.user.dto.request.UpdateUserRequest;
import com.cartethyia.easyorange.user.dto.vo.UserProfileVO;
import com.cartethyia.easyorange.user.dto.vo.UserVO;
import com.cartethyia.easyorange.user.common.enums.Sex;
import com.cartethyia.easyorange.user.common.enums.UserStatus;
import com.cartethyia.easyorange.user.common.enums.UserType;
import com.cartethyia.easyorange.user.infrastructure.event.UserEventPublisher;
import com.cartethyia.easyorange.user.infrastructure.storage.FileStorageAdapter;
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
    private FileStorageAdapter fileStorageAdapter;

    @Mock
    private UserAssembler userAssembler;

    @Mock
    private UserEventPublisher userEventPublisher;

    private UserAppService userAppService;

    @BeforeEach
    void setUp() {
        userAppService = new UserAppService(
            userRepository,
            passwordDomainService,
            fileStorageAdapter,
            userAssembler,
            userEventPublisher
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
        return User.builder()
            .id(1L)
            .username("testuser")
            .password("$2a$10$encoded")
            .userType(UserType.NORMAL)
            .status(UserStatus.NORMAL)
            .email("test@example.com")
            .phone("13812345678")
            .realName("张三")
            .sex(Sex.MALE)
            .avatar("/avatar/old.png")
            .build();
    }

    @Nested
    @DisplayName("getUserInfo")
    class GetUserInfoTests {

        @Test
        @DisplayName("应返回 UserProfileVO")
        void shouldReturnUserProfileVO() {
            // Arrange
            setSecurityContext(1L);
            User user = buildTestUser();
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            UserProfileVO profileVO = UserProfileVO.builder()
                .id(1L)
                .username("testuser")
                .build();
            when(userAssembler.toProfileVo(any(), any(), any(), anyLong())).thenReturn(profileVO);

            // Act
            UserProfileVO result = userAppService.getUserInfo();

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getUsername()).isEqualTo("testuser");
        }

        @Test
        @DisplayName("用户不存在时应抛出异常")
        void shouldThrowWhenUserNotFound() {
            // Arrange
            setSecurityContext(999L);
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> userAppService.getUserInfo())
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户不存在");
        }

        @Test
        @DisplayName("未登录时应抛出异常")
        void shouldThrowWhenNotAuthenticated() {
            // Arrange
            SecurityContextHolder.clearContext();

            // Act & Assert
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
            // Arrange
            setSecurityContext(1L);
            User user = buildTestUser();
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(userRepository.update(any(User.class))).thenReturn(true);

            UserVO userVO = UserVO.builder().userId(1L).username("testuser").build();
            when(userAssembler.toVo(any(User.class))).thenReturn(userVO);

            UpdateUserRequest request = new UpdateUserRequest();
            request.setEmail("new@example.com");
            request.setPhone("13999999999");
            request.setGender(1);

            // Act
            UserVO result = userAppService.updateUserInfo(request);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo(1L);
            verify(userRepository).update(any(User.class));
        }

        @Test
        @DisplayName("没有需要更新的字段时应抛出异常")
        void shouldThrowWhenNoUpdateFields() {
            // Arrange
            setSecurityContext(1L);
            User user = buildTestUser();
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            UpdateUserRequest request = new UpdateUserRequest();

            // Act & Assert
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
            // Arrange
            setSecurityContext(1L);
            User user = buildTestUser();
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(passwordDomainService.matches("OldPassword123", "$2a$10$encoded")).thenReturn(true);
            when(passwordDomainService.encode("NewPassword456")).thenReturn("$2a$10$newEncoded");
            when(userRepository.updatePassword(1L, "$2a$10$newEncoded")).thenReturn(true);

            ChangePasswordRequest request = new ChangePasswordRequest();
            request.setOldPassword("OldPassword123");
            request.setNewPassword("NewPassword456");

            // Act
            userAppService.changePassword(request);

            // Assert
            verify(passwordDomainService).validateDifferentPassword("OldPassword123", "NewPassword456");
            verify(passwordDomainService).matches("OldPassword123", "$2a$10$encoded");
            verify(passwordDomainService).encode("NewPassword456");
            verify(userRepository).updatePassword(1L, "$2a$10$newEncoded");
            verify(userEventPublisher).publishPasswordChanged(1L);
        }

        @Test
        @DisplayName("旧密码错误时应抛出异常")
        void shouldThrowWhenOldPasswordWrong() {
            // Arrange
            setSecurityContext(1L);
            User user = buildTestUser();
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(passwordDomainService.matches("WrongOldPassword", "$2a$10$encoded")).thenReturn(false);

            ChangePasswordRequest request = new ChangePasswordRequest();
            request.setOldPassword("WrongOldPassword");
            request.setNewPassword("NewPassword456");

            // Act & Assert
            assertThatThrownBy(() -> userAppService.changePassword(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("密码错误");

            verify(userRepository, never()).updatePassword(any(), any());
            verify(userEventPublisher, never()).publishPasswordChanged(any());
        }

        @Test
        @DisplayName("新旧密码相同时应抛出异常")
        void shouldThrowWhenPasswordsAreSame() {
            // Arrange
            setSecurityContext(1L);
            User user = buildTestUser();
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            doThrow(BusinessException.of("新密码不能与旧密码相同"))
                .when(passwordDomainService).validateDifferentPassword("SamePassword", "SamePassword");

            ChangePasswordRequest request = new ChangePasswordRequest();
            request.setOldPassword("SamePassword");
            request.setNewPassword("SamePassword");

            // Act & Assert
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
            // Arrange
            setSecurityContext(1L);
            User user = buildTestUser();
            User updatedUser = buildTestUser();
            updatedUser.setAvatar("/avatar/new.png");

            MockMultipartFile file = new MockMultipartFile(
                "avatar", "new.jpg", "image/jpeg", "fake-image".getBytes());

            when(userRepository.findById(1L)).thenReturn(Optional.of(user)).thenReturn(Optional.of(updatedUser));
            when(fileStorageAdapter.uploadAvatar(file, 1L)).thenReturn("/avatar/new.png");
            when(userRepository.update(any(User.class))).thenReturn(true);

            UserVO userVO = UserVO.builder().userId(1L).username("testuser").avatar("/avatar/new.png").build();
            when(userAssembler.toVo(updatedUser)).thenReturn(userVO);

            // Act
            UserVO result = userAppService.uploadAvatar(file);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getAvatar()).isEqualTo("/avatar/new.png");
            verify(fileStorageAdapter).deleteIfExists("/avatar/old.png");
            verify(fileStorageAdapter).uploadAvatar(file, 1L);
        }

        @Test
        @DisplayName("头像为 null 时应抛出异常")
        void shouldThrowWhenAvatarIsNull() {
            // Act & Assert
            assertThatThrownBy(() -> userAppService.uploadAvatar(null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("头像不能为空");
        }

        @Test
        @DisplayName("头像文件为空时应抛出异常")
        void shouldThrowWhenAvatarIsEmpty() {
            // Arrange
            MockMultipartFile emptyFile = new MockMultipartFile(
                "avatar", "empty.jpg", "image/jpeg", new byte[0]);

            // Act & Assert
            assertThatThrownBy(() -> userAppService.uploadAvatar(emptyFile))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("头像不能为空");
        }
    }
}
