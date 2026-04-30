package com.cartethyia.easyorange.user.dto.vo;

import com.cartethyia.easyorange.common.util.MaskUtils;
import com.cartethyia.easyorange.user.domain.model.User;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserVO {

    private Long userId;

    private String username;

    private String email;

    private String phone;

    private String studentId;

    private String realName;

    private String avatar;

    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;

    public static UserVO from(User user) {
        if (user == null) {
            return null;
        }
        return builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(MaskUtils.maskEmail(user.getEmail()))
                .phone(MaskUtils.maskPhone(user.getPhone()))
                .studentId(user.getStudentId())
                .realName(MaskUtils.maskName(user.getRealName()))
                .avatar(user.getAvatar())
                .status(user.getStatus() != null
                        ? Integer.parseInt(user.getStatus().getCode()) : 0)
                .createTime(user.getCreateTime())
                .updateTime(user.getUpdateTime())
                .build();
    }
}
