package com.cartethyia.easyorange.user.domain.aggregate;

import com.cartethyia.easyorange.user.domain.enums.UserStatus;
import com.cartethyia.easyorange.user.domain.enums.UserType;
import com.cartethyia.easyorange.user.domain.valueobject.AuditInfo;
import com.cartethyia.easyorange.user.domain.valueobject.Avatar;
import com.cartethyia.easyorange.user.domain.valueobject.ContactInfo;
import com.cartethyia.easyorange.user.domain.valueobject.Credentials;
import com.cartethyia.easyorange.user.domain.valueobject.LoginInfo;
import com.cartethyia.easyorange.user.domain.valueobject.PersonalInfo;
import java.util.Objects;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

/**
 * 用户聚合根 — 凭据、状态、联系方式、资料、登录痕迹的持有者。
 * <p>
 * 取舍：本类<b>不</b>对外承诺不可变。所有变更方法走 Lombok {@code toBuilder} 返回<b>新实例</b>
 * （含 {@link #assignId(String)}），实例本身不可变、字段全 final，但聚合不做「同一实例多方改写」的自制约束——
 * 变更即派生，共享引用不会被就地改脏。
 * <p>
 * 边界：密码只以密文形态进出（{@link #getPassword()} 直出 {@code Credentials.encodedPassword()}），
 * 明文编码在 {@code PasswordEncoderPort} 一侧完成；对外响应一律经
 * {@code application/dto/UserView} 脱敏，不把本对象直接序列化。审计信息按 operatorId 是否为 null 决定
 * 是否落库（null = 系统自身操作，不写 updateBy），软删由 delFlag 过滤，聚合不提供删除迁移。
 * <p>
 * {@code remark} 是<b>管理端</b>变更原因（停用 / 改角色 / 重置密码时前端必填的那句），对应
 * {@code eo_user.remark}。C 端用户自己改资料没有「原因」这个概念，故该字段只在
 * {@link #withChangeReason(String)} 这一条路径上被写入。
 */
@Getter
public class User {

    private final String id;
    private final Credentials credentials;
    private final UserType userType;
    private final UserStatus status;
    private final ContactInfo contactInfo;
    private final PersonalInfo personalInfo;
    private final LoginInfo loginInfo;
    private final AuditInfo auditInfo;
    private final String remark;

    @Builder(toBuilder = true)
    private User(
            String id,
            @NonNull Credentials credentials,
            UserType userType,
            UserStatus status,
            ContactInfo contactInfo,
            PersonalInfo personalInfo,
            LoginInfo loginInfo,
            AuditInfo auditInfo,
            String remark) {
        this.id = id;
        this.credentials = credentials;
        this.userType = userType;
        this.status = status;
        this.contactInfo = contactInfo;
        this.personalInfo = personalInfo;
        this.loginInfo = loginInfo;
        this.auditInfo = auditInfo;
        this.remark = remark;
    }

    public static User create(String username, String encodedPassword, String phone) {
        return User.builder()
                .credentials(new Credentials(username, encodedPassword))
                .userType(UserType.NORMAL)
                .status(UserStatus.NORMAL)
                .contactInfo(ContactInfo.empty().withPhone(phone))
                .personalInfo(PersonalInfo.builder().nickName(username).build())
                .loginInfo(LoginInfo.empty())
                .build();
    }

    public User assignId(String id) {
        Objects.requireNonNull(id, "用户ID不能为空");

        return this.toBuilder().id(id).build();
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

        return this.toBuilder()
                .personalInfo(updated)
                .auditInfo(updateAuditInfo(operatorId))
                .build();
    }

    public User changeAvatar(Avatar avatar, String operatorId) {
        Objects.requireNonNull(avatar, "头像不能为空");
        Objects.requireNonNull(avatar.url(), "头像地址不能为空");

        return this.toBuilder()
                .personalInfo(this.personalInfo.withAvatar(avatar.url()))
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

    public User changeStatus(UserStatus newStatus, String operatorId) {
        Objects.requireNonNull(newStatus, "用户状态不能为空");

        return this.toBuilder()
                .status(newStatus)
                .auditInfo(updateAuditInfo(operatorId))
                .build();
    }

    public User changeUserType(UserType newUserType, String operatorId) {
        Objects.requireNonNull(newUserType, "用户角色不能为空");

        return this.toBuilder()
                .userType(newUserType)
                .auditInfo(updateAuditInfo(operatorId))
                .build();
    }

    /**
     * 记录管理端变更原因 — 管理端口写路径专用，与 {@code operatorId} 一起构成「谁、为什么」，
     * 两者都落到同一行（{@code remark} + {@code auditInfo.updateBy}）。
     */
    public User withChangeReason(String reason) {
        return this.toBuilder().remark(reason).build();
    }

    public User recordLogin(String loginIp) {
        return this.toBuilder().loginInfo(this.loginInfo.recordLogin(loginIp)).build();
    }

    public String getUsername() {
        return credentials.username();
    }

    public String getPassword() {
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
