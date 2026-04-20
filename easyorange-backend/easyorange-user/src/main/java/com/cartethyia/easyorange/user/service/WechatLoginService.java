package com.cartethyia.easyorange.user.service;

import com.cartethyia.easyorange.user.dto.response.WechatSessionResponse;

/**
 * 微信登录服务
 *
 * @author cartethyia
 */
public interface WechatLoginService {

    /**
     * 微信登录 code 换取 session
     *
     * @param jsCode 微信登录 code
     * @return 微信 session 响应
     */
    WechatSessionResponse jsCode2Session(String jsCode);
}