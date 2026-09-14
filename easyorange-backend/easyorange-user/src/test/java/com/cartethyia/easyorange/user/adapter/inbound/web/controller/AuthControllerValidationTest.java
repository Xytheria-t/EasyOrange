package com.cartethyia.easyorange.user.adapter.inbound.web.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cartethyia.easyorange.common.enums.ResultCode;
import com.cartethyia.easyorange.framework.config.properties.JwtProperties;
import com.cartethyia.easyorange.framework.exception.GlobalExceptionHandler;
import com.cartethyia.easyorange.framework.web.cookie.RefreshCookie;
import com.cartethyia.easyorange.user.adapter.inbound.web.assembler.UserAssembler;
import com.cartethyia.easyorange.user.application.service.AuthAppService;
import com.cartethyia.easyorange.user.application.service.CredentialAppService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 方法参数级约束（{@code @RequestParam @NotBlank/@Pattern}）的 HTTP 契约回归测试。
 * <p>
 * AuthController 未标注 {@code @Validated}，参数约束由 Spring MVC 内建方法校验执行，抛
 * {@code HandlerMethodValidationException}；该异常必须被 {@link GlobalExceptionHandler} 映射为
 * 400 + B0003，否则会落 500 兜底（前端按 >=500 判可重试，等于把参数错误当系统故障重试）。
 * <p>
 * 切片测试不会自动扫到 framework 包下的 advice，故显式 {@code @Import}。
 */
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@DisplayName("AuthController 参数约束校验测试")
class AuthControllerValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthAppService authAppService;

    @MockitoBean
    private CredentialAppService credentialAppService;

    @MockitoBean
    private UserAssembler userAssembler;

    @MockitoBean
    private RefreshCookie refreshCookie;

    @MockitoBean
    private JwtProperties jwtProperties;

    @Test
    @DisplayName("手机号格式不合法应返回 400 + B0003，而非 500 兜底")
    void sendSmsCode_invalidPhone_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/sms-code").param("phone", "123"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ResultCode.VALIDATE_FAILED.getCode()))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("手机号格式不正确")));
    }

    @Test
    @DisplayName("手机号为空应返回 400 + B0003")
    void sendSmsCode_blankPhone_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/sms-code").param("phone", " "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ResultCode.VALIDATE_FAILED.getCode()));
    }
}
