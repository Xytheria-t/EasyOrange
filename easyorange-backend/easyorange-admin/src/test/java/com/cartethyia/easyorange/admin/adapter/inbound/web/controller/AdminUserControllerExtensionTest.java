package com.cartethyia.easyorange.admin.adapter.inbound.web.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cartethyia.easyorange.admin.adapter.inbound.web.assembler.AdminUserAssembler;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.ResetPasswordResponse;
import com.cartethyia.easyorange.admin.application.service.AdminUserAppService;
import com.cartethyia.easyorange.admin.application.service.AdminUserSecurityAppService;
import com.cartethyia.easyorange.common.security.AuthUser;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminUserController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminUserControllerExtensionTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminUserAppService adminUserService;

    @MockitoBean
    private AdminUserSecurityAppService adminUserSecurityService;

    @MockitoBean
    private AdminUserAssembler assembler;

    /**
     * 管理端写端点从 {@code @AuthenticationPrincipal} 取操作人；无 SecurityContext 时
     * 解析出 null 会让「谁改的」这条审计链在测试里断掉，故显式造一个已认证主体。
     */
    @BeforeEach
    void authenticate() {
        var operator = new AuthUser("admin-1", "admin");
        var authentication = new UsernamePasswordAuthenticationToken(
                operator, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    void unlockUser_shouldSucceed() throws Exception {
        doNothing().when(adminUserSecurityService).unlockUser("1", "admin-1");

        mockMvc.perform(put("/api/admin/users/1/unlock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A0000"));
    }

    @Test
    void resetPassword_shouldReturnNewPassword() throws Exception {
        when(adminUserSecurityService.resetPassword(eq("1"), any(), eq("admin-1")))
                .thenReturn("newPass123!");
        // 响应形状由 assembler 产出，这里 stub 其返回以锁住 Controller 的输出契约
        when(assembler.toResetPasswordResponse("newPass123!"))
                .thenReturn(ResetPasswordResponse.builder()
                        .newPassword("newPass123!")
                        .message("密码已重置，请将新密码安全地传递给用户")
                        .build());

        mockMvc.perform(put("/api/admin/users/1/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\": \"用户忘记密码\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A0000"))
                .andExpect(jsonPath("$.data.newPassword").value("newPass123!"))
                .andExpect(jsonPath("$.data.message").value("密码已重置，请将新密码安全地传递给用户"));
    }

    @Test
    void resetPassword_withoutReason_shouldReturn400() throws Exception {
        mockMvc.perform(put("/api/admin/users/1/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void forceLogout_shouldSucceed() throws Exception {
        doNothing().when(adminUserSecurityService).forceLogout("1");

        mockMvc.perform(put("/api/admin/users/1/force-logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A0000"));

        verify(adminUserSecurityService).forceLogout("1");
    }

    @Test
    void changeUserRole_shouldSucceed() throws Exception {
        doNothing().when(adminUserSecurityService).changeUserRole(eq("1"), any(), any(), eq("admin-1"));

        mockMvc.perform(put("/api/admin/users/1/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\": \"02\", \"reason\": \"晋升管理员\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A0000"));
    }

    @Test
    void changeUserRole_withoutRole_shouldReturn400() throws Exception {
        mockMvc.perform(put("/api/admin/users/1/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\": \"晋升管理员\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changeUserRole_withoutReason_shouldReturn400() throws Exception {
        mockMvc.perform(put("/api/admin/users/1/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\": \"02\"}"))
                .andExpect(status().isBadRequest());
    }
}
