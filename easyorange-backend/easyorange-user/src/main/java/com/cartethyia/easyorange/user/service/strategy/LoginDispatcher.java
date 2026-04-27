package com.cartethyia.easyorange.user.service.strategy;

import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.user.dto.request.LoginRequest;
import com.cartethyia.easyorange.user.dto.response.LoginResponse;
import com.cartethyia.easyorange.user.enums.ClientType;
import com.cartethyia.easyorange.user.enums.LoginMethod;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoginDispatcher {

    private final List<LoginStrategy> loginStrategies;
    private Map<LoginMethod, LoginStrategy> strategyMap;

    @PostConstruct
    public void init() {
        strategyMap = new HashMap<>();
        for (LoginStrategy strategy : loginStrategies) {
            LoginMethod method = strategy.supportedLoginMethod();
            LoginStrategy prev = strategyMap.put(method, strategy);
            BizRequire.isNull(prev,
                    "登录方式 '" + method + "' 被多个策略支持");
        }
        strategyMap = Map.copyOf(strategyMap);
        log.info("登录策略初始化完成，共注册 {} 种策略：{}",
                strategyMap.size(),
                strategyMap.values().stream()
                        .map(s -> s.getClass().getSimpleName())
                        .collect(Collectors.joining(", ")));
    }

    public LoginResponse login(LoginRequest loginRequest) {
        LoginMethod loginMethod = loginRequest.getEffectiveLoginMethod();
        ClientType clientType = loginRequest.getEffectiveClientType();

        LoginStrategy strategy = strategyMap.get(loginMethod);
        BizRequire.notNull(strategy, "不支持的登录方式：" + loginMethod);

        if (!strategy.supportedClientTypes().contains(clientType)) {
            BizRequire.fail("登录方式 '" + loginMethod + "' 不支持 '" + clientType + "' 平台");
        }

        return strategy.login(loginRequest);
    }
}
