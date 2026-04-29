package com.cartethyia.easyorange.user.service.auth.strategy;

import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.user.dto.request.LoginRequest;
import com.cartethyia.easyorange.user.dto.response.LoginResponse;
import com.cartethyia.easyorange.user.enums.LoginMethod;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class LoginDispatcher {

    private final Map<LoginMethod, LoginStrategy> strategyMap;

    public LoginResponse login(LoginRequest loginRequest) {
        LoginMethod loginMethod = loginRequest.getEffectiveLoginMethod();

        LoginStrategy strategy = strategyMap.get(loginMethod);
        BizRequire.notNull(strategy, "不支持的登录方式：" + loginMethod);

        return strategy.login(loginRequest);
    }
}