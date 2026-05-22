package com.cartethyia.easyorange.user.config;

import com.cartethyia.easyorange.user.adapter.outbound.cache.RedisLoginAttemptAdapter;
import com.cartethyia.easyorange.user.adapter.outbound.cache.RedisSmsCodeAdapter;
import com.cartethyia.easyorange.user.domain.port.output.LoginAttemptPort;
import com.cartethyia.easyorange.user.domain.port.output.NicknameGeneratorPort;
import com.cartethyia.easyorange.user.domain.port.output.PasswordEncoderPort;
import com.cartethyia.easyorange.user.domain.port.output.SmsCodePort;
import com.cartethyia.easyorange.user.domain.port.output.SmsRateLimitPort;
import com.cartethyia.easyorange.user.domain.repository.UserRepository;
import com.cartethyia.easyorange.user.domain.service.AuthenticationService;
import com.cartethyia.easyorange.user.domain.service.LoginSecurityService;
import com.cartethyia.easyorange.user.domain.service.RegistrationService;
import com.cartethyia.easyorange.user.domain.service.SmsCodeService;
import com.cartethyia.easyorange.user.infrastructure.util.NicknameGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserDomainConfig {

    @Bean
    public NicknameGeneratorPort nicknameGeneratorPort() {
        return new NicknameGenerator();
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
    public SmsCodeService smsCodeService(SmsCodePort smsCodePort, SmsRateLimitPort smsRateLimitPort) {
        return new SmsCodeService(smsCodePort, smsRateLimitPort);
    }
}
