package com.cartethyia.easyorange.user.domain.aggregate;

import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.user.domain.event.PasswordChangedEvent;
import com.cartethyia.easyorange.user.domain.event.UserRegisteredEvent;
import com.cartethyia.easyorange.user.domain.valueobject.Email;
import com.cartethyia.easyorange.user.domain.valueobject.Nickname;
import com.cartethyia.easyorange.user.domain.valueobject.Password;
import com.cartethyia.easyorange.user.domain.valueobject.Phone;
import com.cartethyia.easyorange.user.domain.valueobject.UserId;
import com.cartethyia.easyorange.user.entity.User;
import com.cartethyia.easyorange.user.enums.AccountType;
import com.cartethyia.easyorange.user.enums.UserResultCode;
import com.cartethyia.easyorange.user.enums.UserStatus;
import com.cartethyia.easyorange.user.enums.UserType;
import com.cartethyia.easyorange.common.exception.BusinessException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class UserAggregate {

    private final UserId id;
    private final String username;
    private Password password;
    private Email email;
    private Phone phone;
    private Nickname nickname;
    private String userType;
    private String status;
    private String loginType;
    private String sex;
    private String realName;
    private String studentId;
    private String loginIp;
    private LocalDateTime loginDate;
    private LocalDateTime pwdUpdateDate;
    private String remark;

    private final List<Object> domainEvents = new ArrayList<>();

    private UserAggregate(Builder builder) {
        this.id = builder.id;
        this.username = builder.username;
        this.password = builder.password;
        this.email = builder.email;
        this.phone = builder.phone;
        this.nickname = builder.nickname;
        this.userType = builder.userType;
        this.status = builder.status;
        this.loginType = builder.loginType;
        this.sex = builder.sex;
        this.realName = builder.realName;
        this.studentId = builder.studentId;
        this.loginIp = builder.loginIp;
        this.loginDate = builder.loginDate;
        this.pwdUpdateDate = builder.pwdUpdateDate;
        this.remark = builder.remark;
    }

    public static UserRegisteredEvent register(String username, Password password, String loginType) {
        BizRequire.notBlank(username, "用户名不能为空");
        BizRequire.notNull(password, "密码不能为空");

        UserId userId = new UserId(generateUserId());

        return new UserRegisteredEvent(userId, username);
    }

    public PasswordChangedEvent changePassword(Password newPassword) {
        this.password = newPassword;
        this.pwdUpdateDate = LocalDateTime.now();
        return new PasswordChangedEvent(this.id);
    }

    public void updateProfile(Email email, Phone phone, Nickname nickname) {
        if (email != null) {
            this.email = email;
        }
        if (phone != null) {
            this.phone = phone;
        }
        if (nickname != null) {
            this.nickname = nickname;
        }
    }

    public void updateLoginInfo(String loginIp) {
        this.loginIp = loginIp;
        this.loginDate = LocalDateTime.now();
    }

    public void disable() {
        this.status = UserStatus.DISABLED.getCode();
    }

    public void enable() {
        this.status = UserStatus.NORMAL.getCode();
    }

    public void lock() {
        this.status = UserStatus.LOCKED.getCode();
    }

    public UserId getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public Password getPassword() {
        return password;
    }

    public Email getEmail() {
        return email;
    }

    public Phone getPhone() {
        return phone;
    }

    public Nickname getNickname() {
        return nickname;
    }

    public String getUserType() {
        return userType;
    }

    public String getStatus() {
        return status;
    }

    public String getLoginType() {
        return loginType;
    }

    public String getSex() {
        return sex;
    }

    public String getRealName() {
        return realName;
    }

    public String getStudentId() {
        return studentId;
    }

    public String getLoginIp() {
        return loginIp;
    }

    public LocalDateTime getLoginDate() {
        return loginDate;
    }

    public LocalDateTime getPwdUpdateDate() {
        return pwdUpdateDate;
    }

    public String getRemark() {
        return remark;
    }

    public List<Object> getDomainEvents() {
        return new ArrayList<>(domainEvents);
    }

    public void clearDomainEvents() {
        domainEvents.clear();
    }

    private void registerEvent(Object event) {
        domainEvents.add(event);
    }

    private static Long generateUserId() {
        return System.currentTimeMillis();
    }

    public UserAggregate withId(Long id) {
        return new Builder()
                .id(new UserId(id))
                .username(this.username)
                .password(this.password)
                .email(this.email)
                .phone(this.phone)
                .nickname(this.nickname)
                .userType(this.userType)
                .status(this.status)
                .loginType(this.loginType)
                .sex(this.sex)
                .realName(this.realName)
                .studentId(this.studentId)
                .loginIp(this.loginIp)
                .loginDate(this.loginDate)
                .pwdUpdateDate(this.pwdUpdateDate)
                .remark(this.remark)
                .build();
    }

    public static UserAggregate from(UserId userId, String username, Password password, Email email,
                                     Phone phone, Nickname nickname, String userType, String status,
                                     String loginType) {
        return new Builder()
                .id(userId)
                .username(username)
                .password(password)
                .email(email)
                .phone(phone)
                .nickname(nickname)
                .userType(userType)
                .status(status)
                .loginType(loginType)
                .build();
    }

    public static UserAggregate fromEntity(User user) {
        if (user == null) {
            return null;
        }
        Builder builder = new Builder()
                .id(new UserId(user.getId()))
                .username(user.getUsername())
                .password(Password.fromEncoded(user.getPassword()));

        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            try {
                builder.email(new Email(user.getEmail()));
            } catch (BusinessException ignored) {
            }
        }
        if (user.getPhone() != null && !user.getPhone().isBlank()) {
            try {
                builder.phone(new Phone(user.getPhone()));
            } catch (BusinessException ignored) {
            }
        }
        if (user.getNickName() != null && !user.getNickName().isBlank()) {
            try {
                builder.nickname(new Nickname(user.getNickName()));
            } catch (BusinessException ignored) {
            }
        }

        return builder
                .userType(user.getUserType())
                .status(user.getStatus())
                .loginType(user.getLoginType())
                .sex(user.getSex())
                .realName(user.getRealName())
                .studentId(user.getStudentId())
                .loginIp(user.getLoginIp())
                .loginDate(user.getLoginDate())
                .pwdUpdateDate(user.getPwdUpdateDate())
                .remark(user.getRemark())
                .build();
    }

    public User toEntity() {
        return User.builder()
                .id(this.id != null ? this.id.value() : null)
                .username(this.username)
                .password(this.password != null ? this.password.getEncodedValue() : null)
                .email(this.email != null ? this.email.value() : null)
                .phone(this.phone != null ? this.phone.value() : null)
                .nickName(this.nickname != null ? this.nickname.value() : null)
                .userType(this.userType)
                .status(this.status)
                .loginType(this.loginType)
                .sex(this.sex)
                .realName(this.realName)
                .studentId(this.studentId)
                .loginIp(this.loginIp)
                .loginDate(this.loginDate)
                .pwdUpdateDate(this.pwdUpdateDate)
                .remark(this.remark)
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UserId id;
        private String username;
        private Password password;
        private Email email;
        private Phone phone;
        private Nickname nickname;
        private String userType = UserType.NORMAL.getCode();
        private String status = UserStatus.NORMAL.getCode();
        private String loginType = AccountType.WEB.getCode();
        private String sex;
        private String realName;
        private String studentId;
        private String loginIp;
        private LocalDateTime loginDate;
        private LocalDateTime pwdUpdateDate;
        private String remark;

        public Builder id(UserId id) {
            this.id = id;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder password(Password password) {
            this.password = password;
            return this;
        }

        public Builder email(Email email) {
            this.email = email;
            return this;
        }

        public Builder phone(Phone phone) {
            this.phone = phone;
            return this;
        }

        public Builder nickname(Nickname nickname) {
            this.nickname = nickname;
            return this;
        }

        public Builder userType(String userType) {
            this.userType = userType;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder loginType(String loginType) {
            this.loginType = loginType;
            return this;
        }

        public Builder sex(String sex) {
            this.sex = sex;
            return this;
        }

        public Builder realName(String realName) {
            this.realName = realName;
            return this;
        }

        public Builder studentId(String studentId) {
            this.studentId = studentId;
            return this;
        }

        public Builder loginIp(String loginIp) {
            this.loginIp = loginIp;
            return this;
        }

        public Builder loginDate(LocalDateTime loginDate) {
            this.loginDate = loginDate;
            return this;
        }

        public Builder pwdUpdateDate(LocalDateTime pwdUpdateDate) {
            this.pwdUpdateDate = pwdUpdateDate;
            return this;
        }

        public Builder remark(String remark) {
            this.remark = remark;
            return this;
        }

        public UserAggregate build() {
            return new UserAggregate(this);
        }
    }
}