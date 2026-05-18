package com.cartethyia.easyorange.admin.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AdminUserResponse {

    private Long userId;

    private String username;

    private String nickname;

    private String avatar;

    private String email;

    private String phone;

    private String studentId;

    private String realName;

    private String userType;

    private String userTypeDesc;

    private String status;

    private String statusDesc;

    private String loginIp;

    private LocalDateTime loginDate;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}