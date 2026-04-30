package com.cartethyia.easyorange.user.domain.model;

import com.cartethyia.easyorange.user.common.enums.Sex;
import com.cartethyia.easyorange.user.common.enums.UserStatus;
import com.cartethyia.easyorange.user.common.enums.UserType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    private Long id;
    private String username;
    private String password;
    private UserType userType;
    private String email;
    private String phone;
    private String studentId;
    private String realName;
    private String nickName;
    private Sex sex;
    private UserStatus status;
    private String loginIp;
    private LocalDateTime loginDate;
    private LocalDateTime pwdUpdateDate;
    private String avatar;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Long createBy;
    private Long updateBy;
    private Integer delFlag;
    private Integer version;

    public static User register(String username, String encodedPassword, String nickName) {
        return User.builder()
            .username(username)
            .password(encodedPassword)
            .nickName(nickName)
            .userType(UserType.NORMAL)
            .status(UserStatus.NORMAL)
            .build();
    }

    public void updateInfo(String email, String phone, Sex sex) {
        if (email != null && !email.isBlank()) {
            this.email = email;
        }
        if (phone != null && !phone.isBlank()) {
            this.phone = phone;
        }
        if (sex != null) {
            this.sex = sex;
        }
        this.updateTime = LocalDateTime.now();
    }

    public void changeAvatar(String avatarUrl) {
        this.avatar = avatarUrl;
        this.updateTime = LocalDateTime.now();
    }

    public void changePassword(String encodedNewPassword) {
        this.password = encodedNewPassword;
        this.pwdUpdateDate = LocalDateTime.now();
    }

    public void updateLoginInfo(String loginIp) {
        this.loginIp = loginIp;
        this.loginDate = LocalDateTime.now();
    }

    public boolean isNormal() {
        return this.status == UserStatus.NORMAL;
    }
}
