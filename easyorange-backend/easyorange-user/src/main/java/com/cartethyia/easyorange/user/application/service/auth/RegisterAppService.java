package com.cartethyia.easyorange.user.application.service.auth;

import com.cartethyia.easyorange.user.application.command.RegisterCommand;
import com.cartethyia.easyorange.user.domain.service.RegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegisterAppService {

    private final RegistrationService registrationService;

    @Transactional(rollbackFor = Exception.class)
    public Long register(RegisterCommand command) {
        return registrationService.registerNewUser(
            command.getUsername(),
            command.getPassword()
        ).getId();
    }
}
