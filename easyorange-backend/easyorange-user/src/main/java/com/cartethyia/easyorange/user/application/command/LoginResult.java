package com.cartethyia.easyorange.user.application.command;

import com.cartethyia.easyorange.user.application.dto.UserVO;

public record LoginResult(
    String token,
    String refreshToken,
    UserVO user
) {}
