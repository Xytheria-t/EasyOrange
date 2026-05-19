package com.cartethyia.easyorange.user.application.service;

import com.cartethyia.easyorange.user.adapter.inbound.web.dto.request.auth.RegisterRequest;
import com.cartethyia.easyorange.user.domain.aggregate.User;
import com.cartethyia.easyorange.user.domain.event.UserRegisteredEvent;
import com.cartethyia.easyorange.user.domain.port.output.UserEventPort;
import com.cartethyia.easyorange.user.domain.port.output.NicknameGenerationPort;
import com.cartethyia.easyorange.user.domain.service.UserRegistrationDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserRegistrationAppService {

    private final UserRegistrationDomainService userRegistrationDomainService;
    private final NicknameGenerationPort nicknameGenerationPort;
    private final UserEventPort userEventPort;

    @Transactional(rollbackFor = Exception.class)
    public Long register(RegisterRequest request) {
        String nickname = nicknameGenerationPort.generate();

        User saved = userRegistrationDomainService.register(
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
