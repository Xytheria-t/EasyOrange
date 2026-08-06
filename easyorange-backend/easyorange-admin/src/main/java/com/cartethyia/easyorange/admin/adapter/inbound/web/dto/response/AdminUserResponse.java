package com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminUserResponse {

    private String userId;

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
