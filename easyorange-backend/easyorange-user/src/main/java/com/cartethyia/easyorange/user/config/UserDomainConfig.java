package com.cartethyia.easyorange.user.config;

import com.cartethyia.easyorange.user.domain.port.LoginAttemptPort;
import com.cartethyia.easyorange.user.domain.port.PasswordEncoderPort;
import com.cartethyia.easyorange.user.domain.port.SmsCodePort;
import com.cartethyia.easyorange.user.domain.repository.UserRepository;
import com.cartethyia.easyorange.user.domain.service.AuthenticationService;
import com.cartethyia.easyorange.user.domain.service.LoginSecurityService;
import com.cartethyia.easyorange.user.domain.service.ProfileUpdateService;
import com.cartethyia.easyorange.user.domain.service.RegistrationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class UserDomainConfig {

    @Bean
    LoginSecurityService loginSecurityService(LoginAttemptPort loginAttemptPort) {
        return new LoginSecurityService(loginAttemptPort);
    }

    @Bean
    AuthenticationService authenticationService(
            UserRepository userRepository,
            PasswordEncoderPort passwordEncoder,
            LoginSecurityService loginSecurityService,
            SmsCodePort smsCodePort) {
        return new AuthenticationService(userRepository, passwordEncoder, loginSecurityService, smsCodePort);
    }

    @Bean
    RegistrationService registrationService(
            UserRepository userRepository,
            PasswordEncoderPort passwordEncoder) {
        return new RegistrationService(userRepository, passwordEncoder);
    }

    @Bean
    ProfileUpdateService profileUpdateService(UserRepository userRepository) {
        return new ProfileUpdateService(userRepository);
    }
}
