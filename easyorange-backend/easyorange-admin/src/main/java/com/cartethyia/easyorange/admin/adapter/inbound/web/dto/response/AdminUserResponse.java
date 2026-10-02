package com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record AdminUserResponse(
        String userId,
        String username,
        String nickname,
        String avatar,
        String email,
        String phone,
        String realName,
        String userType,
        String userTypeDesc,
        String status,
        String statusDesc,
        String loginIp,
        LocalDateTime loginDate,
        LocalDateTime createTime,
        LocalDateTime updateTime) {}
