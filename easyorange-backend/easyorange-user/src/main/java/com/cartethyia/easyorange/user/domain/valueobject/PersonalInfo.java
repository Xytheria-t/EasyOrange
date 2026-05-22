package com.cartethyia.easyorange.user.domain.valueobject;

import com.cartethyia.easyorange.user.domain.enums.Sex;
import org.immutables.value.Value;
import org.jetbrains.annotations.Nullable;

@Value.Immutable
public abstract class PersonalInfo {
    @Nullable
    public abstract String realName();

    @Nullable
    public abstract String nickName();

    @Nullable
    public abstract Sex sex();

    @Nullable
    public abstract String studentId();

    @Nullable
    public abstract String avatar();

    public PersonalInfo withRealName(String newRealName) {
        return ImmutablePersonalInfo.builder().from(this).realName(newRealName).build();
    }

    public PersonalInfo withNickName(String newNickName) {
        return ImmutablePersonalInfo.builder().from(this).nickName(newNickName).build();
    }

    public PersonalInfo withSex(Sex newSex) {
        return ImmutablePersonalInfo.builder().from(this).sex(newSex).build();
    }

    public PersonalInfo withStudentId(String newStudentId) {
        return ImmutablePersonalInfo.builder().from(this).studentId(newStudentId).build();
    }

    public PersonalInfo withAvatar(String newAvatar) {
        return ImmutablePersonalInfo.builder().from(this).avatar(newAvatar).build();
    }

    @Value.Check
    protected PersonalInfo validate() {
        if (realName() != null && realName().isBlank()) {
            throw new IllegalArgumentException("realName must not be blank");
        }
        if (nickName() != null && nickName().isBlank()) {
            throw new IllegalArgumentException("nickName must not be blank");
        }
        if (studentId() != null && studentId().isBlank()) {
            throw new IllegalArgumentException("studentId must not be blank");
        }
        if (avatar() != null && avatar().isBlank()) {
            throw new IllegalArgumentException("avatar must not be blank");
        }
        return this;
    }

    public static PersonalInfo empty() {
        return ImmutablePersonalInfo.builder().build();
    }
}
