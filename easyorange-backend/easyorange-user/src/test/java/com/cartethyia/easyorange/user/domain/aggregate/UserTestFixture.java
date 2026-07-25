package com.cartethyia.easyorange.user.domain.aggregate;

import com.cartethyia.easyorange.user.domain.enums.Sex;
import com.cartethyia.easyorange.user.domain.enums.UserStatus;
import com.cartethyia.easyorange.user.domain.enums.UserType;
import com.cartethyia.easyorange.user.domain.valueobject.ContactInfo;
import com.cartethyia.easyorange.user.domain.valueobject.Credentials;
import com.cartethyia.easyorange.user.domain.valueobject.LoginInfo;
import com.cartethyia.easyorange.user.domain.valueobject.PersonalInfo;

/**
 * User 测试夹具 — Test Data Builder 模式 + 快捷工厂方法。
 * <p>
 * 推荐用法：
 * <pre>{@code
 * normalUser();                                          // 完整档案的普通用户
 * disabledUser();                                        // 已禁用用户
 * minimalUser();                                         // 仅 id+credentials+loginInfo
 * aUser().credentials(new Credentials("u", "p")).build(); // 覆盖单个字段
 * }</pre>
 */
public final class UserTestFixture {

    public static final String USER_ID = "1";
    public static final String USERNAME = "testuser";
    public static final String PHONE = "13812345678";
    public static final String EMAIL = "test@example.com";
    public static final String ENCODED_PW = "$2a$10$encoded";

    private UserTestFixture() {}

    // ==================== Fluent Builder ====================

    public static User.UserBuilder aUser() {
        return User.builder()
            .id(USER_ID)
            .credentials(new Credentials(USERNAME, ENCODED_PW))
            .userType(UserType.NORMAL)
            .status(UserStatus.NORMAL)
            .contactInfo(new ContactInfo(EMAIL, PHONE))
            .personalInfo(PersonalInfo.builder()
                .realName("张三")
                .nickName("小张")
                .sex(Sex.MALE)
                .studentId("2024001")
                .avatar("/avatar/test.png")
                .build())
            .loginInfo(LoginInfo.empty());
    }

    // ==================== Convenience Factories ====================

    /** 完整档案的普通用户 — 适用于 service/repository 测试。 */
    public static User normalUser() {
        return aUser().build();
    }

    /** 已禁用用户 — 适用于认证失败场景测试。 */
    public static User disabledUser() {
        return aUser().status(UserStatus.DISABLED).build();
    }

    /** 仅含 id+credentials+loginInfo 的最小用户 — 适用于不关心 profile 的 domain/service 测试。 */
    public static User minimalUser() {
        return User.builder()
            .id(USER_ID)
            .credentials(new Credentials(USERNAME, ENCODED_PW))
            .userType(UserType.NORMAL)
            .status(UserStatus.NORMAL)
            .loginInfo(LoginInfo.empty())
            .build();
    }

    /** 指定凭证的最小用户 — 适用于需要特定用户名/密码的测试。 */
    public static User userWithCredentials(String username, String encodedPassword) {
        return User.builder()
            .id(USER_ID)
            .credentials(new Credentials(username, encodedPassword))
            .userType(UserType.NORMAL)
            .status(UserStatus.NORMAL)
            .loginInfo(LoginInfo.empty())
            .build();
    }
}
