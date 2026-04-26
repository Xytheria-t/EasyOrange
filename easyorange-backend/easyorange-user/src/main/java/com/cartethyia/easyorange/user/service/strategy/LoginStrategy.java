package com.cartethyia.easyorange.user.service.strategy;

import com.cartethyia.easyorange.user.dto.request.LoginDTO;
import com.cartethyia.easyorange.user.dto.response.LoginResponse;
import com.cartethyia.easyorange.user.enums.LoginMethod;

public interface LoginStrategy {

    LoginResponse login(LoginDTO loginDTO);

    LoginMethod supportedLoginMethod();
}
