package com.cartethyia.easyorange.user.domain.valueobject;

import com.cartethyia.easyorange.common.ddd.ValueObject;
import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.user.constant.UserConstants;

import java.util.Objects;
import java.util.regex.Pattern;

public final class Password implements ValueObject {

    private static final Pattern PASSWORD_PATTERN = Pattern.compile(UserConstants.PASSWORD_REGEX);

    private final String encodedValue;
    private final String rawValue;

    private Password(String encodedValue, String rawValue) {
        this.encodedValue = encodedValue;
        this.rawValue = rawValue;
    }

    public static Password fromRaw(String rawPassword) {
        BizRequire.notBlank(rawPassword, "密码不能为空");
        BizRequire.isTrue(PASSWORD_PATTERN.matcher(rawPassword).matches(),
                "密码必须包含大小写字母和数字，长度6-20位");
        return new Password(null, rawPassword);
    }

    public static Password fromEncoded(String encodedPassword) {
        BizRequire.notBlank(encodedPassword, "加密密码不能为空");
        return new Password(encodedPassword, null);
    }

    public String value() {
        return encodedValue != null ? encodedValue : rawValue;
    }

    public String getEncodedValue() {
        return encodedValue;
    }

    public boolean matches(java.util.function.BiFunction<String, String, Boolean> matcher) {
        if (rawValue == null) {
            return false;
        }
        return matcher.apply(rawValue, encodedValue);
    }

    public Password encode(java.util.function.Function<String, String> encoder) {
        if (encodedValue != null) {
            return this;
        }
        return new Password(encoder.apply(rawValue), null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Password password = (Password) o;
        return Objects.equals(encodedValue, password.encodedValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(encodedValue);
    }

    @Override
    public String toString() {
        return "Password{***}";
    }
}