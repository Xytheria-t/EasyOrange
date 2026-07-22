package com.cartethyia.easyorange.user.domain.aggregate;

import com.cartethyia.easyorange.user.domain.enums.Sex;
import com.cartethyia.easyorange.user.domain.enums.UserStatus;
import com.cartethyia.easyorange.user.domain.enums.UserType;
import com.cartethyia.easyorange.user.domain.valueobject.ContactInfo;
import com.cartethyia.easyorange.user.domain.valueobject.Credentials;
import com.cartethyia.easyorange.user.domain.valueobject.LoginInfo;
import com.cartethyia.easyorange.user.domain.valueobject.PersonalInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("User 领域模型测试")
class UserTest {

    @Nested
    @DisplayName("create")
    class CreateTests {

        @Test
        @DisplayName("应创建正常状态的普通用户，昵称默认与用户名相同")
        void shouldCreateNormalUser() {
            User user = User.create("testuser", "encodedPassword");

            assertThat(user.getUsername()).isEqualTo("testuser");
            assertThat(user.getPassword()).isEqualTo("encodedPassword");
            assertThat(user.getUserType()).isEqualTo(UserType.NORMAL);
            assertThat(user.getStatus()).isEqualTo(UserStatus.NORMAL);
            assertThat(user.getPersonalInfo()).isNotNull();
            assertThat(user.getPersonalInfo().nickName()).isEqualTo("testuser");
        }

