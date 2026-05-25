package com.cartethyia.easyorange.admin.adapter.inbound.web.controller;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.common.result.Result;
import com.cartethyia.easyorange.admin.dto.request.AdminUserQueryRequest;
import com.cartethyia.easyorange.admin.dto.request.UpdateStatusRequest;
import com.cartethyia.easyorange.admin.dto.response.AdminUserResponse;
import com.cartethyia.easyorange.admin.service.AdminUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    public Result<PageResult<AdminUserResponse>> listUsers(AdminUserQueryRequest request) {
        return Result.success(adminUserService.listUsers(request));
    }

    @GetMapping("/{id}")
    public Result<AdminUserResponse> getUserDetail(@PathVariable Long id) {
        return Result.success(adminUserService.getUserDetail(id));
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateUserStatus(
        @PathVariable Long id,
        @Valid @RequestBody UpdateStatusRequest request
    ) {
        adminUserService.updateUserStatus(id, request);
        return Result.success();
    }
}