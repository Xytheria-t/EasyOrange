package com.cartethyia.easyorange.payment.adapter.outbound.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cartethyia.easyorange.payment.adapter.outbound.persistence.mapper.PaymentConfigMapper;
import com.cartethyia.easyorange.payment.adapter.outbound.persistence.po.PaymentConfigPO;
import com.cartethyia.easyorange.payment.constant.PaymentConstant;
import com.cartethyia.easyorange.payment.adapter.inbound.web.response.PaymentConfigResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class PaymentConfigRepository {

    private final PaymentConfigMapper paymentConfigMapper;

    public PaymentConfigPO getByChannelCode(String channelCode) {
        return paymentConfigMapper.selectOne(new LambdaQueryWrapper<PaymentConfigPO>()
                .eq(PaymentConfigPO::getChannelCode, channelCode)
                .eq(PaymentConfigPO::getStatus, PaymentConstant.CONFIG_STATUS_ENABLED));
    }

    public List<PaymentConfigResponse> getEnabledChannels() {
        return paymentConfigMapper.selectList(new LambdaQueryWrapper<PaymentConfigPO>()
                        .eq(PaymentConfigPO::getStatus, PaymentConstant.CONFIG_STATUS_ENABLED)
                        .select(PaymentConfigPO::getId, PaymentConfigPO::getChannelCode,
                                PaymentConfigPO::getChannelName, PaymentConfigPO::getSandbox,
                                PaymentConfigPO::getStatus))
                .stream()
                .map(config -> PaymentConfigResponse.builder()
                        .id(config.getId())
                        .channelCode(config.getChannelCode())
                        .channelName(config.getChannelName())
                        .sandbox(config.getSandbox())
                        .status(config.getStatus())
                        .build())
                .collect(Collectors.toList());
    }

    public boolean isSandbox(String channelCode) {
        PaymentConfigPO config = getByChannelCode(channelCode);
        return config != null && Boolean.TRUE.equals(config.getSandbox());
    }
}
