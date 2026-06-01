package com.cartethyia.easyorange.user.application.service;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.framework.util.SecurityContextUtil;
import com.cartethyia.easyorange.framework.util.TestSecurityUtil;
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
import com.cartethyia.easyorange.user.domain.valueobject.AuditInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProfileAppService 测试")
class ProfileAppServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AvatarFilePort avatarFilePort;

    private ProfileAppService profileAppService;

    private static final Long USER_ID = 1L;
    private static final String USERNAME = "testuser";
    private static final String EMAIL = "test@example.com";
    private static final String PHONE = "13812345678";

    @BeforeEach
    void setUp() {
        profileAppService = new ProfileAppService(userRepository, avatarFilePort);
    }

    @AfterEach
    void tearDown() {
        TestSecurityUtil.clearSecurityContext();
    }

    private User buildTestUser() {
        ContactInfo contactInfo = new ContactInfo(EMAIL, PHONE);
        PersonalInfo personalInfo = ImmutablePersonalInfo.builder()
            .realName("张三")
            .nickName("小张")
            .sex(Sex.MALE)
            .avatar("/avatar/old.png")
            .studentId("2021001")
            .build();

        return User.builder()
            .id(USER_ID)
            .credentials(new Credentials(USERNAME, "encodedPassword"))
            .userType(UserType.NORMAL)
            .status(UserStatus.NORMAL)
            .contactInfo(contactInfo)
            .personalInfo(personalInfo)
            .loginInfo(LoginInfo.empty())
            .build();
    }

    @Nested
    @DisplayName("getCurrentUser")
    class GetCurrentUser {

        @Test
        @DisplayName("成功获取当前用户信息")
        void success() {
            setSecurityContext();
            User user = buildTestUser();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

            User result = profileAppService.getCurrentUser();

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(USER_ID);
        }

        @Test
        @DisplayName("用户不存在时抛出异常")
        void userNotFound() {
            setSecurityContext();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> profileAppService.getCurrentUser())
                .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("updateUserInfo")
    class UpdateUserInfo {

        @Test
        @DisplayName("成功更新邮箱")
        void updateEmail() {
            setSecurityContext();
            User user = buildTestUser();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(userRepository.update(any(User.class))).thenReturn(true);

            profileAppService.updateUserInfo(null, "new@example.com", null, null, null, null);
        }

        @Test
        @DisplayName("更新已存在的邮箱时抛出异常")
        void emailAlreadyExists() {
            setSecurityContext();
            User user = buildTestUser();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(userRepository.findByEmail("existing@example.com")).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> profileAppService.updateUserInfo(null, "existing@example.com", null, null, null, null))
                .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("更新已存在的手机号时抛出异常")
        void phoneAlreadyExists() {
            setSecurityContext();
            User user = buildTestUser();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(userRepository.findByPhone("13900000000")).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> profileAppService.updateUserInfo(null, null, "13900000000", null, null, null))
                .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("更新已存在的学号时抛出异常")
        void studentIdAlreadyExists() {
            setSecurityContext();
            User user = buildTestUser();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(userRepository.findByStudentId("2022001")).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> profileAppService.updateUserInfo(null, null, null, null, null, "2022001"))
                .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("没有需要更新的字段时抛出异常")
        void noFieldsToUpdate() {
            setSecurityContext();
            User user = buildTestUser();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> profileAppService.updateUserInfo(null, null, null, null, null, null))
                .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("更新失败时抛出异常")
        void updateFails() {
            setSecurityContext();
            User user = buildTestUser();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

            when(userRepository.update(any(User.class))).thenReturn(false);

            assertThatThrownBy(() -> profileAppService.updateUserInfo("新昵称", null, null, null, null, null))
                .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("uploadAvatar")
    class UploadAvatar {

        @Test
        @DisplayName("成功上传头像")
        void success() {
            setSecurityContext();
            User user = buildTestUser();
            User updatedUser = user.changeAvatar("/avatar/new.png", USER_ID);

            when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user))
                .thenReturn(Optional.of(updatedUser));

            byte[] content = "image-data".getBytes();
            when(avatarFilePort.upload(content, "image/png", "avatar.png", USER_ID))
                .thenReturn("/avatar/new.png");
            when(userRepository.update(any(User.class))).thenReturn(true);

            profileAppService.uploadAvatar(content, "image/png", "avatar.png");

            verify(avatarFilePort).deleteIfExists("/avatar/old.png");
        }

        @Test
        @DisplayName("头像为空时抛出异常")
        void emptyContent() {
            assertThatThrownBy(() -> profileAppService.uploadAvatar(new byte[0], "image/png", "avatar.png"))
                .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("头像超过5MB时抛出异常")
        void exceedsMaxSize() {
            byte[] content = new byte[6 * 1024 * 1024];
            assertThatThrownBy(() -> profileAppService.uploadAvatar(content, "image/png", "avatar.png"))
                .isInstanceOf(BusinessException.class);
        }
    }

    // ==================== Test Helpers ====================

    private void setSecurityContext() {
        TestSecurityUtil.setSecurityContext(USER_ID);
    }
}
