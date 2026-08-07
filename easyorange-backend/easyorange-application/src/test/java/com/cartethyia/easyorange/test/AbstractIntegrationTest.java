package com.cartethyia.easyorange.test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 集成测试基类 — 复用项目 docker compose 的 dev 基础设施（MySQL / Redis / RabbitMQ）。
 * <p>
 * 基础设施由 Spring Boot 的 {@code spring-boot-docker-compose} 按仓库根 {@code compose.yaml}
 * （{@code lifecycle-management: start-only}）保证处于运行态；应用连接使用 {@code application-it.yaml}
 * 中的显式 localhost 属性（compose 把容器端口发布到宿主机，凭据与 compose.yaml 服务一致）。
 * <p>
 * 采用 Boot docker-compose 而非 Testcontainers：Testcontainers 依赖 ryuk sidecar 镜像，在无代理的
 * Docker 环境拉取失败会导致 {@code @Testcontainers(disabledWithoutDocker=true)} 静默跳过，掩盖真实装配缺陷。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("it")
public abstract class AbstractIntegrationTest {}