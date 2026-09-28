package com.cartethyia.easyorange.admin.adapter.inbound.web.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cartethyia.easyorange.admin.adapter.inbound.web.assembler.AdminUserAssembler;
import com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response.AdminUserResponse;
import com.cartethyia.easyorange.admin.application.service.AdminUserAppService;
import com.cartethyia.easyorange.admin.application.service.AdminUserSecurityAppService;
import com.cartethyia.easyorange.admin.domain.port.AdminUserPort.UserDetail;
import com.cartethyia.easyorange.admin.domain.port.AdminUserPort.UserQueryResult;
import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.common.security.AuthUser;
import java.time.LocalDateTime;
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
class AdminUserControllerTest {

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
    void listUsers_shouldReturnPaginatedUsers() throws Exception {
        var users = List.of(
                AdminUserResponse.builder()
                        .userId("1")
                        .username("alice")
                        .status("NORMAL")
                        .statusDesc("正常")
                        .build(),
                AdminUserResponse.builder()
                        .userId("2")
                        .username("bob")
                        .status("NORMAL")
                        .statusDesc("正常")
                        .build());
        var pageResult = PageResult.of(users, 2L, 1, 20);
        when(adminUserService.listUsers(any())).thenReturn(new UserQueryResult(List.of(), 2, 1, 20));
        when(assembler.toPageResponse(any())).thenReturn(pageResult);

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A0000"))
                .andExpect(jsonPath("$.data.records.length()").value(2))
                .andExpect(jsonPath("$.data.records[0].userId").value("1"))
                .andExpect(jsonPath("$.data.records[0].username").value("alice"))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.current").value(1))
                .andExpect(jsonPath("$.data.size").value(20));
    }

    @Test
    void listUsers_withKeyword_shouldFilterByKeyword() throws Exception {
        var users = List.of(
                AdminUserResponse.builder().userId("1").username("alice").build());
        var pageResult = PageResult.of(users, 1L, 1, 20);
        when(adminUserService.listUsers(any())).thenReturn(new UserQueryResult(List.of(), 1, 1, 20));
        when(assembler.toPageResponse(any())).thenReturn(pageResult);

        mockMvc.perform(get("/api/admin/users?keyword=alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A0000"))
                .andExpect(jsonPath("$.data.records[0].username").value("alice"));
    }

    @Test
    void getUserDetail_shouldReturnUser() throws Exception {
        var user = AdminUserResponse.builder()
                .userId("1")
                .username("alice")
                .nickname("Alice")
                .email("alice@test.com")
                .status("NORMAL")
                .statusDesc("正常")
                .createTime(LocalDateTime.of(2026, 1, 1, 0, 0))
                .build();
        var detail = new UserDetail(
                "1",
                "alice",
                "Alice",
                null,
                "alice@test.com",
                null,
                null,
                "01",
                "普通用户",
                "NORMAL",
                "正常",
                null,
                null,
                LocalDateTime.of(2026, 1, 1, 0, 0),
                null);
        when(adminUserService.getUserDetail("1")).thenReturn(detail);
        when(assembler.toResponse(detail)).thenReturn(user);

        mockMvc.perform(get("/api/admin/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A0000"))
                .andExpect(jsonPath("$.data.userId").value("1"))
                .andExpect(jsonPath("$.data.username").value("alice"))
                .andExpect(jsonPath("$.data.email").value("alice@test.com"));
    }

    @Test
    void updateUserStatus_shouldSucceed() throws Exception {
        doNothing().when(adminUserService).updateUserStatus(eq("1"), any(), any(), eq("admin-1"));

        mockMvc.perform(put("/api/admin/users/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"" + "NORMAL" + "\", \"reason\": \"启用用户\"}"))
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
