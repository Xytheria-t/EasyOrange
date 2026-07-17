package com.cartethyia.easyorange.product.application.query;

public record SellerInfo(String id, String username, String nickName, String avatar) {
    public static SellerInfo of(String id, String username, String nickName, String avatar) {
        return new SellerInfo(id, username, nickName, avatar);
    }
}
