package com.cartethyia.easyorange.user.service.auth;

import com.cartethyia.easyorange.user.dto.bo.ForgotPasswordBo;
import com.cartethyia.easyorange.user.dto.bo.RegisterBo;
import com.cartethyia.easyorange.user.dto.request.LoginRequest;
import com.cartethyia.easyorange.user.dto.request.RefreshTokenRequest;
import com.cartethyia.easyorange.user.dto.response.LoginResponse;

public interface AuthService {

    Long register(RegisterBo bo);

    LoginResponse login(LoginRequest loginRequest);

    void logout(String accessToken, String refreshToken);

    String refreshToken(String refreshToken);

    Long forgotPassword(ForgotPasswordBo bo);
}