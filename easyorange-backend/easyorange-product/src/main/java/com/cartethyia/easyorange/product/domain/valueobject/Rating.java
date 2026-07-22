package com.cartethyia.easyorange.product.domain.valueobject;

public record Rating(int value) {
    public Rating {
        if (value < 1 || value > 5) throw new IllegalArgumentException("评分必须在1-5之间");
    }

    public static Rating of(int value) {
        return new Rating(value);
    }

    public static final Rating DEFAULT = new Rating(5);
}
