package com.cartethyia.easyorange.user.domain.aggregate;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.user.domain.event.PasswordChangedEvent;
import com.cartethyia.easyorange.user.domain.event.UserRegisteredEvent;
import com.cartethyia.easyorange.user.domain.valueobject.Email;
import com.cartethyia.easyorange.user.domain.valueobject.Nickname;
import com.cartethyia.easyorange.user.domain.valueobject.Password;
import com.cartethyia.easyorange.user.domain.valueobject.Phone;
import com.cartethyia.easyorange.user.domain.valueobject.UserId;
import com.cartethyia.easyorange.user.entity.User;
import com.cartethyia.easyorange.user.enums.AccountType;
import com.cartethyia.easyorange.user.enums.UserStatus;
import com.cartethyia.easyorange.user.enums.UserType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("UserAggregate 聚合根测试")
class UserAggregateTest {

    @Nested
    @DisplayName("register - 用户注册")
    class RegisterTests {

        @Test
        @DisplayName("register 创建 UserRegisteredEvent")
        void register_validParams_returnsUserRegisteredEvent() {
            String username = "testuser";
            Password password = Password.fromRaw("Abc123");
            String loginType = AccountType.WEB.getCode();

            UserRegisteredEvent event = UserAggregate.register(username, password, loginType);

            assertThat(event).isNotNull();
            assertThat(event.getUserId()).isNotNull();
            assertThat(event.getUsername()).isEqualTo(username);
            assertThat(event.getAggregateType()).isEqualTo("User");
            assertThat(event.eventType()).isEqualTo("UserRegistered");
        }

        @Test
        @DisplayName("register 生成唯一的 UserId")
        void register_multipleCalls_generatesUniqueUserIds() throws InterruptedException {
            Password password = Password.fromRaw("Abc123");

            UserRegisteredEvent event1 = UserAggregate.register("user1", password, AccountType.WEB.getCode());
            Thread.sleep(2); // 确保时间戳不同
            UserRegisteredEvent event2 = UserAggregate.register("user2", password, AccountType.WEB.getCode());

            assertThat(event1.getUserId()).isNotEqualTo(event2.getUserId());
        }

