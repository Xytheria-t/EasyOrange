package com.cartethyia.easyorange.message.adapter.inbound.websocket;

import com.cartethyia.easyorange.message.domain.constant.MessageConstant;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor webSocketAuthInterceptor;
    private final ThreadPoolTaskScheduler taskScheduler;

    // Lombok 构造器不会把 @Qualifier 复制到参数上，手写显式构造器以限定 "taskScheduler"
    public WebSocketConfig(
            WebSocketAuthInterceptor webSocketAuthInterceptor,
            @Qualifier("taskScheduler") ThreadPoolTaskScheduler taskScheduler) {
        this.webSocketAuthInterceptor = webSocketAuthInterceptor;
        this.taskScheduler = taskScheduler;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
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
