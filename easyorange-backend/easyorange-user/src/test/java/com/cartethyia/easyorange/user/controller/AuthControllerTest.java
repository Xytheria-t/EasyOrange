package com.cartethyia.easyorange.user.controller;

import com.cartethyia.easyorange.user.dto.request.LoginDTO;
import com.cartethyia.easyorange.user.dto.response.LoginResponse;
import com.cartethyia.easyorange.user.dto.vo.UserVO;
import com.cartethyia.easyorange.framework.service.TokenService;
import com.cartethyia.easyorange.framework.config.properties.JwtProperties;
import com.cartethyia.easyorange.user.service.strategy.LoginDispatcher;
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
    private LoginDispatcher loginDispatcher;

    @Mock
    private TokenService tokenService;

    @Mock
    private JwtProperties jwtProperties;

    @InjectMocks
    private AuthController authController;

    @Test
    void shouldCallLogin() {
        // Given
        LoginDTO loginDTO = LoginDTO.builder()
                .account("testuser")
                .password("password123")
                .build();

        UserVO userInfo = UserVO.builder()
                .id(1L)
                .username("testuser")
                .build();

        LoginResponse mockResponse = LoginResponse.builder()
                .token("mock-jwt-token")
                .user(userInfo)
                .build();

        given(loginDispatcher.login(any(LoginDTO.class))).willReturn(mockResponse);

        // When
        var result = authController.login(loginDTO);

        // Then
        assertNotNull(result);
        assertEquals("A0000", result.code());
        assertEquals("mock-jwt-token", result.data().getToken());
        assertEquals("testuser", result.data().getUser().getUsername());
        verify(loginDispatcher, times(1)).login(any(LoginDTO.class));
    }

    @Test
    void shouldCallLogout() {
        // Given
        String authHeader = "Bearer valid-token";
        given(jwtProperties.getTokenPrefix()).willReturn("Bearer ");
        doNothing().when(tokenService).delToken("valid-token");

        // When
        var result = authController.logout(authHeader);

        // Then
        assertNotNull(result);
        assertEquals("A0000", result.code());
        verify(tokenService, times(1)).delToken("valid-token");
    }

    @Test
    void shouldCallLogoutWithoutAuthHeader() {
        // When
        var result = authController.logout(null);

        // Then
        assertNotNull(result);
        assertEquals("A0000", result.code());
        verify(tokenService, never()).delToken(anyString());
    }

    @Test
    void shouldCallRefreshToken() {
        // Given
        String authHeader = "Bearer old-token";
        String newToken = "new-refreshed-token";

        given(jwtProperties.getTokenPrefix()).willReturn("Bearer ");
        given(tokenService.refreshToken("old-token")).willReturn(newToken);

        // When
        var result = authController.refreshToken(authHeader);

        // Then
        assertNotNull(result);
        assertEquals("A0000", result.code());
        assertEquals("new-refreshed-token", result.data());
        verify(tokenService, times(1)).refreshToken("old-token");
    }
}
