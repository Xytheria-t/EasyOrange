package com.cartethyia.easyorange.user.adapter.inbound.web.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.cartethyia.easyorange.user.application.dto.UserVO;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private String token;

    private String refreshToken;

    private UserVO user;
}
