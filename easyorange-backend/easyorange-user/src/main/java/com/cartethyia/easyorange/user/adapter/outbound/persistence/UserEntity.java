package com.cartethyia.easyorange.user.adapter.outbound.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cartethyia.easyorange.framework.entity.BaseDO;
import com.cartethyia.easyorange.user.domain.aggregate.User;
import com.cartethyia.easyorange.user.domain.enums.Sex;
import com.cartethyia.easyorange.user.domain.enums.UserStatus;
import com.cartethyia.easyorange.user.domain.enums.UserType;
import com.cartethyia.easyorange.user.domain.valueobject.AuditInfo;
import com.cartethyia.easyorange.user.domain.valueobject.ContactInfo;
import com.cartethyia.easyorange.user.domain.valueobject.Credentials;
import com.cartethyia.easyorange.user.domain.valueobject.LoginInfo;
import com.cartethyia.easyorange.user.domain.valueobject.PersonalInfo;
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
            .credentials(new Credentials(username, password))
            .userType(userType)
            .status(status)
            .contactInfo(new ContactInfo(email, phone))
            .personalInfo(new PersonalInfo(realName, nickName, sex, studentId, avatar))
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
            .status(user.getStatus())
            .email(contactField(user, ContactInfo::email))
            .phone(contactField(user, ContactInfo::phone))
            .realName(personalField(user, PersonalInfo::realName))
            .nickName(personalField(user, PersonalInfo::nickName))
            .sex(personalField(user, PersonalInfo::sex))
            .studentId(personalField(user, PersonalInfo::studentId))
            .avatar(personalField(user, PersonalInfo::avatar))
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

    private static <T> T contactField(User user, Function<ContactInfo, T> extractor) {
        return user.getContactInfo() != null ? extractor.apply(user.getContactInfo()) : null;
    }

    private static <T> T personalField(User user, Function<PersonalInfo, T> extractor) {
        return user.getPersonalInfo() != null ? extractor.apply(user.getPersonalInfo()) : null;
    }

    private static <T> T loginField(User user, Function<LoginInfo, T> extractor) {
        return user.getLoginInfo() != null ? extractor.apply(user.getLoginInfo()) : null;
    }

    private static <T> T auditField(User user, Function<AuditInfo, T> extractor) {
        return user.getAuditInfo() != null ? extractor.apply(user.getAuditInfo()) : null;
    }
}
