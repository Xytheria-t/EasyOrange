package com.cartethyia.easyorange.payment.adapter.outbound.persistence;

import com.cartethyia.easyorange.common.repository.BaseRepository;
import com.cartethyia.easyorange.payment.adapter.outbound.persistence.mapper.PaymentConfigMapper;
import com.cartethyia.easyorange.payment.adapter.outbound.persistence.PaymentConfigDO;
import com.cartethyia.easyorange.payment.domain.constant.PaymentConstant;
import com.cartethyia.easyorange.payment.adapter.inbound.web.response.PaymentConfigResponse;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Repository
public class PaymentConfigRepository extends BaseRepository<PaymentConfigMapper, PaymentConfigDO> {

    public PaymentConfigRepository(PaymentConfigMapper paymentConfigMapper) {
        super(paymentConfigMapper);
    }

    public PaymentConfigDO getByChannelCode(String channelCode) {
        return lambdaQuery()
                .eq(PaymentConfigDO::getChannelCode, channelCode)
                .eq(PaymentConfigDO::getStatus, PaymentConstant.CONFIG_STATUS_ENABLED)
                .one();
    }

    public List<PaymentConfigResponse> getEnabledChannels() {
        return lambdaQuery()
                        .eq(PaymentConfigDO::getStatus, PaymentConstant.CONFIG_STATUS_ENABLED)
                        .select(PaymentConfigDO::getId, PaymentConfigDO::getChannelCode,
                                PaymentConfigDO::getChannelName, PaymentConfigDO::getSandbox,
                                PaymentConfigDO::getStatus)
                        .list()
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
        PaymentConfigDO config = getByChannelCode(channelCode);
        return config != null && Boolean.TRUE.equals(config.getSandbox());
    }
}