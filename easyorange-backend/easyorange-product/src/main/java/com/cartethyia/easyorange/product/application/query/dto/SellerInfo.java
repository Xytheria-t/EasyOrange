package com.cartethyia.easyorange.product.application.query.dto;

public record SellerInfo(Long id, String username, String nickName) {
    public static SellerInfo of(Long id, String username, String nickName) {
        return new SellerInfo(id, username, nickName);
    }
}
