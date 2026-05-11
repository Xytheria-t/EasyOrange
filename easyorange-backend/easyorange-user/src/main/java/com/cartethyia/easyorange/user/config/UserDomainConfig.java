package com.cartethyia.easyorange.user.config;

import com.cartethyia.easyorange.user.adapter.outbound.cache.RedisLoginAttemptAdapter;
import com.cartethyia.easyorange.user.adapter.outbound.cache.RedisSmsCodeAdapter;
import com.cartethyia.easyorange.user.domain.port.output.LoginAttemptPort;
import com.cartethyia.easyorange.user.domain.port.output.PasswordEncoderPort;
import com.cartethyia.easyorange.user.domain.port.output.SmsCodePort;
import com.cartethyia.easyorange.user.domain.port.output.SmsRateLimitPort;
import com.cartethyia.easyorange.user.domain.repository.UserRepository;
import com.cartethyia.easyorange.user.domain.service.AuthenticationDomainService;
import com.cartethyia.easyorange.user.domain.service.LoginSecurityDomainService;
import com.cartethyia.easyorange.user.domain.service.SmsCodeDomainService;
import com.cartethyia.easyorange.user.domain.service.UserRegistrationDomainService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserDomainConfig {

    @Bean
    public LoginSecurityDomainService loginSecurityDomainService(LoginAttemptPort loginAttemptPort) {
        return new LoginSecurityDomainService(loginAttemptPort);
    }

    @Bean
    public AuthenticationDomainService authenticationDomainService(
            UserRepository userRepository,
            PasswordEncoderPort passwordEncoder,
            LoginSecurityDomainService loginSecurityDomainService,
            SmsCodeDomainService smsCodeDomainService) {
        return new AuthenticationDomainService(userRepository, passwordEncoder, loginSecurityDomainService, smsCodeDomainService);
    }

    @Bean
    public UserRegistrationDomainService userRegistrationDomainService(
            UserRepository userRepository,
            PasswordEncoderPort passwordEncoder) {
        return new UserRegistrationDomainService(userRepository, passwordEncoder);
    }

    @Bean
    public SmsCodeDomainService smsCodeDomainService(SmsCodePort smsCodePort, SmsRateLimitPort smsRateLimitPort) {
        return new SmsCodeDomainService(smsCodePort, smsRateLimitPort);
    }
}
