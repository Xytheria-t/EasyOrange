package com.cartethyia.easyorange.user.service.impl;

import com.cartethyia.easyorange.common.util.BizRequire;
import com.cartethyia.easyorange.common.util.RequestUtil;
import com.cartethyia.easyorange.framework.service.TokenService;
import com.cartethyia.easyorange.user.dto.request.LoginDTO;
import com.cartethyia.easyorange.user.dto.response.LoginResponse;
import com.cartethyia.easyorange.user.entity.User;
import com.cartethyia.easyorange.user.dto.response.WechatSessionResponse;
import com.cartethyia.easyorange.user.enums.AccountType;
import com.cartethyia.easyorange.user.enums.ClientType;
import com.cartethyia.easyorange.user.enums.UserStatus;
import com.cartethyia.easyorange.user.enums.UserType;
import com.cartethyia.easyorange.user.service.LoginStrategyService;
import com.cartethyia.easyorange.user.service.UserService;
import com.cartethyia.easyorange.user.service.WechatLoginService;
import com.cartethyia.easyorange.user.util.NicknameGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class MiniLoginServiceImpl implements LoginStrategyService {

    private final UserService userService;
    private final WechatLoginService wechatLoginService;
    private final TokenService tokenService;
    private final NicknameGenerator nicknameGenerator;

    @Override
    public LoginResponse login(LoginDTO loginDTO) {
        String wxCode = loginDTO.getWxCode();
        BizRequire.notBlank(wxCode, "微信登录 code 不能为空");

        WechatSessionResponse sessionResponse = wechatLoginService.jsCode2Session(wxCode);

        String openid = sessionResponse.getOpenid();
        User user = userService.lambdaQuery().eq(User::getOpenid, openid).one();

        if (user == null) {
            String username = nicknameGenerator.generate();

            user = User.builder()
                .openid(openid)
                .unionid(sessionResponse.getUnionid())
                .username(username)
                .loginType(AccountType.WECHAT.getCode())
                .userType(UserType.NORMAL.getCode())
                .status(UserStatus.NORMAL.getCode())
                .delFlag(0)
                .loginDate(LocalDateTime.now())
                .loginIp(RequestUtil.getClientIp())
                .build();
        } else {
            user.setLoginDate(LocalDateTime.now());
            user.setLoginIp(RequestUtil.getClientIp());
            if (AccountType.WEB.getCode().equals(user.getLoginType())) {
                user.setLoginType(AccountType.BOTH.getCode());
            }
        }

        boolean isNewUser = (user.getId() == null);
        userService.saveOrUpdate(user);

        String token = tokenService.createToken(user.getId(), user.getUsername());

        log.info("action=login_success account={} clientType={} userId={} isNewUser={}",
                user.getUsername(), "MINI", user.getId(), isNewUser);

        return LoginResponse.builder()
            .token(token)
            .user(LoginResponse.UserInfo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .phone(user.getPhone())
                .build())
            .build();
    }

    @Override
    public String[] supportedClientTypes() {
        return new String[]{ClientType.MINI.getValue()};
    }
}
