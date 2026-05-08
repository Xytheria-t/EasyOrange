package com.cartethyia.easyorange.user.infrastructure.config;

import com.cartethyia.easyorange.user.adapter.outbound.cache.RedisLoginAttemptAdapter;
import com.cartethyia.easyorange.user.adapter.outbound.cache.RedisSmsCodeAdapter;
import com.cartethyia.easyorange.user.domain.port.output.LoginAttemptPort;
import com.cartethyia.easyorange.user.domain.port.output.PasswordEncoderPort;
import com.cartethyia.easyorange.user.domain.port.output.SmsCodePort;
import com.cartethyia.easyorange.user.domain.port.output.SmsRateLimitPort;
import com.cartethyia.easyorange.user.domain.repository.UserRepository;
import com.cartethyia.easyorange.user.domain.service.AuthenticationDomainService;
import com.cartethyia.easyorange.user.domain.service.LoginSecurityDomainService;
import com.cartethyia.easyorange.user.domain.service.PasswordDomainService;
import com.cartethyia.easyorange.user.domain.service.SmsCodeDomainService;
import com.cartethyia.easyorange.user.domain.service.UserRegistrationDomainService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserDomainConfig {

    @Bean
    public PasswordDomainService passwordDomainService(PasswordEncoderPort passwordEncoderPort) {
        return new PasswordDomainService(passwordEncoderPort);
    }

    @Bean
    public LoginSecurityDomainService loginSecurityDomainService(LoginAttemptPort loginAttemptPort) {
        return new LoginSecurityDomainService(loginAttemptPort);
    }

    @Bean
    public AuthenticationDomainService authenticationDomainService(
            UserRepository userRepository,
            PasswordDomainService passwordDomainService,
            LoginSecurityDomainService loginSecurityDomainService,
            SmsCodeDomainService smsCodeDomainService) {
        return new AuthenticationDomainService(userRepository, passwordDomainService, loginSecurityDomainService, smsCodeDomainService);
    }

    @Bean
    public UserRegistrationDomainService userRegistrationDomainService(
            UserRepository userRepository,
            PasswordDomainService passwordDomainService) {
        return new UserRegistrationDomainService(userRepository, passwordDomainService);
    }

    @Bean
    public SmsCodeDomainService smsCodeDomainService(SmsCodePort smsCodePort, SmsRateLimitPort smsRateLimitPort) {
        return new SmsCodeDomainService(smsCodePort, smsRateLimitPort);
    }
}
