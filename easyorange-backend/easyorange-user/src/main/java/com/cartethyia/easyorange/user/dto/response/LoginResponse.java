package com.cartethyia.easyorange.user.dto.response;

import com.cartethyia.easyorange.user.dto.vo.UserVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private String token;

    private UserVO user;
}
