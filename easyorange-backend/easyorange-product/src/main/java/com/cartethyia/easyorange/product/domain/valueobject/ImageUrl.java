package com.cartethyia.easyorange.product.domain.valueobject;

import com.cartethyia.easyorange.common.ddd.ValueObject;
import com.cartethyia.easyorange.common.util.BizRequire;

import java.util.Objects;
import java.util.regex.Pattern;

public final class ImageUrl implements ValueObject {

    private static final Pattern URL_PATTERN = Pattern.compile("^https?://.*");

    private final String value;

    public ImageUrl(String value) {
        BizRequire.notBlank(value, "图片URL不能为空");
        BizRequire.isTrue(URL_PATTERN.matcher(value).matches(), "图片URL格式不正确");
        this.value = value.trim();
    }

    public String value() {
        return value;
    }

    public String trimmed() {
        return value.trim();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ImageUrl imageUrl = (ImageUrl) o;
        return Objects.equals(value, imageUrl.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "ImageUrl{" + value + '}';
    }
}
