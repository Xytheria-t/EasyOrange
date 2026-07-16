package com.cartethyia.easyorange.user.domain.valueobject;

import com.cartethyia.easyorange.user.domain.enums.Sex;
import lombok.Builder;
import lombok.With;
import org.jetbrains.annotations.Nullable;

@With
@Builder(toBuilder = true)
public record PersonalInfo(
    @Nullable String realName,
    @Nullable String nickName,
    @Nullable Sex sex,
    @Nullable String studentId,
    @Nullable String avatar
) {
    public PersonalInfo {
        requireNonBlank(realName, "realName");
        requireNonBlank(nickName, "nickName");
        requireNonBlank(studentId, "studentId");
        requireNonBlank(avatar, "avatar");
    }

    private static void requireNonBlank(@Nullable String value, String fieldName) {
        if (value != null && value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }

    public static PersonalInfo empty() {
        return new PersonalInfo(null, null, null, null, null);
    }
}
