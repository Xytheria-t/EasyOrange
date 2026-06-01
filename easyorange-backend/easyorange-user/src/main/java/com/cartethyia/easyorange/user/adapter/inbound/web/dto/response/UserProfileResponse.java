package com.cartethyia.easyorange.user.adapter.inbound.web.dto.response;

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
public class UserProfileResponse implements CommonUserFields {

    private Long userId;

    private String username;

    private String nickname;

    private String email;

    private String phone;

    private String studentId;

    private String realName;

    private Integer status;

    private String statusDesc;

    private Integer gender;

    private UserType userType;

    private String avatar;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;

    private Set<String> roles;

    private Set<String> permissions;

    private Long loginTime;
}
