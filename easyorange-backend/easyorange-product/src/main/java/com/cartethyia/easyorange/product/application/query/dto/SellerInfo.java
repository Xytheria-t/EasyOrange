package com.cartethyia.easyorange.product.application.query.dto;

public record SellerInfo(Long id, String username, String nickName, String avatar) {
    public static SellerInfo of(Long id, String username, String nickName, String avatar) {
        return new SellerInfo(id, username, nickName, avatar);
    }
}
