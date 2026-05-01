package com.cartethyia.easyorange.user.domain.port;

public interface UserEventPort {

    void publishUserRegistered(Long userId, String username);

    void publishPasswordChanged(Long userId);

    void publishForgotPassword(Long userId, String phone);
}
