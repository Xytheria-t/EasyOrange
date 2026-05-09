package com.cartethyia.easyorange.product.domain.valueobject;

public record SellerInfo(Long id, String username, String nickName, String avatar) {

    public static SellerInfo of(Long id, String username, String nickName, String avatar) {
        return new SellerInfo(id, username, nickName, avatar);
    }

    public static SellerInfo empty(Long id) {
        return new SellerInfo(id, "未知用户", null, null);
    }
}
