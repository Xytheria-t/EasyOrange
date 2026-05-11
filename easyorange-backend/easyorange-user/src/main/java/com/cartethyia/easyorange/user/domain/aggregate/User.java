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

    private final Long id;
    private final Credentials credentials;
    private final UserType userType;
    private final UserStatus status;
    private final ContactInfo contactInfo;
    private final PersonalInfo personalInfo;
    private final LoginInfo loginInfo;
    private final AuditInfo auditInfo;

    public String getUsername() {
        return credentials != null ? credentials.username() : null;
    }

    public String getPassword() {
        return credentials != null ? credentials.encodedPassword() : null;
    }

    public static User register(String username, String encodedPassword, String nickName) {
        Objects.requireNonNull(username, "username must not be null");
        Objects.requireNonNull(encodedPassword, "password must not be null");

        return User.builder()
            .credentials(new Credentials(username, encodedPassword))
            .userType(UserType.NORMAL)
            .status(UserStatus.NORMAL)
            .contactInfo(ContactInfo.empty())
            .personalInfo(new PersonalInfo(null, nickName, null, null, null))
            .loginInfo(LoginInfo.initial())
            .build();
    }

    public User assignId(Long id) {
        Objects.requireNonNull(id, "id must not be null");

        return this.toBuilder()
            .id(id)
            .build();
    }

    public User updateContactInfo(String email, String phone, Long operatorId) {
        ContactInfo updated = this.contactInfo;

        if (email != null && !email.isBlank()) {
            updated = updated.withEmail(email);
        }
        if (phone != null && !phone.isBlank()) {
            updated = updated.withPhone(phone);
        }

        return this.toBuilder()
            .contactInfo(updated)
            .auditInfo(updateAuditInfo(operatorId))
            .build();
    }

    public User updatePersonalInfo(String realName, String nickName, Sex sex, String studentId, Long operatorId) {
        PersonalInfo updated = this.personalInfo;

        if (realName != null && !realName.isBlank()) {
            updated = updated.withRealName(realName);
        }
        if (nickName != null && !nickName.isBlank()) {
            updated = updated.withNickName(nickName);
        }
        if (sex != null) {
            updated = updated.withSex(sex);
        }
        if (studentId != null && !studentId.isBlank()) {
            updated = updated.withStudentId(studentId);
        }

        return this.toBuilder()
            .personalInfo(updated)
            .auditInfo(updateAuditInfo(operatorId))
            .build();
    }

    public User changeAvatar(String avatarUrl, Long operatorId) {
        Objects.requireNonNull(avatarUrl, "avatarUrl must not be null");

        return this.toBuilder()
            .personalInfo(this.personalInfo.withAvatar(avatarUrl))
            .auditInfo(updateAuditInfo(operatorId))
            .build();
    }

    public User changePassword(String encodedNewPassword, Long operatorId) {
        Objects.requireNonNull(encodedNewPassword, "password must not be null");

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

    public boolean isNormal() {
        return this.status == UserStatus.NORMAL;
    }

    private AuditInfo updateAuditInfo(Long operatorId) {
        if (this.auditInfo == null) {
            return operatorId != null ? AuditInfo.create(operatorId) : null;
        }
        return operatorId != null ? this.auditInfo.update(operatorId) : this.auditInfo;
    }
}
