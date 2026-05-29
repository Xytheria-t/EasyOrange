package com.cartethyia.easyorange.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "easyorange.ai")
public class AiProperties {

    private DeepSeek deepseek = new DeepSeek();
    private QwenVl qwenVl = new QwenVl();

    @Data
    public static class DeepSeek {
        private String apiKey;
        private String baseUrl = "https://api.deepseek.com";
        private String model = "deepseek-chat";
        private int timeout = 30000;
    }

    @Data
    public static class QwenVl {
        private String apiKey;
        private String baseUrl = "https://dashscope.aliyuncs.com/api/v1";
        private String model = "qwen-vl-max";
        private int timeout = 60000;
    }
}