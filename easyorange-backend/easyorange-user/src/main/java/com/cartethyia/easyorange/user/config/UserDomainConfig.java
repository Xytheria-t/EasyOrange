package com.cartethyia.easyorange.user.config;

import com.cartethyia.easyorange.user.adapter.outbound.mock.MockSmsSenderAdapter;
import com.cartethyia.easyorange.user.adapter.outbound.cache.RedisLoginAttemptAdapter;
import com.cartethyia.easyorange.user.adapter.outbound.cache.RedisSmsCodeAdapter;
import com.cartethyia.easyorange.user.domain.port.LoginAttemptPort;
import com.cartethyia.easyorange.user.domain.port.PasswordEncoderPort;
import com.cartethyia.easyorange.user.domain.port.SmsCodePort;
import com.cartethyia.easyorange.user.domain.port.SmsRateLimitPort;
import com.cartethyia.easyorange.user.domain.port.SmsSenderPort;
import com.cartethyia.easyorange.user.domain.repository.UserRepository;
import com.cartethyia.easyorange.user.domain.service.AuthenticationService;
import com.cartethyia.easyorange.user.domain.service.LoginSecurityService;
import com.cartethyia.easyorange.user.domain.service.PasswordManagementService;
import com.cartethyia.easyorange.user.domain.service.RegistrationService;
import com.cartethyia.easyorange.user.domain.service.SmsCodeService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserDomainConfig {

    @Bean
    public LoginSecurityService loginSecurityService(LoginAttemptPort loginAttemptPort) {
        return new LoginSecurityService(loginAttemptPort);
    }

    @Bean
    public AuthenticationService authenticationService(
            UserRepository userRepository,
            PasswordEncoderPort passwordEncoder,
            LoginSecurityService loginSecurityService,
            SmsCodeService smsCodeService) {
        return new AuthenticationService(userRepository, passwordEncoder, loginSecurityService, smsCodeService);
    }

    @Bean
    public RegistrationService registrationService(
            UserRepository userRepository,
            PasswordEncoderPort passwordEncoder) {
        return new RegistrationService(userRepository, passwordEncoder);
    }

    @Bean
    public SmsCodeService smsCodeService(SmsCodePort smsCodePort, SmsRateLimitPort smsRateLimitPort, SmsSenderPort smsSenderPort) {
        return new SmsCodeService(smsCodePort, smsRateLimitPort, smsSenderPort);
    }

    @Bean
    public SmsSenderPort smsSenderPort() {
        return new MockSmsSenderAdapter();
    }

    @Bean
    public PasswordManagementService passwordManagementService(
            UserRepository userRepository,
            PasswordEncoderPort passwordEncoder,
            SmsCodeService smsCodeService) {
        return new PasswordManagementService(userRepository, passwordEncoder, smsCodeService);
    }
}
