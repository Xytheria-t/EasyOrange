package com.cartethyia.easyorange.user.application.service.profile;

import com.cartethyia.easyorange.common.dto.AuthUser;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.user.application.command.UpdateUserCommand;
import com.cartethyia.easyorange.user.application.dto.UserProfileVO;
import com.cartethyia.easyorange.user.application.dto.UserVO;
import com.cartethyia.easyorange.user.application.assembler.UserAssembler;
import com.cartethyia.easyorange.user.domain.aggregate.User;
import com.cartethyia.easyorange.user.domain.enums.Sex;
import com.cartethyia.easyorange.user.domain.enums.UserStatus;
import com.cartethyia.easyorange.user.domain.enums.UserType;
import com.cartethyia.easyorange.user.domain.port.AvatarFilePort;
import com.cartethyia.easyorange.user.domain.repository.UserRepository;
import com.cartethyia.easyorange.user.domain.valueobject.ContactInfo;
import com.cartethyia.easyorange.user.domain.valueobject.Credentials;
import com.cartethyia.easyorange.user.domain.valueobject.ImmutablePersonalInfo;
import com.cartethyia.easyorange.user.domain.valueobject.LoginInfo;
import com.cartethyia.easyorange.user.domain.valueobject.PersonalInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
@DisplayName("ProfileAppService 测试")
class ProfileAppServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AvatarFilePort avatarFilePort;

    @Mock
    private UserAssembler userAssembler;

    private ProfileAppService profileAppService;

    @BeforeEach
    void setUp() {
        profileAppService = new ProfileAppService(userRepository, avatarFilePort, userAssembler);
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
        ContactInfo contactInfo = new ContactInfo(
            "test@example.com",
            "13812345678"
        );
        PersonalInfo personalInfo = ImmutablePersonalInfo.builder()
            .realName("张三")
            .sex(Sex.MALE)
            .avatar("/avatar/old.png")
            .build();

        return User.builder()
            .id(1L)
            .credentials(new Credentials("testuser", "$2a$10$encoded"))
            .userType(UserType.NORMAL)
            .status(UserStatus.NORMAL)
            .contactInfo(contactInfo)
            .personalInfo(personalInfo)
            .loginInfo(LoginInfo.initial())
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

            UserProfileVO result = profileAppService.getUserInfo();

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getUsername()).isEqualTo("testuser");
        }

        @Test
        @DisplayName("用户不存在时应抛出异常")
        void shouldThrowWhenUserNotFound() {
            setSecurityContext(999L);
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> profileAppService.getUserInfo())
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户不存在");
        }

        @Test
        @DisplayName("未登录时应抛出异常")
        void shouldThrowWhenNotAuthenticated() {
            SecurityContextHolder.clearContext();

            assertThatThrownBy(() -> profileAppService.getUserInfo())
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
            when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
            when(userRepository.findByPhone("13999999999")).thenReturn(Optional.empty());
            when(userRepository.update(any(User.class))).thenReturn(true);

            UserVO userVO = UserVO.builder().userId(1L).username("testuser").build();
            when(userAssembler.toVo(any(User.class))).thenReturn(userVO);

            UpdateUserCommand command = new UpdateUserCommand(null, "new@example.com", "13999999999", 1, null, null);

            UserVO result = profileAppService.updateUserInfo(command);

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

            UpdateUserCommand command = new UpdateUserCommand(null, null, null, null, null, null);

            assertThatThrownBy(() -> profileAppService.updateUserInfo(command))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("没有需要更新的字段");

            verify(userRepository, never()).update(any());
        }

        @Test
        @DisplayName("更新邮箱为已存在的邮箱时应抛出异常")
        void shouldThrowWhenEmailAlreadyExists() {
            setSecurityContext(1L);
            User user = buildTestUser();
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            User otherUser = buildTestUser();
            when(userRepository.findByEmail("taken@example.com")).thenReturn(Optional.of(otherUser));

            UpdateUserCommand command = new UpdateUserCommand(null, "taken@example.com", null, null, null, null);

            assertThatThrownBy(() -> profileAppService.updateUserInfo(command))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("邮箱已被注册");

            verify(userRepository, never()).update(any());
        }

        @Test
        @DisplayName("更新邮箱为当前用户自己的邮箱时不应抛出异常")
        void shouldNotThrowWhenEmailBelongsToCurrentUser() {
            setSecurityContext(1L);
            User user = buildTestUser();
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(userRepository.update(any(User.class))).thenReturn(true);

            UserVO userVO = UserVO.builder().userId(1L).username("testuser").build();
            when(userAssembler.toVo(any(User.class))).thenReturn(userVO);

            UpdateUserCommand command = new UpdateUserCommand(null, "test@example.com", null, null, null, null);

            UserVO result = profileAppService.updateUserInfo(command);

            assertThat(result).isNotNull();
            verify(userRepository).update(any(User.class));
        }

        @Test
        @DisplayName("更新手机号为已存在的手机号时应抛出异常")
        void shouldThrowWhenPhoneAlreadyExists() {
            setSecurityContext(1L);
            User user = buildTestUser();
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            User otherUser = buildTestUser();
            when(userRepository.findByPhone("13800000000")).thenReturn(Optional.of(otherUser));

            UpdateUserCommand command = new UpdateUserCommand(null, null, "13800000000", null, null, null);

            assertThatThrownBy(() -> profileAppService.updateUserInfo(command))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("手机号已被注册");

            verify(userRepository, never()).update(any());
        }

        @Test
        @DisplayName("更新学号为已存在的学号时应抛出异常")
        void shouldThrowWhenStudentIdAlreadyExists() {
            setSecurityContext(1L);
            User user = buildTestUser();
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            User otherUser = buildTestUser();
            when(userRepository.findByStudentId("2024001")).thenReturn(Optional.of(otherUser));

            UpdateUserCommand command = new UpdateUserCommand(null, null, null, null, null, "2024001");

            assertThatThrownBy(() -> profileAppService.updateUserInfo(command))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("学号已被注册");

            verify(userRepository, never()).update(any());
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
            User updatedUser = User.builder()
                .id(1L)
                .credentials(new Credentials("testuser", "$2a$10$encoded"))
                .userType(UserType.NORMAL)
                .status(UserStatus.NORMAL)
                .contactInfo(user.getContactInfo())
                .personalInfo(user.getPersonalInfo())
                .build();

            byte[] content = "fake-image".getBytes();

            when(userRepository.findById(1L)).thenReturn(Optional.of(user)).thenReturn(Optional.of(updatedUser));
            when(avatarFilePort.upload(any(byte[].class), eq("image/jpeg"), eq("new.jpg"), eq(1L))).thenReturn("/avatar/new.png");
            when(userRepository.update(any(User.class))).thenReturn(true);

            UserVO userVO = UserVO.builder().userId(1L).username("testuser").avatar("/avatar/new.png").build();
            when(userAssembler.toVo(updatedUser)).thenReturn(userVO);

            UserVO result = profileAppService.uploadAvatar(content, "image/jpeg", "new.jpg");

            assertThat(result).isNotNull();
            assertThat(result.getAvatar()).isEqualTo("/avatar/new.png");
            verify(avatarFilePort).deleteIfExists("/avatar/old.png");
            verify(avatarFilePort).upload(any(byte[].class), eq("image/jpeg"), eq("new.jpg"), eq(1L));
        }

        @Test
        @DisplayName("头像为 null 时应抛出异常")
        void shouldThrowWhenAvatarIsNull() {
            assertThatThrownBy(() -> profileAppService.uploadAvatar(null, null, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("头像不能为空");
        }

        @Test
        @DisplayName("头像文件为空时应抛出异常")
        void shouldThrowWhenAvatarIsEmpty() {
            assertThatThrownBy(() -> profileAppService.uploadAvatar(new byte[0], "image/jpeg", "empty.jpg"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("头像不能为空");
        }
    }
}