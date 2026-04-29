package com.cartethyia.easyorange.user.config;

import com.cartethyia.easyorange.user.enums.LoginMethod;
import com.cartethyia.easyorange.user.service.auth.strategy.PasswordLoginStrategy;
import com.cartethyia.easyorange.user.service.auth.strategy.LoginStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class LoginStrategyConfig {

    @Bean
    public Map<LoginMethod, LoginStrategy> strategyMap(PasswordLoginStrategy passwordLoginStrategy) {
        return Map.of(
                LoginMethod.PASSWORD, passwordLoginStrategy
        );
    }
}
