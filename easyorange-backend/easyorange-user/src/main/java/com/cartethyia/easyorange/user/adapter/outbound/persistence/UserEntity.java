package com.cartethyia.easyorange.user.adapter.outbound.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cartethyia.easyorange.common.entity.BaseDO;
import com.cartethyia.easyorange.user.domain.aggregate.User;
import com.cartethyia.easyorange.user.domain.shared.enums.Sex;
import com.cartethyia.easyorange.user.domain.shared.enums.UserStatus;
import com.cartethyia.easyorange.user.domain.shared.enums.UserType;
import com.cartethyia.easyorange.user.domain.valueobject.AuditInfo;
import com.cartethyia.easyorange.user.domain.valueobject.LoginInfo;
import com.cartethyia.easyorange.user.domain.valueobject.UserProfile;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.function.Function;

@TableName("eo_user")
@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UserEntity extends BaseDO {

    @TableId(value = "user_id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("username")
    private String username;

    @JsonIgnore
    private String password;

    @TableField("user_type")
    private UserType userType;

    private String email;

    @TableField("phonenumber")
    private String phone;

    @TableField("student_id")
    private String studentId;

    @TableField("real_name")
    private String realName;

    @TableField("nick_name")
    private String nickName;

    private Sex sex;

    private UserStatus status;

    @TableField("login_ip")
    private String loginIp;

    @TableField("login_date")
    private LocalDateTime loginDate;

    @TableField("pwd_update_date")
    private LocalDateTime pwdUpdateDate;

    @TableField("avatar")
    private String avatar;

    private String remark;

    public User toDomain() {
        return User.builder()
            .id(id)
            .username(username)
            .password(password)
            .userType(userType)
            .studentId(studentId)
            .status(status)
            .profile(new UserProfile(email, phone, realName, nickName, sex, avatar, remark))
            .loginInfo(new LoginInfo(loginIp, loginDate, pwdUpdateDate))
            .auditInfo(new AuditInfo(getCreateTime(), getUpdateTime(), getCreateBy(), getUpdateBy(), getDelFlag(), getVersion()))
            .build();
    }

    public static UserEntity from(User user) {
        return UserEntity.builder()
            .id(user.getId())
            .username(user.getUsername())
            .password(user.getPassword())
            .userType(user.getUserType())
            .studentId(user.getStudentId())
            .status(user.getStatus())
            .email(profileField(user, UserProfile::email))
            .phone(profileField(user, UserProfile::phone))
            .realName(profileField(user, UserProfile::realName))
            .nickName(profileField(user, UserProfile::nickName))
            .sex(profileField(user, UserProfile::sex))
            .avatar(profileField(user, UserProfile::avatar))
            .remark(profileField(user, UserProfile::remark))
            .loginIp(loginField(user, LoginInfo::loginIp))
            .loginDate(loginField(user, LoginInfo::loginDate))
            .pwdUpdateDate(loginField(user, LoginInfo::pwdUpdateDate))
            .createTime(auditField(user, AuditInfo::createTime))
            .updateTime(auditField(user, AuditInfo::updateTime))
            .createBy(auditField(user, AuditInfo::createBy))
            .updateBy(auditField(user, AuditInfo::updateBy))
            .delFlag(auditField(user, AuditInfo::delFlag))
            .version(auditField(user, AuditInfo::version))
            .build();
    }

    private static <T> T profileField(User user, Function<UserProfile, T> extractor) {
        return user.getProfile() != null ? extractor.apply(user.getProfile()) : null;
    }

    private static <T> T loginField(User user, Function<LoginInfo, T> extractor) {
        return user.getLoginInfo() != null ? extractor.apply(user.getLoginInfo()) : null;
    }

    private static <T> T auditField(User user, Function<AuditInfo, T> extractor) {
        return user.getAuditInfo() != null ? extractor.apply(user.getAuditInfo()) : null;
    }
}
