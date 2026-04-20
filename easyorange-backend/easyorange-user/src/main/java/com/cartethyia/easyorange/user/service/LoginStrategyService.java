package com.cartethyia.easyorange.user.service;

import com.cartethyia.easyorange.user.dto.request.LoginDTO;
import com.cartethyia.easyorange.user.dto.response.LoginResponse;

public interface LoginStrategyService {

    LoginResponse login(LoginDTO loginDTO);

    String[] supportedClientTypes();
}
