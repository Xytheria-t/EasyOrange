package com.cartethyia.easyorange.framework.messaging.reliability;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ConfirmCallback implements RabbitTemplate.ConfirmCallback {

    @Override
    public void confirm(CorrelationData correlationData, boolean ack, String cause) {
        if (correlationData == null) {
            log.warn("Received confirm with null correlation data");
            return;
        }

        String messageId = correlationData.getId();

        if (ack) {
            log.debug("Message confirmed: {}", messageId);
        } else {
            log.error("Message failed: {}, cause: {}", messageId, cause);
        }
    }
}
