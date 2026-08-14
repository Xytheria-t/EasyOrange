package com.cartethyia.easyorange.framework.messaging.nats;

import io.nats.client.Connection;
import io.nats.client.JetStreamManagement;
import io.nats.client.Nats;
import io.nats.client.api.StreamConfiguration;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * NATS 连接装配 — 仅在 {@code easyorange.messaging.nats.enabled=true} 时创建连接。
 */
@Configuration
@EnableConfigurationProperties(NatsProperties.class)
@RequiredArgsConstructor
public class NatsConfig {

    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(name = "easyorange.messaging.nats.enabled", havingValue = "true")
    public Connection natsConnection(NatsProperties properties) throws IOException, InterruptedException {
        return Nats.connect(properties.getUrl());
    }

    /**
     * JetStream 管理句柄 — 启动时确保流存在（重复执行幂等）。
     */
    @Bean
    @ConditionalOnProperty(name = "easyorange.messaging.nats.enabled", havingValue = "true")
    public JetStreamManagement natsJetStreamManagement(Connection connection, NatsProperties properties) {
        try {
            JetStreamManagement jsm = connection.jetStreamManagement();
            jsm.addStream(StreamConfiguration.builder()
                    .name(properties.getStream())
                    .subjects(properties.getSubjectPrefix() + ">")
                    .build());
            return jsm;
        } catch (Exception e) {
            throw new IllegalStateException("NATS JetStream stream setup failed: " + properties.getStream(), e);
        }
    }
}
