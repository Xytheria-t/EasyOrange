package com.cartethyia.easyorange.user.application.service;

import com.cartethyia.easyorange.user.adapter.inbound.web.dto.request.auth.RegisterRequest;
import com.cartethyia.easyorange.user.domain.aggregate.User;
import com.cartethyia.easyorange.user.domain.event.UserRegisteredEvent;
import com.cartethyia.easyorange.user.domain.port.output.UserEventPort;
import com.cartethyia.easyorange.user.domain.service.UserRegistrationDomainService;
import com.cartethyia.easyorange.user.domain.valueobject.Credentials;
import com.cartethyia.easyorange.user.domain.valueobject.PersonalInfo;
import com.cartethyia.easyorange.user.infrastructure.util.NicknameGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserRegistrationAppService 测试")
class UserRegistrationAppServiceTest {

    @Mock
    private UserRegistrationDomainService userRegistrationDomainService;

    @Mock
    private NicknameGenerator nicknameGenerator;

    @Mock
    private UserEventPort userEventPort;

    private UserRegistrationAppService service;

    @BeforeEach
    void setUp() {
        service = new UserRegistrationAppService(userRegistrationDomainService, nicknameGenerator, userEventPort);
    }

    @Test
    @DisplayName("注册成功 — 应生成昵称、委托领域服务注册、发布事件并返回ID")
    void register_success() {
        String username = "newuser";
        String password = "Password123";
        String phone = "13812345678";
        String email = "new@example.com";
        RegisterRequest request = new RegisterRequest(username, password, phone, email);

        String generatedNickname = "阳光番茄";
        when(nicknameGenerator.generate()).thenReturn(generatedNickname);

        User savedUser = User.builder()
            .id(100L)
            .credentials(new Credentials(username, "encodedPassword"))
            .personalInfo(new PersonalInfo(null, generatedNickname, null, null, null))
            .build();
        when(userRegistrationDomainService.register(username, password, phone, email, generatedNickname))
            .thenReturn(savedUser);

        Long result = service.register(request);

        assertThat(result).isEqualTo(100L);
        verify(nicknameGenerator).generate();
        verify(userRegistrationDomainService).register(username, password, phone, email, generatedNickname);

        ArgumentCaptor<UserRegisteredEvent> eventCaptor = ArgumentCaptor.forClass(UserRegisteredEvent.class);
        verify(userEventPort).publishUserRegistered(eventCaptor.capture());
        UserRegisteredEvent event = eventCaptor.getValue();
        assertThat(event.getUserId()).isEqualTo(100L);
        assertThat(event.getUsername()).isEqualTo("newuser");
    }

    @Test
    @DisplayName("注册成功 — phone和email为空也应注册成功")
    void register_withoutContactInfo() {
        String username = "newuser";
        String password = "Password123";
        RegisterRequest request = new RegisterRequest(username, password, null, null);

        String generatedNickname = "快乐橙子";
        when(nicknameGenerator.generate()).thenReturn(generatedNickname);

        User savedUser = User.builder()
            .id(101L)
            .credentials(new Credentials(username, "encodedPassword"))
            .personalInfo(new PersonalInfo(null, generatedNickname, null, null, null))
            .build();
        when(userRegistrationDomainService.register(username, password, null, null, generatedNickname))
            .thenReturn(savedUser);

        Long result = service.register(request);

        assertThat(result).isEqualTo(101L);
        verify(userRegistrationDomainService).register(username, password, null, null, generatedNickname);
        verify(userEventPort).publishUserRegistered(any(UserRegisteredEvent.class));
    }
}
