package com.cartethyia.easyorange.user.service.auth.strategy;

import com.cartethyia.easyorange.user.dto.request.LoginRequest;
import com.cartethyia.easyorange.user.dto.response.LoginResponse;

public interface LoginStrategy {

    LoginResponse login(LoginRequest loginRequest);
}