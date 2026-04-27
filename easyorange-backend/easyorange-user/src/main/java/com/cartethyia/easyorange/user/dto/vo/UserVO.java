package com.cartethyia.easyorange.user.dto.vo;

import com.cartethyia.easyorange.common.util.MaskUtils;
import com.cartethyia.easyorange.user.entity.User;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


/**
 * 用户信息视图对象
 * @author cartethyia
 */
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserVO {

    private Long id;

    private String username;

    private String email;

    private String phone;

    private String studentId;

    private String realName;

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
                .id(user.getId())
                .username(user.getUsername())
                .email(MaskUtils.maskEmail(user.getEmail()))
                .phone(MaskUtils.maskPhone(user.getPhone()))
                .studentId(user.getStudentId())
                .realName(MaskUtils.maskName(user.getRealName()))
                .status(user.getStatus() != null
                        ? Integer.parseInt(user.getStatus().getCode()) : 0)
                .createTime(user.getCreateTime())
                .updateTime(user.getUpdateTime())
                .build();
    }
}


