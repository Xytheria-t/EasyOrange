package com.cartethyia.easyorange.user.service;

import com.cartethyia.easyorange.user.dto.request.LoginRequest;
import com.cartethyia.easyorange.user.dto.response.LoginResponse;

public interface LoginService {

    LoginResponse login(LoginRequest loginRequest);
}
