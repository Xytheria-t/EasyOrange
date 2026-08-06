package com.cartethyia.easyorange.product.domain.valueobject;

import com.cartethyia.easyorange.common.util.BizRequire;
import java.util.regex.Pattern;

public record ImageUrl(String value) {

    private static final Pattern URL_PATTERN = Pattern.compile("^https?://.*");

    public ImageUrl {
        BizRequire.notBlank(value, "图片URL不能为空");
        value = value.trim();
        BizRequire.requireTrue(URL_PATTERN.matcher(value).matches(), "图片URL格式不正确");
    }

    public static ImageUrl of(String value) {
        return new ImageUrl(value);
    }
}
