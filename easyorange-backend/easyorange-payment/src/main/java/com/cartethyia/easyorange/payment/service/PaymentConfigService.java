package com.cartethyia.easyorange.payment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cartethyia.easyorange.payment.dto.vo.PaymentConfigVO;
import com.cartethyia.easyorange.payment.entity.PaymentConfig;

import java.util.List;

public interface PaymentConfigService extends IService<PaymentConfig> {

    PaymentConfig getByChannelCode(String channelCode);

    List<PaymentConfigVO> getEnabledChannels();

    boolean isSandbox(String channelCode);
}
