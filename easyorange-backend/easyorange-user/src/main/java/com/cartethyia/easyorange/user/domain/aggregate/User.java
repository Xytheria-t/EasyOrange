package com.cartethyia.easyorange.user.domain.aggregate;

import com.cartethyia.easyorange.user.domain.enums.Sex;
import com.cartethyia.easyorange.user.domain.enums.UserStatus;
import com.cartethyia.easyorange.user.domain.enums.UserType;
import com.cartethyia.easyorange.user.domain.valueobject.AuditInfo;
import com.cartethyia.easyorange.user.domain.valueobject.LoginInfo;
import com.cartethyia.easyorange.user.domain.valueobject.UserProfile;
import lombok.Builder;
import lombok.Getter;

import java.util.Objects;

@Getter
@Builder(toBuilder = true)
public class User {

    private final Long id;
    private final String username;
    private final String password;
    private final UserType userType;
    private final String studentId;
    private final UserStatus status;
    private final UserProfile profile;
    private final LoginInfo loginInfo;
    private final AuditInfo auditInfo;

    public static User register(String username, String encodedPassword, String nickName) {
        Objects.requireNonNull(username, "username must not be null");
        Objects.requireNonNull(encodedPassword, "password must not be null");

        return User.builder()
            .username(username)
            .password(encodedPassword)
            .userType(UserType.NORMAL)
            .status(UserStatus.NORMAL)
            .profile(new UserProfile(null, null, null, nickName, null, null, null))
            .loginInfo(LoginInfo.initial())
            .build();
    }

    public User assignId(Long id) {
        Objects.requireNonNull(id, "id must not be null");

        return this.toBuilder()
            .id(id)
            .build();
    }

    public User updateProfile(String email, String phone, Sex sex, String realName, String nickname, String studentId, Long operatorId) {
        UserProfile newProfile = this.profile;

        if (email != null && !email.isBlank()) {
            newProfile = newProfile.updateEmail(email);
        }
        if (phone != null && !phone.isBlank()) {
            newProfile = newProfile.updatePhone(phone);
        }
        if (sex != null) {
            newProfile = newProfile.updateSex(sex);
        }
        if (realName != null && !realName.isBlank()) {
            newProfile = newProfile.updateRealName(realName);
        }
        if (nickname != null && !nickname.isBlank()) {
            newProfile = newProfile.updateNickName(nickname);
        }

        User.UserBuilder builder = this.toBuilder()
            .profile(newProfile)
            .auditInfo(updateAuditInfo(operatorId));

        if (studentId != null && !studentId.isBlank()) {
            builder.studentId(studentId);
        }

        return builder.build();
    }

    public User changeAvatar(String avatarUrl, Long operatorId) {
        Objects.requireNonNull(avatarUrl, "avatarUrl must not be null");

        return this.toBuilder()
            .profile(this.profile.updateAvatar(avatarUrl))
            .auditInfo(updateAuditInfo(operatorId))
            .build();
    }

    public User changePassword(String encodedNewPassword, Long operatorId) {
        Objects.requireNonNull(encodedNewPassword, "password must not be null");

        return this.toBuilder()
            .password(encodedNewPassword)
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
