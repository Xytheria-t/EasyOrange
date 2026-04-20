package com.cartethyia.easyorange.user.service.impl;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.user.dto.response.WechatSessionResponse;
import com.cartethyia.easyorange.user.service.WechatLoginService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import cn.binarywang.wx.miniapp.api.WxMaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WechatLoginServiceImpl implements WechatLoginService {

    private final WxMaService wxMaService;

    @Override
    public WechatSessionResponse jsCode2Session(String jsCode) {
        try {
            WxMaJscode2SessionResult result = wxMaService.getUserService().getSessionInfo(jsCode);
            return WechatSessionResponse.builder()
                    .openid(result.getOpenid())
                    .sessionKey(result.getSessionKey())
                    .unionid(result.getUnionid())
                    .build();
        } catch (Exception e) {
            log.error("action=wechat_login_failed", e);
            throw BusinessException.of("微信登录失败，请稍后重试");
        }
    }
}