package com.cartethyia.easyorange.user.dto.vo;

import com.cartethyia.easyorange.common.util.MaskUtils;
import com.cartethyia.easyorange.user.entity.User;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileVO {

    private Long id;

    private String username;

    private String email;

    private String phone;

    private String studentId;

    private String realName;

    private Integer status;

    private String statusDesc;

    private Integer gender;

    private String userType;

    private String avatar;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;

    private Set<String> roles;

    private Set<String> permissions;

    private Long loginTime;

    public static UserProfileVO from(User user, Set<String> roles, Set<String> permissions, Long loginTime) {
        if (user == null) {
            return null;
        }
        return builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(MaskUtils.maskEmail(user.getEmail()))
                .phone(MaskUtils.maskPhone(user.getPhone()))
                .studentId(user.getStudentId())
                .realName(MaskUtils.maskName(user.getRealName()))
                .status(user.getStatus() != null
                        ? Integer.parseInt(user.getStatus().getCode()) : 0)
                .statusDesc(user.getStatus() != null
                        ? user.getStatus().getDescription() : null)
                .gender(user.getSex() != null ? user.getSex().ordinal() : null)
                .userType(user.getUserType() != null
                        ? user.getUserType().getDescription() : null)
                .avatar(user.getAvatar())
                .createTime(user.getCreateTime())
                .updateTime(user.getUpdateTime())
                .roles(roles)
                .permissions(permissions)
                .loginTime(loginTime)
                .build();
    }
}