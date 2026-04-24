package com.cartethyia.easyorange.product.domain.valueobject;

import com.cartethyia.easyorange.common.util.BizRequire;

public record ImageUrl(String value) {
    private static final java.util.regex.Pattern URL_PATTERN = java.util.regex.Pattern.compile("^https?://.*");

    public ImageUrl {
        BizRequire.notBlank(value, "图片URL不能为空");
        BizRequire.isTrue(URL_PATTERN.matcher(value).matches(), "图片URL格式不正确");
        value = value.trim();
    }

    public String trimmed() {
        return value.trim();
    }
}