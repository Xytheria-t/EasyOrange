package com.cartethyia.easyorange.user.domain.valueobject;

import com.cartethyia.easyorange.user.domain.shared.enums.Sex;

public record UserProfile(
    String email,
    String phone,
    String realName,
    String nickName,
    Sex sex,
    String avatar,
    String remark
) {
    public static UserProfile empty() {
        return new UserProfile(null, null, null, null, null, null, null);
    }

    public UserProfile updateEmail(String newEmail) {
        return new UserProfile(newEmail, phone, realName, nickName, sex, avatar, remark);
    }

    public UserProfile updatePhone(String newPhone) {
        return new UserProfile(email, newPhone, realName, nickName, sex, avatar, remark);
    }

    public UserProfile updateNickName(String newNickName) {
        return new UserProfile(email, phone, realName, newNickName, sex, avatar, remark);
    }

    public UserProfile updateSex(Sex newSex) {
        return new UserProfile(email, phone, realName, nickName, newSex, avatar, remark);
    }

    public UserProfile updateAvatar(String newAvatar) {
        return new UserProfile(email, phone, realName, nickName, sex, newAvatar, remark);
    }

    public UserProfile updateRemark(String newRemark) {
        return new UserProfile(email, phone, realName, nickName, sex, avatar, newRemark);
    }

    public UserProfile updateRealName(String newRealName) {
        return new UserProfile(email, phone, newRealName, nickName, sex, avatar, remark);
    }
}
