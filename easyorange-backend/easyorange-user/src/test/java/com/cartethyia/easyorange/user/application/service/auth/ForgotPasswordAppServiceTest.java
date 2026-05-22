package com.cartethyia.easyorange.user.application.service.auth;

import com.cartethyia.easyorange.user.adapter.inbound.web.dto.request.password.ForgotPasswordRequest;
import com.cartethyia.easyorange.user.domain.aggregate.User;
import com.cartethyia.easyorange.user.domain.event.ForgotPasswordEvent;
import com.cartethyia.easyorange.user.domain.port.output.UserEventPort;
import com.cartethyia.easyorange.user.domain.service.AuthenticationService;
import com.cartethyia.easyorange.user.domain.valueobject.Credentials;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ForgotPasswordAppService 测试")
class ForgotPasswordAppServiceTest {

    @Mock
    private AuthenticationService authenticationService;

    @Mock
    private UserEventPort userEventPort;

    private ForgotPasswordAppService service;

    @BeforeEach
    void setUp() {
        service = new ForgotPasswordAppService(authenticationService, userEventPort);
    }

    @Test
    @DisplayName("忘记密码成功 — 用户存在时发布事件")
    void forgotPassword_userExists() {
        String phone = "13812345678";
        String verifyCode = "123456";
        String newPassword = "NewPass123";
        ForgotPasswordRequest request = new ForgotPasswordRequest(phone, verifyCode, newPassword);

        User user = User.builder()
            .id(1L)
            .credentials(new Credentials("testuser", "encodedPassword"))
            .build();
        when(authenticationService.resetPassword(phone, verifyCode, newPassword))
            .thenReturn(user);

        service.forgotPassword(request);

        ArgumentCaptor<ForgotPasswordEvent> eventCaptor = ArgumentCaptor.forClass(ForgotPasswordEvent.class);
        verify(userEventPort).publishForgotPassword(eventCaptor.capture());
        ForgotPasswordEvent event = eventCaptor.getValue();
        assertThat(event.getUserId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("忘记密码 — 用户不存在时不发布事件")
    void forgotPassword_userNotFound() {
        String phone = "13812345678";
        String verifyCode = "123456";
        String newPassword = "NewPass123";
        ForgotPasswordRequest request = new ForgotPasswordRequest(phone, verifyCode, newPassword);

        when(authenticationService.resetPassword(phone, verifyCode, newPassword))
            .thenReturn(null);

        service.forgotPassword(request);

        verify(userEventPort, never()).publishForgotPassword(any());
    }
}