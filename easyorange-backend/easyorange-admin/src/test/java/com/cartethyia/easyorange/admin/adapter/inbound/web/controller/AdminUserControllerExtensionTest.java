package com.cartethyia.easyorange.admin.adapter.inbound.web.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.ResetPasswordResponse;
import com.cartethyia.easyorange.admin.service.AdminUserSecurityService;
import com.cartethyia.easyorange.admin.service.AdminUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminUserController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminUserControllerExtensionTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminUserService adminUserService;

    @MockitoBean
    private AdminUserSecurityService adminUserSecurityService;

    @Test
    void unlockUser_shouldSucceed() throws Exception {
        doNothing().when(adminUserSecurityService).unlockUser("1");

        mockMvc.perform(put("/api/admin/users/1/unlock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A0000"));
    }

    @Test
    void resetPassword_shouldReturnNewPassword() throws Exception {
        var resetResponse = ResetPasswordResponse.builder()
                .newPassword("newPass123!")
                .message("密码已重置")
                .build();
        when(adminUserSecurityService.resetPassword(eq("1"))).thenReturn(resetResponse);

        mockMvc.perform(put("/api/admin/users/1/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\": \"用户忘记密码\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A0000"))
                .andExpect(jsonPath("$.data.newPassword").value("newPass123!"))
                .andExpect(jsonPath("$.data.message").value("密码已重置"));
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
        doNothing().when(adminUserSecurityService).changeUserRole(eq("1"), any());

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
