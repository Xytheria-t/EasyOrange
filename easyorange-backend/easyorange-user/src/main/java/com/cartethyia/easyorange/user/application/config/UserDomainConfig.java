package com.cartethyia.easyorange.user.application.config;

import com.cartethyia.easyorange.user.domain.port.LoginAttemptPort;
import com.cartethyia.easyorange.user.domain.port.PasswordEncoderPort;
import com.cartethyia.easyorange.user.domain.port.SmsCodePort;
import com.cartethyia.easyorange.user.domain.repository.UserRepository;
import com.cartethyia.easyorange.user.domain.service.AdminUserManagementService;
import com.cartethyia.easyorange.user.domain.service.AuthenticationService;
import com.cartethyia.easyorange.user.domain.service.LoginSecurityService;
import com.cartethyia.easyorange.user.domain.service.PasswordManagementService;
import com.cartethyia.easyorange.user.domain.service.ProfileUpdateService;
import com.cartethyia.easyorange.user.domain.service.RegistrationService;
import com.cartethyia.easyorange.user.domain.service.SmsVerificationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class UserDomainConfig {

    @Bean
    AdminUserManagementService adminUserManagementService(UserRepository userRepository) {
        return new AdminUserManagementService(userRepository);
    }

    @Bean
    LoginSecurityService loginSecurityService(LoginAttemptPort loginAttemptPort) {
        return new LoginSecurityService(loginAttemptPort);
    }

    @Bean
    SmsVerificationService smsVerificationService(SmsCodePort smsCodePort) {
        return new SmsVerificationService(smsCodePort);
    }

    @Bean
    AuthenticationService authenticationService(
            UserRepository userRepository,
            PasswordEncoderPort passwordEncoder,
            LoginSecurityService loginSecurityService,
            SmsVerificationService smsVerificationService) {
        return new AuthenticationService(userRepository, passwordEncoder, loginSecurityService, smsVerificationService);
    }

    @Bean
    PasswordManagementService passwordManagementService(
            UserRepository userRepository,
            PasswordEncoderPort passwordEncoder,
            SmsVerificationService smsVerificationService) {
        return new PasswordManagementService(userRepository, passwordEncoder, smsVerificationService);
    }

    @Bean
    RegistrationService registrationService(UserRepository userRepository, PasswordEncoderPort passwordEncoder) {
        return new RegistrationService(userRepository, passwordEncoder);
    }

    @Bean
    ProfileUpdateService profileUpdateService(UserRepository userRepository) {
        return new ProfileUpdateService(userRepository);
    }
}
