package com.cartethyia.easyorange.user.config;

import com.cartethyia.easyorange.user.adapter.outbound.mock.MockSmsSenderAdapter;
import com.cartethyia.easyorange.user.domain.port.LoginAttemptPort;
import com.cartethyia.easyorange.user.domain.port.PasswordEncoderPort;
import com.cartethyia.easyorange.user.domain.port.SmsCodePort;
import com.cartethyia.easyorange.user.domain.port.SmsSenderPort;
import com.cartethyia.easyorange.user.domain.repository.UserRepository;
import com.cartethyia.easyorange.user.domain.service.AuthenticationService;
import com.cartethyia.easyorange.user.domain.service.LoginSecurityService;
import com.cartethyia.easyorange.user.domain.service.RegistrationService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserDomainConfig {

    @Bean
    @ConditionalOnMissingBean(SmsSenderPort.class)
    public SmsSenderPort smsSenderPort() {
        return new MockSmsSenderAdapter();
    }

    @Bean
    public LoginSecurityService loginSecurityService(LoginAttemptPort loginAttemptPort) {
        return new LoginSecurityService(loginAttemptPort);
    }

    @Bean
    public AuthenticationService authenticationService(
            UserRepository userRepository,
            PasswordEncoderPort passwordEncoder,
            LoginSecurityService loginSecurityService,
            SmsCodePort smsCodePort) {
        return new AuthenticationService(userRepository, passwordEncoder, loginSecurityService, smsCodePort);
    }

    @Bean
    public RegistrationService registrationService(
            UserRepository userRepository,
            PasswordEncoderPort passwordEncoder) {
        return new RegistrationService(userRepository, passwordEncoder);
    }
}
