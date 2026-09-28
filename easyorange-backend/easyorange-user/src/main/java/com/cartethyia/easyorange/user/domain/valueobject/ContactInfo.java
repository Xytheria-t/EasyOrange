package com.cartethyia.easyorange.user.domain.valueobject;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.user.domain.constant.UserConstant;
import com.cartethyia.easyorange.user.domain.enums.UserResultCode;

/**
 * 联系方式值对象 — 邮箱与手机号的格式守卫。
 * <p>
 * 取舍：手机号规则引 {@link UserConstant#PHONE_REGEX}（5 个 Request DTO 与 {@code AuthController}
 * 的入参校验同一来源），不另抄一份；邮箱格式仅本类使用，保留在此。
 * <p>
 * 边界：null / blank 一律放行（表示未填写），只有非空且不合规才拒。
 * 抛 {@link BusinessException} 而非 {@code IllegalArgumentException}——后者会穿透到全局异常处理
 * 变成 500，脏数据应当是 400。
 */
public record ContactInfo(String email, String phone) {

    private static final String EMAIL_REGEX = "^[\\w.-]+@[\\w.-]+\\.\\w+$";

    public ContactInfo {
        if (email != null && !email.isBlank() && !email.matches(EMAIL_REGEX)) {
            throw BusinessException.of(UserResultCode.EMAIL_INVALID);
        }
        if (phone != null && !phone.isBlank() && !phone.matches(UserConstant.PHONE_REGEX)) {
            throw BusinessException.of(UserResultCode.PHONE_INVALID);
        }
    }

    public static ContactInfo empty() {
        return new ContactInfo(null, null);
    }

    public ContactInfo withEmail(String newEmail) {
        return new ContactInfo(newEmail, phone);
    }

    public ContactInfo withPhone(String newPhone) {
        return new ContactInfo(email, newPhone);
    }
}
