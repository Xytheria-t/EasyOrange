package com.cartethyia.easyorange.test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * 集成测试基类 — 启动真实 MySQL / Redis / RabbitMQ 容器并注入 Spring 连接。
 * <p>
 * 采用 {@code @Testcontainers(disabledWithoutDocker = true)}：环境无 Docker 时用例自动跳过，
 * 不破坏本地/CI 构建。三个容器都是必需的——Redis 监听容器与 RabbitMQ 监听器在启动期即建立连接，
 * 缺任一都会导致完整应用上下文无法启动。
 * <p>
 * MySQL / RabbitMQ 连接由 {@code @ServiceConnection} 自动绑定；Redis 因官方 Testcontainers 模块
 * 未在项目镜像源提供，改用 {@link GenericContainer} + {@code @DynamicPropertySource} 注入。
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("it")
public abstract class AbstractIntegrationTest {

    @Container
    @ServiceConnection
    static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>(DockerImageName.parse("mysql:8.4")).withDatabaseName("easyorange");

    @Container
    static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine")).withExposedPorts(6379);

    @Container
    @ServiceConnection
    static final RabbitMQContainer RABBITMQ = new RabbitMQContainer(DockerImageName.parse("rabbitmq:3.13-management"));

    @DynamicPropertySource
    static void redisConnection(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> String.valueOf(REDIS.getMappedPort(6379)));
    }
}
