package com.cartethyia.easyorange.message.adapter.inbound.websocket;

import com.cartethyia.easyorange.message.domain.constant.MessageConstant;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor webSocketAuthInterceptor;
    private final ThreadPoolTaskScheduler taskScheduler;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 显式注册 /app：不设时应用目标前缀为空，客户端发往 /app/chat.send 会被静默丢弃
        registry.setApplicationDestinationPrefixes("/app");
        registry.enableSimpleBroker(MessageConstant.WS_TOPIC_PREFIX, MessageConstant.WS_QUEUE_PREFIX)
                .setTaskScheduler(taskScheduler);
        registry.setUserDestinationPrefix(MessageConstant.WS_USER_PREFIX);
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint(MessageConstant.WS_ENDPOINT)
                .addInterceptors(webSocketAuthInterceptor)
                .setHandshakeHandler(new AuthHandshakeHandler())
                .setAllowedOriginPatterns("*");
    }
}
