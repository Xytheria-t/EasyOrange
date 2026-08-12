package com.cartethyia.easyorange.user.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.cartethyia.easyorange.common.event.DomainEventPublisher;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.user.application.service.ProfileAppService.UpdateCommand;
import com.cartethyia.easyorange.user.domain.aggregate.User;
import com.cartethyia.easyorange.user.domain.aggregate.UserTestFixture;
import com.cartethyia.easyorange.user.domain.enums.UserResultCode;
import com.cartethyia.easyorange.user.domain.port.AvatarFilePort;
import com.cartethyia.easyorange.user.domain.repository.UserRepository;
import com.cartethyia.easyorange.user.domain.service.ProfileUpdateService;
import com.cartethyia.easyorange.user.domain.valueobject.Avatar;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProfileAppService 测试")
class ProfileAppServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AvatarFilePort avatarFilePort;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    private ProfileAppService profileAppService;
    private ProfileUpdateService profileUpdateService;

    private static final String USER_ID = UserTestFixture.USER_ID;

    @BeforeEach
    void setUp() {
        profileUpdateService = new ProfileUpdateService(userRepository);
        profileAppService =
                new ProfileAppService(userRepository, avatarFilePort, domainEventPublisher, profileUpdateService);
    }

    @Nested
    @DisplayName("getCurrentUser")
    class GetCurrentUser {

        @Test
        @DisplayName("成功获取当前用户信息")
        void success() {
            User user = UserTestFixture.normalUser();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

            User result = profileAppService.getCurrentUser(USER_ID);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(USER_ID);
        }

        @Test
        @DisplayName("用户不存在时抛出异常")
        void userNotFound() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> profileAppService.getCurrentUser(USER_ID)).isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("updateUserInfo")
    class UpdateUserInfo {

        @Test
        @DisplayName("成功更新邮箱")
        void updateEmail() {
            User user = UserTestFixture.normalUser();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

            profileAppService.updateUserInfo(
                    USER_ID, new UpdateCommand(null, "new@example.com", null, null, null, null));
        }

        @Test
        @DisplayName("更新已存在的邮箱时抛出异常")
        void emailAlreadyExists() {
            User user = UserTestFixture.normalUser();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(userRepository.findByEmail("existing@example.com")).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> profileAppService.updateUserInfo(
                            USER_ID, new UpdateCommand(null, "existing@example.com", null, null, null, null)))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("更新已存在的手机号时抛出异常")
        void phoneAlreadyExists() {
            User user = UserTestFixture.normalUser();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(userRepository.findByPhone("13900000000")).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> profileAppService.updateUserInfo(
                            USER_ID, new UpdateCommand(null, null, "13900000000", null, null, null)))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("更新已存在的学号时抛出异常")
        void studentIdAlreadyExists() {
            User user = UserTestFixture.normalUser();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(userRepository.findByStudentId("2022001")).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> profileAppService.updateUserInfo(
                            USER_ID, new UpdateCommand(null, null, null, null, null, "2022001")))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("没有需要更新的字段时抛出异常")
        void noFieldsToUpdate() {
            User user = UserTestFixture.normalUser();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> profileAppService.updateUserInfo(
                            USER_ID, new UpdateCommand(null, null, null, null, null, null)))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("uploadAvatar")
    class UploadAvatar {

        @Test
        @DisplayName("成功上传头像")
        void success() {
            User user = UserTestFixture.normalUser();
            byte[] content = "image-data".getBytes();
            Avatar avatar = Avatar.uploaded("/avatar/new.png", content, "image/png");
            User updatedUser = user.changeAvatar(avatar, USER_ID);

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(avatarFilePort.upload(content, "image/png", "avatar.png", USER_ID))
                    .thenReturn("/avatar/new.png");
            var result = profileAppService.uploadAvatar(USER_ID, content, "image/png", "avatar.png");

            assertThat(result.getId()).isEqualTo(USER_ID);
            assertThat(result.getPersonalInfo().avatar()).isEqualTo("/avatar/new.png");
            verify(avatarFilePort).deleteIfExists("/avatar/test.png");
        }

        @Test
        @DisplayName("头像为空时抛出异常")
        void emptyContent() {
            assertThatThrownBy(() -> profileAppService.uploadAvatar(USER_ID, new byte[0], "image/png", "avatar.png"))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getCode())
                    .isEqualTo(UserResultCode.AVATAR_EMPTY.getCode());
        }

        @Test
        @DisplayName("头像超过5MB时抛出异常")
        void exceedsMaxSize() {
            byte[] content = new byte[6 * 1024 * 1024];
            assertThatThrownBy(() -> profileAppService.uploadAvatar(USER_ID, content, "image/png", "avatar.png"))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getCode())
                    .isEqualTo(UserResultCode.AVATAR_TOO_LARGE.getCode());
        }
    }
}
