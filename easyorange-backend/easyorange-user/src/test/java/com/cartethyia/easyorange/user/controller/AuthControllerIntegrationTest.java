package com.cartethyia.easyorange.user.controller;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.framework.config.properties.JwtProperties;
import com.cartethyia.easyorange.framework.exception.GlobalExceptionHandler;
import com.cartethyia.easyorange.user.converter.UserConverter;
import com.cartethyia.easyorange.user.dto.bo.ForgotPasswordBo;
import com.cartethyia.easyorange.user.dto.bo.RegisterBo;
import com.cartethyia.easyorange.user.dto.request.ForgotPasswordRequest;
import com.cartethyia.easyorange.user.dto.request.LoginRequest;
import com.cartethyia.easyorange.user.dto.request.RefreshTokenRequest;
import com.cartethyia.easyorange.user.dto.request.RegisterRequest;
import com.cartethyia.easyorange.user.dto.response.LoginResponse;
import com.cartethyia.easyorange.user.dto.vo.UserVO;
import com.cartethyia.easyorange.user.service.auth.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerIntegrationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc createMvc(AuthService authService, JwtProperties jwtProperties, UserConverter userConverter) {
        AuthController controller = new AuthController(authService, jwtProperties, userConverter);
        return MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void register_withValidRequest_returnsOk() throws Exception {
        AuthService authService = mock(AuthService.class);
        JwtProperties jwtProperties = mock(JwtProperties.class);
        UserConverter userConverter = mock(UserConverter.class);

        MockMvc mockMvc = createMvc(authService, jwtProperties, userConverter);

        given(userConverter.toBo(any(RegisterRequest.class))).willReturn(new RegisterBo("testuser", "Test123"));
        given(authService.register(any(RegisterBo.class))).willReturn(1L);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"testuser\",\"password\":\"Test123\",\"email\":\"test@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A0000"))
                .andExpect(jsonPath("$.data").value(1));
    }

    @Test
    void login_withValidRequest_returnsOk() throws Exception {
        AuthService authService = mock(AuthService.class);
        JwtProperties jwtProperties = mock(JwtProperties.class);
        UserConverter userConverter = mock(UserConverter.class);

        MockMvc mockMvc = createMvc(authService, jwtProperties, userConverter);

        UserVO userInfo = UserVO.builder().id(1L).username("testuser").build();
        LoginResponse mockResponse = LoginResponse.builder()
                .token("mock-jwt-token")
                .refreshToken("mock-refresh-token")
                .user(userInfo)
                .build();

        given(authService.login(any(LoginRequest.class))).willReturn(mockResponse);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"account\":\"testuser\",\"password\":\"Test123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A0000"))
                .andExpect(jsonPath("$.data.token").value("mock-jwt-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("mock-refresh-token"))
                .andExpect(jsonPath("$.data.user.username").value("testuser"));
    }

    @Test
    void logout_withValidToken_revokesTokens() throws Exception {
        AuthService authService = mock(AuthService.class);
        JwtProperties jwtProperties = mock(JwtProperties.class);
        UserConverter userConverter = mock(UserConverter.class);

        given(jwtProperties.getTokenPrefix()).willReturn("Bearer ");
        doNothing().when(authService).logout(any(), any());

        MockMvc mockMvc = createMvc(authService, jwtProperties, userConverter);

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer valid-access-token")
                        .header("X-Refresh-Token", "Bearer valid-refresh-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A0000"));

        verify(authService).logout("valid-access-token", "valid-refresh-token");
    }

    @Test
    void logout_withoutAuthHeader_returnsOk() throws Exception {
        AuthService authService = mock(AuthService.class);
        JwtProperties jwtProperties = mock(JwtProperties.class);
        UserConverter userConverter = mock(UserConverter.class);

        MockMvc mockMvc = createMvc(authService, jwtProperties, userConverter);

        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A0000"));
    }

    @Test
    void refresh_withValidRefreshToken_returnsNewAccessToken() throws Exception {
        AuthService authService = mock(AuthService.class);
        JwtProperties jwtProperties = mock(JwtProperties.class);
        UserConverter userConverter = mock(UserConverter.class);

        given(authService.refreshToken("valid-refresh-token")).willReturn("new-access-token");

        MockMvc mockMvc = createMvc(authService, jwtProperties, userConverter);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(RefreshTokenRequest.builder().refreshToken("valid-refresh-token").build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A0000"))
                .andExpect(jsonPath("$.data").value("new-access-token"));
    }

    @Test
    void forgotPassword_withValidRequest_returnsOk() throws Exception {
        AuthService authService = mock(AuthService.class);
        JwtProperties jwtProperties = mock(JwtProperties.class);
        UserConverter userConverter = mock(UserConverter.class);

        given(authService.forgotPassword(any(ForgotPasswordBo.class))).willReturn(1L);

        MockMvc mockMvc = createMvc(authService, jwtProperties, userConverter);

        mockMvc.perform(post("/api/auth/password-reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"13800138000\",\"newPassword\":\"NewTest123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A0000"));
    }
}