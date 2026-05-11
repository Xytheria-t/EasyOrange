package com.cartethyia.easyorange.admin.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class RecentUserVO {

    private Long userId;

    private String username;

    private String nickname;

    private String avatar;

    private String email;

    private String phone;

    private String userType;

    private String userTypeDesc;

    private String status;

    private String statusDesc;

    private LocalDateTime createTime;
}
