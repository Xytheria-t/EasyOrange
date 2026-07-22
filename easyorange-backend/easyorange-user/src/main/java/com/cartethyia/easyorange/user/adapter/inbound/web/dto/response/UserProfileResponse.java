package com.cartethyia.easyorange.user.adapter.inbound.web.dto.response;

import com.cartethyia.easyorange.common.constant.CommonConstant;
import com.cartethyia.easyorange.user.domain.enums.UserType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class    UserProfileResponse implements CommonUserFields {

    private String userId;

    private String username;

    private String nickname;

    private String email;

    private String phone;

    private String studentId;

    private String realName;

    private String status;

    private String statusDesc;

    private String gender;

    private UserType userType;

    private String avatar;

    @JsonFormat(pattern = CommonConstant.DATETIME_FORMAT, timezone = "GMT+8")
    private LocalDateTime createTime;

    @JsonFormat(pattern = CommonConstant.DATETIME_FORMAT, timezone = "GMT+8")
    private LocalDateTime updateTime;

    private Set<String> roles;

    private Set<String> permissions;

    private Long loginTime;
}
