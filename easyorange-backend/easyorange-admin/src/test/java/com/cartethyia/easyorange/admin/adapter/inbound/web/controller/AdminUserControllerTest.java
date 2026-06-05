package com.cartethyia.easyorange.admin.adapter.inbound.web.controller;

import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.AdminUserResponse;
import com.cartethyia.easyorange.admin.service.AdminUserService;
import com.cartethyia.easyorange.common.result.PageResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminUserController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminUserService adminUserService;

    @Test
    void listUsers_shouldReturnPaginatedUsers() throws Exception {
        var users = List.of(
            AdminUserResponse.builder().userId(1L).username("alice").status("1").statusDesc("正常").build(),
            AdminUserResponse.builder().userId(2L).username("bob").status("1").statusDesc("正常").build()
        );
        var pageResult = PageResult.of(users, 2L, 1, 20);
        when(adminUserService.listUsers(any())).thenReturn(pageResult);

        mockMvc.perform(get("/api/admin/users"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("A0000"))
            .andExpect(jsonPath("$.data.records.length()").value(2))
            .andExpect(jsonPath("$.data.records[0].userId").value(1))
            .andExpect(jsonPath("$.data.records[0].username").value("alice"))
            .andExpect(jsonPath("$.data.total").value(2))
            .andExpect(jsonPath("$.data.current").value(1))
            .andExpect(jsonPath("$.data.size").value(20));
    }

    @Test
    void listUsers_withKeyword_shouldFilterByKeyword() throws Exception {
        var users = List.of(
            AdminUserResponse.builder().userId(1L).username("alice").build()
        );
        var pageResult = PageResult.of(users, 1L, 1, 20);
        when(adminUserService.listUsers(any())).thenReturn(pageResult);

        mockMvc.perform(get("/api/admin/users?keyword=alice"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("A0000"))
            .andExpect(jsonPath("$.data.records[0].username").value("alice"));
    }

    @Test
    void getUserDetail_shouldReturnUser() throws Exception {
        var user = AdminUserResponse.builder()
            .userId(1L).username("alice").nickname("Alice").email("alice@test.com")
            .status("1").statusDesc("正常").createTime(LocalDateTime.of(2026, 1, 1, 0, 0))
            .build();
        when(adminUserService.getUserDetail(1L)).thenReturn(user);

        mockMvc.perform(get("/api/admin/users/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("A0000"))
            .andExpect(jsonPath("$.data.userId").value(1))
            .andExpect(jsonPath("$.data.username").value("alice"))
            .andExpect(jsonPath("$.data.email").value("alice@test.com"));
    }

    @Test
    void updateUserStatus_shouldSucceed() throws Exception {
        doNothing().when(adminUserService).updateUserStatus(1L, null);

        mockMvc.perform(put("/api/admin/users/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\": 1, \"reason\": \"启用用户\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("A0000"));
    }

    @Test
    void updateUserStatus_withoutStatus_shouldReturn400() throws Exception {
        mockMvc.perform(put("/api/admin/users/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\": \"启用用户\"}"))
            .andExpect(status().isBadRequest());
    }
}