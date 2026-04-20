package com.cartethyia.easyorange.user.assembler;

import com.cartethyia.easyorange.user.dto.response.LoginResponse;
import com.cartethyia.easyorange.user.dto.vo.UserVO;
import com.cartethyia.easyorange.user.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UserAssembler {

    public UserVO toUserVO(User user) {
        if (user == null) {
            return null;
        }
        return UserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .phone(user.getPhone())
                .studentId(user.getStudentId())
                .realName(user.getRealName())
                .status(parseStatus(user.getStatus()))
                .createTime(user.getCreateTime())
                .updateTime(user.getUpdateTime())
                .build();
    }

    public LoginResponse.UserInfo toLoginUserInfo(User user) {
        if (user == null) {
            return null;
        }
        return LoginResponse.UserInfo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .phone(user.getPhone())
                .build();
    }

    private Integer parseStatus(String status) {
        if (StringUtils.isBlank(status)) {
            return 0;
        }
        try {
            return Integer.parseInt(status);
        } catch (NumberFormatException e) {
            log.warn("Invalid user status value: {}", status);
            return 0;
        }
    }
}
