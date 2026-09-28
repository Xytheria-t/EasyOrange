package com.cartethyia.easyorange.user.domain.valueobject;

import com.cartethyia.easyorange.common.exception.BusinessException;
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
        @Nullable String avatar) {
    public PersonalInfo {
        rejectBlank(realName, "realName");
        rejectBlank(nickName, "nickName");
        rejectBlank(avatar, "avatar");
    }

    /** 拒绝 blank 值；null 是允许的（表示字段未设置） */
    private static void rejectBlank(@Nullable String value, String fieldName) {
        if (value != null && value.isBlank()) {
            throw BusinessException.of(fieldName + " 不能为空白字符串");
        }
    }

    public static PersonalInfo empty() {
        return new PersonalInfo(null, null, null, null);
    }
}
