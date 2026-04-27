package com.cartethyia.easyorange.user.service.strategy;

import com.cartethyia.easyorange.user.dto.request.LoginRequest;
import com.cartethyia.easyorange.user.dto.response.LoginResponse;
import com.cartethyia.easyorange.user.enums.ClientType;
import com.cartethyia.easyorange.user.enums.LoginMethod;

import java.util.Set;

public interface LoginStrategy {

    LoginResponse login(LoginRequest loginRequest);

    LoginMethod supportedLoginMethod();

    default Set<ClientType> supportedClientTypes() {
        return Set.of(ClientType.values());
    }
}