        @Test
        @DisplayName("register 用户名为空抛出异常")
        void register_blankUsername_throws() {
            Password password = Password.fromRaw("Abc123");

            assertThatThrownBy(() -> UserAggregate.register("", password, AccountType.WEB.getCode()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户名不能为空");
        }

        @Test
        @DisplayName("register 用户名为 null 抛出异常")
        void register_nullUsername_throws() {
            Password password = Password.fromRaw("Abc123");

            assertThatThrownBy(() -> UserAggregate.register(null, password, AccountType.WEB.getCode()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户名不能为空");
        }

        @Test
        @DisplayName("register 密码为 null 抛出异常")
        void register_nullPassword_throws() {
            assertThatThrownBy(() -> UserAggregate.register("username", null, AccountType.WEB.getCode()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("密码不能为空");
        }
    }

    @Nested
    @DisplayName("changePassword - 修改密码")
    class ChangePasswordTests {

        @Test
        @DisplayName("changePassword 返回 PasswordChangedEvent")
        void changePassword_validPassword_returnsEvent() {
            UserAggregate aggregate = createTestAggregate();
            Password newPassword = Password.fromRaw("NewPass1");

            PasswordChangedEvent event = aggregate.changePassword(newPassword);

            assertThat(event).isNotNull();
            assertThat(event.getUserId()).isEqualTo(aggregate.getId());
            assertThat(event.getAggregateType()).isEqualTo("User");
            assertThat(event.eventType()).isEqualTo("PasswordChanged");
        }

        @Test
        @DisplayName("changePassword 更新聚合根密码")
        void changePassword_updatesAggregatePassword() {
            UserAggregate aggregate = createTestAggregate();
            Password originalPassword = aggregate.getPassword();
            Password newPassword = Password.fromRaw("NewPass1");

            aggregate.changePassword(newPassword);

            assertThat(aggregate.getPassword()).isNotSameAs(originalPassword);
            assertThat(aggregate.getPassword().value()).isEqualTo("NewPass1");
        }

        @Test
        @DisplayName("changePassword 更新密码更新时间")
        void changePassword_updatesPwdUpdateDate() {
            UserAggregate aggregate = createTestAggregate();
            Password newPassword = Password.fromRaw("NewPass1");
            LocalDateTime beforeChange = LocalDateTime.now().minusSeconds(1);

            aggregate.changePassword(newPassword);

            assertThat(aggregate.getPwdUpdateDate()).isAfter(beforeChange);
        }
    }

    @Nested
    @DisplayName("updateProfile - 更新个人资料")
    class UpdateProfileTests {

        @Test
        @DisplayName("updateProfile 更新邮箱")
        void updateProfile_withEmail_updatesEmail() {
            UserAggregate aggregate = createTestAggregate();
            Email newEmail = Email.of("new@test.com");

            aggregate.updateProfile(newEmail, null, null);

            assertThat(aggregate.getEmail()).isEqualTo(newEmail);
        }

        @Test
        @DisplayName("updateProfile 更新手机号")
        void updateProfile_withPhone_updatesPhone() {
            UserAggregate aggregate = createTestAggregate();
            Phone newPhone = Phone.of("13912345678");

            aggregate.updateProfile(null, newPhone, null);

            assertThat(aggregate.getPhone()).isEqualTo(newPhone);
        }

        @Test
        @DisplayName("updateProfile 更新昵称")
        void updateProfile_withNickname_updatesNickname() {
            UserAggregate aggregate = createTestAggregate();
            Nickname newNickname = Nickname.of("NewNickname");

            aggregate.updateProfile(null, null, newNickname);

            assertThat(aggregate.getNickname()).isEqualTo(newNickname);
        }

        @Test
        @DisplayName("updateProfile 同时更新多个字段")
        void updateProfile_withAllFields_updatesAll() {
            UserAggregate aggregate = createTestAggregate();
            Email email = Email.of("new@test.com");
            Phone phone = Phone.of("13912345678");
            Nickname nickname = Nickname.of("NewNick");

            aggregate.updateProfile(email, phone, nickname);

            assertThat(aggregate.getEmail()).isEqualTo(email);
            assertThat(aggregate.getPhone()).isEqualTo(phone);
            assertThat(aggregate.getNickname()).isEqualTo(nickname);
        }

        @Test
        @DisplayName("updateProfile 传入 null 不更新对应字段")
        void updateProfile_withNull_keepsOriginal() {
            UserAggregate aggregate = createTestAggregate();
            Email originalEmail = aggregate.getEmail();
            Phone originalPhone = aggregate.getPhone();
            Nickname originalNickname = aggregate.getNickname();

            aggregate.updateProfile(null, null, null);

            assertThat(aggregate.getEmail()).isEqualTo(originalEmail);
            assertThat(aggregate.getPhone()).isEqualTo(originalPhone);
            assertThat(aggregate.getNickname()).isEqualTo(originalNickname);
        }
    }

    @Nested
    @DisplayName("updateLoginInfo - 更新登录信息")
    class UpdateLoginInfoTests {

        @Test
        @DisplayName("updateLoginInfo 更新登录IP")
        void updateLoginInfo_updatesLoginIp() {
            UserAggregate aggregate = createTestAggregate();
            String newLoginIp = "192.168.1.100";

            aggregate.updateLoginInfo(newLoginIp);

            assertThat(aggregate.getLoginIp()).isEqualTo(newLoginIp);
        }

        @Test
        @DisplayName("updateLoginInfo 更新登录时间")
        void updateLoginInfo_updatesLoginDate() {
            UserAggregate aggregate = createTestAggregate();
            LocalDateTime beforeUpdate = LocalDateTime.now().minusSeconds(1);

            aggregate.updateLoginInfo("127.0.0.1");

            assertThat(aggregate.getLoginDate()).isAfter(beforeUpdate);
        }
    }

    @Nested
    @DisplayName("disable / enable / lock - 状态变更")
    class StatusChangeTests {

        @Test
        @DisplayName("disable 设置状态为禁用")
        void disable_setsStatusToDisabled() {
            UserAggregate aggregate = createTestAggregate();

            aggregate.disable();

            assertThat(aggregate.getStatus()).isEqualTo(UserStatus.DISABLED.getCode());
        }

        @Test
        @DisplayName("enable 设置状态为正常")
        void enable_setsStatusToNormal() {
            UserAggregate aggregate = createTestAggregate();
            aggregate.disable();

            aggregate.enable();

            assertThat(aggregate.getStatus()).isEqualTo(UserStatus.NORMAL.getCode());
        }

        @Test
        @DisplayName("lock 设置状态为锁定")
        void lock_setsStatusToLocked() {
            UserAggregate aggregate = createTestAggregate();

            aggregate.lock();

            assertThat(aggregate.getStatus()).isEqualTo(UserStatus.LOCKED.getCode());
        }
    }

    @Nested
    @DisplayName("from - 静态工厂方法")
    class FromTests {

        @Test
        @DisplayName("from 创建完整的 UserAggregate")
        void from_withAllParams_createsAggregate() {
            UserId userId = UserId.of(1L);
            String username = "testuser";
            Password password = Password.fromRaw("Abc123");
            Email email = Email.of("test@test.com");
            Phone phone = Phone.of("13800138000");
            Nickname nickname = Nickname.of("TestUser");
            String userType = UserType.ADMIN.getCode();
            String status = UserStatus.NORMAL.getCode();
            String loginType = AccountType.WEB.getCode();

            UserAggregate aggregate = UserAggregate.from(
                userId, username, password, email, phone, nickname, userType, status, loginType
            );

            assertThat(aggregate.getId()).isEqualTo(userId);
            assertThat(aggregate.getUsername()).isEqualTo(username);
            assertThat(aggregate.getEmail()).isEqualTo(email);
            assertThat(aggregate.getPhone()).isEqualTo(phone);
            assertThat(aggregate.getNickname()).isEqualTo(nickname);
            assertThat(aggregate.getUserType()).isEqualTo(userType);
            assertThat(aggregate.getStatus()).isEqualTo(status);
            assertThat(aggregate.getLoginType()).isEqualTo(loginType);
        }
    }

    @Nested
    @DisplayName("fromEntity / toEntity - 实体转换")
    class EntityConversionTests {

        @Test
        @DisplayName("fromEntity 正确转换 User 实体")
        void fromEntity_validUser_returnsAggregate() {
            User entity = User.builder()
                .id(1L)
                .username("testuser")
                .password("$2a$10$encoded")
                .email("test@test.com")
                .phone("13800138000")
                .nickName("TestNick")
                .userType(UserType.NORMAL.getCode())
                .status(UserStatus.NORMAL.getCode())
                .loginType(AccountType.WEB.getCode())
                .build();

            UserAggregate aggregate = UserAggregate.fromEntity(entity);

            assertThat(aggregate).isNotNull();
            assertThat(aggregate.getId().value()).isEqualTo(1L);
            assertThat(aggregate.getUsername()).isEqualTo("testuser");
            assertThat(aggregate.getEmail().value()).isEqualTo("test@test.com");
            assertThat(aggregate.getPhone().value()).isEqualTo("13800138000");
            assertThat(aggregate.getNickname().value()).isEqualTo("TestNick");
        }

        @Test
        @DisplayName("fromEntity 处理 null 实体返回 null")
        void fromEntity_nullUser_returnsNull() {
            assertThat(UserAggregate.fromEntity(null)).isNull();
        }

        @Test
        @DisplayName("fromEntity 处理无效邮箱字段")
        void fromEntity_invalidEmail_ignoresEmail() {
            User entity = User.builder()
                .id(1L)
                .username("testuser")
                .password("encoded")
                .email("invalid-email")
                .build();

            UserAggregate aggregate = UserAggregate.fromEntity(entity);

            assertThat(aggregate.getEmail()).isNull();
        }

        @Test
        @DisplayName("fromEntity 处理无效手机号字段")
        void fromEntity_invalidPhone_ignoresPhone() {
            User entity = User.builder()
                .id(1L)
                .username("testuser")
                .password("encoded")
                .phone("12345")
                .build();

            UserAggregate aggregate = UserAggregate.fromEntity(entity);

            assertThat(aggregate.getPhone()).isNull();
        }

        @Test
        @DisplayName("fromEntity 处理无效昵称字段")
        void fromEntity_invalidNickname_ignoresNickname() {
            User entity = User.builder()
                .id(1L)
                .username("testuser")
                .password("encoded")
                .nickName("")
                .build();

            UserAggregate aggregate = UserAggregate.fromEntity(entity);

            assertThat(aggregate.getNickname()).isNull();
        }

        @Test
        @DisplayName("fromEntity 处理 null 字段")
        void fromEntity_nullFields_handlesGracefully() {
            User entity = User.builder()
                .id(1L)
                .username("testuser")
                .password("$2a$10$encoded")
                .build();

            UserAggregate aggregate = UserAggregate.fromEntity(entity);

            assertThat(aggregate).isNotNull();
            assertThat(aggregate.getEmail()).isNull();
            assertThat(aggregate.getPhone()).isNull();
            assertThat(aggregate.getNickname()).isNull();
        }

        @Test
        @DisplayName("toEntity 正确转换聚合根为 User 实体")
        void toEntity_validAggregate_returnsEntity() {
            UserAggregate aggregate = createTestAggregate();

            User entity = aggregate.toEntity();

            assertThat(entity.getId()).isEqualTo(aggregate.getId().value());
            assertThat(entity.getUsername()).isEqualTo(aggregate.getUsername());
            assertThat(entity.getEmail()).isEqualTo(aggregate.getEmail().value());
            assertThat(entity.getPhone()).isEqualTo(aggregate.getPhone().value());
            assertThat(entity.getNickName()).isEqualTo(aggregate.getNickname().value());
        }

        @Test
        @DisplayName("toEntity 处理 null 字段")
        void toEntity_withNullFields_returnsEntityWithNulls() {
            UserAggregate aggregate = UserAggregate.builder()
                .id(new UserId(1L))
                .username("testuser")
                .password(Password.fromRaw("Abc123"))
                .build();

            User entity = aggregate.toEntity();

            assertThat(entity.getEmail()).isNull();
            assertThat(entity.getPhone()).isNull();
            assertThat(entity.getNickName()).isNull();
        }
    }

    @Nested
    @DisplayName("withId - 不可变 ID 变更")
    class WithIdTests {

        @Test
        @DisplayName("withId 返回新的聚合根实例")
        void withId_returnsNewInstance() {
            UserAggregate original = createTestAggregate();
            Long newId = 999L;

            UserAggregate updated = original.withId(newId);

            assertThat(updated).isNotSameAs(original);
            assertThat(updated.getId().value()).isEqualTo(newId);
            assertThat(original.getId().value()).isNotEqualTo(newId);
        }

        @Test
        @DisplayName("withId 保留其他字段不变")
        void withId_preservesOtherFields() {
            UserAggregate original = createTestAggregate();
            String originalUsername = original.getUsername();

            UserAggregate updated = original.withId(999L);

            assertThat(updated.getUsername()).isEqualTo(originalUsername);
            assertThat(updated.getEmail()).isEqualTo(original.getEmail());
        }
    }

    @Nested
    @DisplayName("Builder - 建造者模式")
    class BuilderTests {

        @Test
        @DisplayName("使用 Builder 构建完整的聚合根")
        void builder_withAllFields_createsAggregate() {
            UserAggregate aggregate = UserAggregate.builder()
                .id(new UserId(1L))
                .username("builderuser")
                .password(Password.fromRaw("Abc123"))
                .email(Email.of("builder@test.com"))
                .phone(Phone.of("13800138000"))
                .nickname(Nickname.of("BuilderUser"))
                .userType(UserType.ADMIN.getCode())
                .status(UserStatus.NORMAL.getCode())
                .loginType(AccountType.WEB.getCode())
                .sex("M")
                .realName("Real Name")
                .studentId("S12345")
                .loginIp("127.0.0.1")
                .remark("Test remark")
                .build();

            assertThat(aggregate.getId().value()).isEqualTo(1L);
            assertThat(aggregate.getUsername()).isEqualTo("builderuser");
            assertThat(aggregate.getEmail().value()).isEqualTo("builder@test.com");
            assertThat(aggregate.getPhone().value()).isEqualTo("13800138000");
            assertThat(aggregate.getNickname().value()).isEqualTo("BuilderUser");
            assertThat(aggregate.getUserType()).isEqualTo(UserType.ADMIN.getCode());
            assertThat(aggregate.getStatus()).isEqualTo(UserStatus.NORMAL.getCode());
            assertThat(aggregate.getLoginType()).isEqualTo(AccountType.WEB.getCode());
            assertThat(aggregate.getSex()).isEqualTo("M");
            assertThat(aggregate.getRealName()).isEqualTo("Real Name");
            assertThat(aggregate.getStudentId()).isEqualTo("S12345");
            assertThat(aggregate.getLoginIp()).isEqualTo("127.0.0.1");
            assertThat(aggregate.getRemark()).isEqualTo("Test remark");
        }

        @Test
        @DisplayName("Builder 使用默认状态值")
        void builder_defaultValues_usesDefaults() {
            UserAggregate aggregate = UserAggregate.builder()
                .id(new UserId(1L))
                .username("testuser")
                .password(Password.fromRaw("Abc123"))
                .build();

            assertThat(aggregate.getUserType()).isEqualTo(UserType.NORMAL.getCode());
            assertThat(aggregate.getStatus()).isEqualTo(UserStatus.NORMAL.getCode());
            assertThat(aggregate.getLoginType()).isEqualTo(AccountType.WEB.getCode());
        }
    }

    @Nested
    @DisplayName("getDomainEvents - 领域事件获取")
    class DomainEventsTests {

        @Test
        @DisplayName("getDomainEvents 返回事件列表的副本")
        void getDomainEvents_returnsCopy() {
            UserAggregate aggregate = createTestAggregate();

            var events1 = aggregate.getDomainEvents();
            var events2 = aggregate.getDomainEvents();

            assertThat(events1).isNotSameAs(events2);
            assertThat(events1).isEqualTo(events2);
        }
    }

    private UserAggregate createTestAggregate() {
        return UserAggregate.builder()
            .id(new UserId(1L))
            .username("testuser")
            .password(Password.fromRaw("Abc123"))
            .email(Email.of("test@test.com"))
            .phone(Phone.of("13800138000"))
            .nickname(Nickname.of("TestUser"))
            .userType(UserType.NORMAL.getCode())
            .status(UserStatus.NORMAL.getCode())
            .loginType(AccountType.WEB.getCode())
            .build();
    }
}