package com.cartethyia.easyorange.framework.config.http;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration(proxyBeanMethods = false)
public class RestClientConfig {

    @Bean
    @ConfigurationProperties(prefix = "http-client")
    public HttpClientProperties httpClientProperties() {
        return new HttpClientProperties();
    }

    @Bean
    public HttpClient httpClient(HttpClientProperties properties) {
        return HttpClient.newBuilder()
            .connectTimeout(properties.getConnectTimeout())
            .version(properties.getVersion())
            .build();
    }

    @Bean
    public JdkClientHttpRequestFactory requestFactory(HttpClient httpClient) {
        return new JdkClientHttpRequestFactory(httpClient);
    }

    @Bean
    public RestClient.Builder restClientBuilder(JdkClientHttpRequestFactory requestFactory) {
        return RestClient.builder()
            .requestFactory(requestFactory);
    }

    @Bean
    public RestClient restClient(RestClient.Builder builder) {
        return builder.build();
    }

    @Data
    public static class HttpClientProperties {
        private Duration connectTimeout = Duration.ofSeconds(10);
        private Duration readTimeout = Duration.ofSeconds(30);
        private HttpClient.Version version = HttpClient.Version.HTTP_2;
    }
}
