package com.cartethyia.easyorange.user.service;

import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.user.dto.request.LoginDTO;
import com.cartethyia.easyorange.user.dto.response.LoginResponse;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoginStrategyContext {

    private final List<LoginStrategyService> loginStrategies;
    private Map<String, LoginStrategyService> strategyMap;

    @PostConstruct
    public void init() {
        strategyMap = new HashMap<>();
        for (LoginStrategyService strategy : loginStrategies) {
            for (String clientType : strategy.supportedClientTypes()) {
                LoginStrategyService prev = strategyMap.put(clientType, strategy);
                BizRequire.isNull(prev,
                        "客户端类型 '" + clientType + "' 被多个策略支持");
            }
        }
        strategyMap = Map.copyOf(strategyMap);
        log.info("登录策略初始化完成，共注册 {} 种策略：{}",
                strategyMap.size(), strategyMap.keySet());
    }

    public LoginResponse login(LoginDTO loginDTO) {
        String clientType = loginDTO.getEffectiveClientType();
        LoginStrategyService strategy = strategyMap.get(clientType);
        BizRequire.notNull(strategy, "不支持的客户端类型：" + clientType);
        return strategy.login(loginDTO);
    }
}
