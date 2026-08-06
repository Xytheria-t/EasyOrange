package com.cartethyia.easyorange.test;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 集成冒烟测试 — 验证完整应用上下文能基于真实 MySQL/Redis/RabbitMQ 容器启动，
 * 且 Flyway 迁移已执行。这是全 Mockito 单测覆盖不到的「装配 + 中间件连通」链路。
 * <p>
 * 无 Docker 环境由 {@link AbstractIntegrationTest} 自动跳过。
 */
class DatabaseConnectivityIT extends AbstractIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void flywayMigrationsAppliedOnStartup() {
        var jdbc = new JdbcTemplate(dataSource);
        var applied = jdbc.queryForObject("SELECT COUNT(*) FROM flyway_schema_history", Integer.class);
        assertThat(applied).isGreaterThan(0);
    }

    @Test
    void outboxTableProvisionedByMigrations() {
        var jdbc = new JdbcTemplate(dataSource);
        var tableCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'easyorange' AND table_name = 'EVENT_PUBLICATION'",
                Integer.class);
        assertThat(tableCount).isEqualTo(1);
    }
}
