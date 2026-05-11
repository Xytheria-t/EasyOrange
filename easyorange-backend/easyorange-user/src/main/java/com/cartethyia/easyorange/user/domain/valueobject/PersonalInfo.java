package com.cartethyia.easyorange.user.domain.valueobject;

import com.cartethyia.easyorange.user.domain.enums.Sex;

public record PersonalInfo(String realName, String nickName, Sex sex, String studentId, String avatar) {
    public PersonalInfo {
        if (realName != null && realName.isBlank()) {
            throw new IllegalArgumentException("realName must not be blank");
        }
        if (nickName != null && nickName.isBlank()) {
            throw new IllegalArgumentException("nickName must not be blank");
        }
        if (studentId != null && studentId.isBlank()) {
            throw new IllegalArgumentException("studentId must not be blank");
        }
        if (avatar != null && avatar.isBlank()) {
            throw new IllegalArgumentException("avatar must not be blank");
        }
    }

    public static PersonalInfo empty() {
        return new PersonalInfo(null, null, null, null, null);
    }

    public PersonalInfo withRealName(String newRealName) {
        return new PersonalInfo(newRealName, nickName, sex, studentId, avatar);
    }

    public PersonalInfo withNickName(String newNickName) {
        return new PersonalInfo(realName, newNickName, sex, studentId, avatar);
    }

    public PersonalInfo withSex(Sex newSex) {
        return new PersonalInfo(realName, nickName, newSex, studentId, avatar);
    }

    public PersonalInfo withStudentId(String newStudentId) {
        return new PersonalInfo(realName, nickName, sex, newStudentId, avatar);
    }

    public PersonalInfo withAvatar(String newAvatar) {
        return new PersonalInfo(realName, nickName, sex, studentId, newAvatar);
    }
}
