package com.cartethyia.easyorange.user.domain.aggregate;

import com.cartethyia.easyorange.user.domain.enums.Sex;
import com.cartethyia.easyorange.user.domain.enums.UserStatus;
import com.cartethyia.easyorange.user.domain.enums.UserType;
import com.cartethyia.easyorange.user.domain.valueobject.AuditInfo;
import com.cartethyia.easyorange.user.domain.valueobject.ContactInfo;
import com.cartethyia.easyorange.user.domain.valueobject.Credentials;
import com.cartethyia.easyorange.user.domain.valueobject.LoginInfo;
import com.cartethyia.easyorange.user.domain.valueobject.PersonalInfo;
import lombok.Builder;
import lombok.Getter;

import java.util.Objects;

@Getter
@Builder(toBuilder = true)
public class User {

    private final String id;
    private final Credentials credentials;
    private final UserType userType;
    private final UserStatus status;
    private final ContactInfo contactInfo;
    private final PersonalInfo personalInfo;
    private final LoginInfo loginInfo;
    private final AuditInfo auditInfo;

    public static User create(String username, String encodedPassword) {
        Objects.requireNonNull(username, "用户名不能为空");
        Objects.requireNonNull(encodedPassword, "密码不能为空");

        return User.builder()
            .credentials(new Credentials(username, encodedPassword))
            .userType(UserType.NORMAL)
            .status(UserStatus.NORMAL)
            .contactInfo(ContactInfo.empty())
            .personalInfo(PersonalInfo.builder().nickName(username).build())
            .loginInfo(LoginInfo.empty())
            .build();
    }

    public User assignId(String id) {
        Objects.requireNonNull(id, "用户ID不能为空");

        return this.toBuilder()
            .id(id)
            .build();
    }

    public User updateContactInfo(ContactUpdateSpec spec, String operatorId) {
        ContactInfo updated = this.contactInfo;

        if (isPresent(spec.email())) {
            updated = updated.withEmail(spec.email());
        }
        if (isPresent(spec.phone())) {
            updated = updated.withPhone(spec.phone());
        }

        return this.toBuilder()
            .contactInfo(updated)
            .auditInfo(updateAuditInfo(operatorId))
            .build();
    }

    public User updatePersonalInfo(PersonalUpdateSpec spec, String operatorId) {
        PersonalInfo updated = this.personalInfo;

        if (isPresent(spec.realName())) {
            updated = updated.withRealName(spec.realName());
        }
        if (isPresent(spec.nickName())) {
            updated = updated.withNickName(spec.nickName());
        }
        if (spec.sex() != null) {
            updated = updated.withSex(spec.sex());
        }
        if (isPresent(spec.studentId())) {
            updated = updated.withStudentId(spec.studentId());
        }

        return this.toBuilder()
            .personalInfo(updated)
            .auditInfo(updateAuditInfo(operatorId))
            .build();
    }

    public User changeAvatar(String avatarUrl, String operatorId) {
        Objects.requireNonNull(avatarUrl, "头像地址不能为空");

        return this.toBuilder()
            .personalInfo(this.personalInfo.withAvatar(avatarUrl))
            .auditInfo(updateAuditInfo(operatorId))
            .build();
    }

    public User changePassword(String encodedNewPassword, String operatorId) {
        Objects.requireNonNull(encodedNewPassword, "新密码不能为空");

        return this.toBuilder()
            .credentials(this.credentials.changePassword(encodedNewPassword))
            .loginInfo(this.loginInfo.updatePasswordTime())
            .auditInfo(updateAuditInfo(operatorId))
            .build();
    }

    public User recordLogin(String loginIp) {
        return this.toBuilder()
            .loginInfo(this.loginInfo.recordLogin(loginIp))
            .build();
    }

    /**
     * 返回用户名。
     *
     * @throws IllegalStateException if the User was constructed without credentials.
     *                               Use {@link #create(String, String)} factory method instead of the builder directly.
     */
    public String getUsername() {
        if (credentials == null) {
            throw new IllegalStateException("User was constructed without credentials - use User.create() factory method");
        }
        return credentials.username();
    }

    /**
     * 返回编码后的密码。
     *
     * @throws IllegalStateException if the User was constructed without credentials.
     *                               Use {@link #create(String, String)} factory method instead of the builder directly.
     */
    public String getPassword() {
        if (credentials == null) {
            throw new IllegalStateException("User was constructed without credentials - use User.create() factory method");
        }
        return credentials.encodedPassword();
    }

    public boolean isEnabled() {
        return this.status == UserStatus.NORMAL;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "User{id=" + id + ", userType=" + userType + ", status=" + status + "}";
    }

    private AuditInfo updateAuditInfo(String operatorId) {
        if (this.auditInfo == null) {
            return operatorId != null ? AuditInfo.create(operatorId) : null;
        }
        return operatorId != null ? this.auditInfo.update(operatorId) : this.auditInfo;
    }

    private static boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }
}