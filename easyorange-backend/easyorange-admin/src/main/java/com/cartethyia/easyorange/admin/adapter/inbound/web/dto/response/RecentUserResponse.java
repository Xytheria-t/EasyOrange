package com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RecentUserResponse {

    private String userId;

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