        @Test
        @DisplayName("用户名为空应抛出异常")
        void shouldThrowWhenUsernameIsNull() {
            assertThatThrownBy(() -> User.create(null, "password"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("用户名");
        }

        @Test
        @DisplayName("密码为空应抛出异常")
        void shouldThrowWhenPasswordIsNull() {
            assertThatThrownBy(() -> User.create("testuser", null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("密码");
        }
    }

    @Nested
    @DisplayName("changePassword")
    class ChangePasswordTests {

        @Test
        @DisplayName("应更新密码和密码修改时间")
        void shouldUpdatePasswordAndPwdUpdateDate() {
            User user = User.builder()
                .id("1")
                .credentials(new Credentials("testuser", "oldEncodedPassword"))
                .loginInfo(LoginInfo.empty())
                .build();

            User updatedUser = user.changePassword("newEncodedPassword", "1");

            assertThat(updatedUser.getPassword()).isEqualTo("newEncodedPassword");
            assertThat(updatedUser.getLoginInfo()).isNotNull();
            assertThat(updatedUser.getLoginInfo().pwdUpdateDate()).isNotNull();
        }

        @Test
        @DisplayName("多次修改密码应覆盖旧密码")
        void shouldOverridePreviousPassword() {
            User user = User.builder()
                .id("1")
                .credentials(new Credentials("testuser", "firstPassword"))
                .loginInfo(LoginInfo.empty())
                .build();

            User secondUser = user.changePassword("secondPassword", "1");
            User thirdUser = secondUser.changePassword("thirdPassword", "1");

            assertThat(thirdUser.getPassword()).isEqualTo("thirdPassword");
        }

        @Test
        @DisplayName("密码为空应抛出异常")
        void shouldThrowWhenPasswordIsNull() {
            User user = User.builder()
                .id("1")
                .credentials(new Credentials("testuser", "oldPassword"))
                .loginInfo(LoginInfo.empty())
                .build();

            assertThatThrownBy(() -> user.changePassword(null, "1"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("新密码");
        }
    }

    @Nested
    @DisplayName("recordLogin")
    class RecordLoginTests {

        @Test
        @DisplayName("应更新登录IP和登录时间")
        void shouldUpdateLoginIpAndLoginDate() {
            User user = User.builder()
                .id("1")
                .credentials(new Credentials("testuser", "password"))
                .loginInfo(LoginInfo.empty())
                .build();

            User updatedUser = user.recordLogin("192.168.1.1");

            assertThat(updatedUser.getLoginInfo()).isNotNull();
            assertThat(updatedUser.getLoginInfo().loginIp()).isEqualTo("192.168.1.1");
            assertThat(updatedUser.getLoginInfo().loginDate()).isNotNull();
        }

        @Test
        @DisplayName("多次更新应覆盖之前的登录信息")
        void shouldOverridePreviousLoginInfo() {
            User user = User.builder()
                .id("1")
                .credentials(new Credentials("testuser", "password"))
                .loginInfo(LoginInfo.empty())
                .build();

            User firstLogin = user.recordLogin("10.0.0.1");
            User secondLogin = firstLogin.recordLogin("10.0.0.2");

            assertThat(secondLogin.getLoginInfo().loginIp()).isEqualTo("10.0.0.2");
        }
    }

    @Nested
    @DisplayName("updateContactInfo / updatePersonalInfo")
    class UpdateProfileTests {

        @Test
        @DisplayName("应更新邮箱、手机、性别和学号")
        void shouldUpdateEmailPhoneSexAndStudentId() {
            User user = User.builder()
                .id("1")
                .credentials(new Credentials("testuser", "password"))
                .contactInfo(ContactInfo.empty())
                .personalInfo(PersonalInfo.empty())
                .build();

            User updatedUser = user.updateContactInfo(new ContactUpdateSpec("new@example.com", "13999999999"), "1")
                .updatePersonalInfo(new PersonalUpdateSpec(null, null, Sex.FEMALE, "2024001"), "1");

            assertThat(updatedUser.getContactInfo().email()).isEqualTo("new@example.com");
            assertThat(updatedUser.getContactInfo().phone()).isEqualTo("13999999999");
            assertThat(updatedUser.getPersonalInfo().sex()).isEqualTo(Sex.FEMALE);
            assertThat(updatedUser.getPersonalInfo().studentId()).isEqualTo("2024001");
            assertThat(updatedUser.getAuditInfo()).isNotNull();
        }

        @Test
        @DisplayName("空值不应覆盖已有字段")
        void shouldNotOverrideWithBlankValues() {
            ContactInfo contactInfo = new ContactInfo("old@example.com", "13812345678");
            PersonalInfo personalInfo = PersonalInfo.builder().build();
            User user = User.builder()
                .id("1")
                .credentials(new Credentials("testuser", "password"))
                .contactInfo(contactInfo)
                .personalInfo(personalInfo)
                .build();

            User updatedUser = user.updateContactInfo(new ContactUpdateSpec("", ""), "1");

            assertThat(updatedUser.getContactInfo().email()).isEqualTo("old@example.com");
            assertThat(updatedUser.getContactInfo().phone()).isEqualTo("13812345678");
        }
    }

    @Nested
    @DisplayName("changeAvatar")
    class ChangeAvatarTests {

        @Test
        @DisplayName("应更新头像URL")
        void shouldUpdateAvatarUrl() {
            PersonalInfo personalInfo = PersonalInfo.builder().avatar("/avatar/old.png").build();
            User user = User.builder()
                .id("1")
                .credentials(new Credentials("testuser", "password"))
                .personalInfo(personalInfo)
                .build();

            User updatedUser = user.changeAvatar("/avatar/new.png", "1");

            assertThat(updatedUser.getPersonalInfo().avatar()).isEqualTo("/avatar/new.png");
            assertThat(updatedUser.getAuditInfo()).isNotNull();
        }

        @Test
        @DisplayName("头像URL为空应抛出异常")
        void shouldThrowWhenAvatarUrlIsNull() {
            User user = User.builder()
                .id("1")
                .credentials(new Credentials("testuser", "password"))
                .personalInfo(PersonalInfo.empty())
                .build();

            assertThatThrownBy(() -> user.changeAvatar(null, "1"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("头像地址");
        }
    }

    @Nested
    @DisplayName("isEnabled")
    class IsEnabledTests {

        @Test
        @DisplayName("正常状态用户应返回 true")
        void shouldReturnTrueForNormalStatus() {
            User user = User.builder()
                .status(UserStatus.NORMAL)
                .build();

            assertThat(user.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("非正常状态用户应返回 false")
        void shouldReturnFalseForNonNormalStatus() {
            User user = User.builder()
                .status(UserStatus.DISABLED)
                .build();

            assertThat(user.isEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("assignId")
    class AssignIdTests {

        @Test
        @DisplayName("应设置用户ID")
        void shouldAssignId() {
            User user = User.builder()
                .credentials(new Credentials("testuser", "password"))
                .build();

            User updatedUser = user.assignId("42");

            assertThat(updatedUser.getId()).isEqualTo("42");
        }

        @Test
        @DisplayName("ID为空应抛出异常")
        void shouldThrowWhenIdIsNull() {
            User user = User.builder()
                .credentials(new Credentials("testuser", "password"))
                .build();

            assertThatThrownBy(() -> user.assignId(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("用户ID");
        }
    }

}
