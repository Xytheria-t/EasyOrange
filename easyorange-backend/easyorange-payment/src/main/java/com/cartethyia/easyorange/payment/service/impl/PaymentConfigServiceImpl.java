package com.cartethyia.easyorange.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cartethyia.easyorange.payment.constant.PaymentConstants;
import com.cartethyia.easyorange.payment.dto.vo.PaymentConfigVO;
import com.cartethyia.easyorange.payment.entity.PaymentConfig;
import com.cartethyia.easyorange.payment.mapper.PaymentConfigMapper;
import com.cartethyia.easyorange.payment.service.PaymentConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentConfigServiceImpl extends ServiceImpl<PaymentConfigMapper, PaymentConfig> implements PaymentConfigService {

    @Override
    public PaymentConfig getByChannelCode(String channelCode) {
        return getOne(new LambdaQueryWrapper<PaymentConfig>()
                .eq(PaymentConfig::getChannelCode, channelCode)
                .eq(PaymentConfig::getStatus, PaymentConstants.CONFIG_STATUS_ENABLED));
    }

    @Override
    public List<PaymentConfigVO> getEnabledChannels() {
        return list(new LambdaQueryWrapper<PaymentConfig>()
                        .eq(PaymentConfig::getStatus, PaymentConstants.CONFIG_STATUS_ENABLED)
                        .select(PaymentConfig::getId, PaymentConfig::getChannelCode,
                                PaymentConfig::getChannelName, PaymentConfig::getSandbox,
                                PaymentConfig::getStatus))
                .stream()
                .map(config -> PaymentConfigVO.builder()
                        .id(config.getId())
                        .channelCode(config.getChannelCode())
                        .channelName(config.getChannelName())
                        .sandbox(config.getSandbox())
                        .status(config.getStatus())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public boolean isSandbox(String channelCode) {
        PaymentConfig config = getByChannelCode(channelCode);
        return config != null && Boolean.TRUE.equals(config.getSandbox());
    }
}
