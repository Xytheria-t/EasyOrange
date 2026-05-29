package com.cartethyia.easyorange.user.application.service.auth;

import com.cartethyia.easyorange.user.application.command.RegisterCommand;
import com.cartethyia.easyorange.user.domain.aggregate.User;
import com.cartethyia.easyorange.user.domain.service.RegistrationService;
import com.cartethyia.easyorange.user.domain.valueobject.Credentials;
import com.cartethyia.easyorange.user.domain.valueobject.ImmutablePersonalInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RegisterAppService 测试")
class RegisterAppServiceTest {

    @Mock
    private RegistrationService registrationService;

    private RegisterAppService service;

    @BeforeEach
    void setUp() {
        service = new RegisterAppService(registrationService);
    }

    @Test
    @DisplayName("注册成功 — 应委托领域服务注册并返回ID")
    void register_success() {
        String username = "newuser";
        String password = "Password123";
        RegisterCommand command = new RegisterCommand(username, password);

        User savedUser = User.builder()
            .id(100L)
            .credentials(new Credentials(username, "encodedPassword"))
            .personalInfo(ImmutablePersonalInfo.builder().nickName(username).build())
            .build();
        when(registrationService.registerNewUser(username, password))
            .thenReturn(savedUser);

        Long result = service.register(command);

        assertThat(result).isEqualTo(100L);
        verify(registrationService).registerNewUser(username, password);
    }
}