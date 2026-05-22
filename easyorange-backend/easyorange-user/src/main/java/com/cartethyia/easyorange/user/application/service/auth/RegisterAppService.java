package com.cartethyia.easyorange.user.application.service.auth;

import com.cartethyia.easyorange.user.adapter.inbound.web.dto.request.auth.RegisterRequest;
import com.cartethyia.easyorange.user.domain.aggregate.User;
import com.cartethyia.easyorange.user.domain.event.UserRegisteredEvent;
import com.cartethyia.easyorange.user.domain.port.output.UserEventPort;
import com.cartethyia.easyorange.user.domain.port.output.NicknameGeneratorPort;
import com.cartethyia.easyorange.user.domain.service.RegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegisterAppService {

    private final RegistrationService registrationService;
    private final NicknameGeneratorPort nicknameGenerationPort;
    private final UserEventPort userEventPort;

    @Transactional(rollbackFor = Exception.class)
    public Long register(RegisterRequest request) {
        String nickname = nicknameGenerationPort.generate();

        User saved = registrationService.registerNewUser(
            request.username(),
            request.password(),
            request.phone(),
            request.email(),
            nickname
        );

        userEventPort.publishUserRegistered(new UserRegisteredEvent(saved.getId(), saved.getUsername()));

        return saved.getId();
    }
}