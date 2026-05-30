package com.cartethyia.easyorange.user.domain.valueobject;

public sealed interface LoginCredential {

    record Password(String identifier, String password) implements LoginCredential {}

    record Sms(String phone, String verifyCode) implements LoginCredential {}
}
