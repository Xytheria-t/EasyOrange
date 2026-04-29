package com.cartethyia.easyorange.user.controller;

import com.cartethyia.easyorange.framework.config.properties.JwtProperties;
import com.cartethyia.easyorange.user.converter.UserConverter;
import com.cartethyia.easyorange.user.dto.request.LoginRequest;
import com.cartethyia.easyorange.user.dto.request.RefreshTokenRequest;
import com.cartethyia.easyorange.user.dto.response.LoginResponse;
import com.cartethyia.easyorange.user.dto.vo.UserVO;
import com.cartethyia.easyorange.user.service.auth.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private JwtProperties jwtProperties;

    @Mock
    private UserConverter userConverter;

    @InjectMocks
    private AuthController authController;

    @Test
    void shouldCallLogin() {
        LoginRequest loginRequest = LoginRequest.builder()
                .account("testuser")
                .password("password123")
                .build();

        UserVO userInfo = UserVO.builder()
                .id(1L)
                .username("testuser")
                .build();

        LoginResponse mockResponse = LoginResponse.builder()
                .token("mock-jwt-token")
                .refreshToken("mock-refresh-token")
                .user(userInfo)
                .build();

        given(authService.login(any(LoginRequest.class))).willReturn(mockResponse);

        var result = authController.login(loginRequest);

        assertNotNull(result);
        assertEquals("A0000", result.code());
        assertEquals("mock-jwt-token", result.data().getToken());
        assertEquals("mock-refresh-token", result.data().getRefreshToken());
        assertEquals("testuser", result.data().getUser().getUsername());
        verify(authService, times(1)).login(any(LoginRequest.class));
    }

    @Test
    void shouldCallLogout() {
        String authHeader = "Bearer valid-token";
        String refreshHeader = "Bearer valid-refresh-token";
        given(jwtProperties.getTokenPrefix()).willReturn("Bearer ");
        doNothing().when(authService).logout("valid-token", "valid-refresh-token");

        var result = authController.logout(authHeader, refreshHeader);

        assertNotNull(result);
        assertEquals("A0000", result.code());
        verify(authService, times(1)).logout("valid-token", "valid-refresh-token");
    }

    @Test
    void shouldCallLogoutWithoutAuthHeader() {
        var result = authController.logout(null, null);

        assertNotNull(result);
        assertEquals("A0000", result.code());
        verify(authService, never()).logout(anyString(), anyString());
    }

    @Test
    void shouldCallRefreshToken() {
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("old-refresh-token")
                .build();
        String newToken = "new-refreshed-token";

        given(authService.refreshToken("old-refresh-token")).willReturn(newToken);

        var result = authController.refreshToken(request);

        assertNotNull(result);
        assertEquals("A0000", result.code());
        assertEquals("new-refreshed-token", result.data());
        verify(authService, times(1)).refreshToken("old-refresh-token");
    }
}
