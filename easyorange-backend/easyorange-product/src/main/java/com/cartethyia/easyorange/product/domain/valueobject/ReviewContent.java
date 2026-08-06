package com.cartethyia.easyorange.product.domain.valueobject;

public record ReviewContent(String value) {
    public ReviewContent {
        if (value != null && value.length() > 2000) throw new IllegalArgumentException("评价内容最多2000字");
    }

    public static ReviewContent of(String value) {
        return new ReviewContent(value);
    }

    public boolean isBlank() {
        return value == null || value.isBlank();
    }

    public static final ReviewContent EMPTY = new ReviewContent("");
}
