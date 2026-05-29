package com.cartethyia.easyorange.user.domain.valueobject;

public sealed interface LoginCommand {

    record PasswordLogin(String identifier, String password) implements LoginCommand {}

    record SmsLogin(String phone, String verifyCode) implements LoginCommand {}
}
