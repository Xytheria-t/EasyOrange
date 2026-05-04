package com.cartethyia.easyorange.test;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.List;

@SpringBootTest
@ActiveProfiles("integration-test")
@DisplayName("集成测试基类 - 支持 Testcontainers 和测试数据加载")
public abstract class IntegrationTestBase extends TestcontainersBaseTest {

    private static final List<String> TEST_DATA_SCRIPTS = Arrays.asList(
            "db/test/01_users.sql",
            "db/test/02_categories.sql",
            "db/test/03_products.sql",
            "db/test/04_orders_payments.sql",
            "db/test/05_messages.sql",
            "db/test/06_search_others.sql"
    );

    private static final String CLEANUP_SCRIPT = "db/test/00_cleanup.sql";

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected DataSource dataSource;

    @BeforeEach
    @DisplayName("加载测试数据")
    void loadTestData() {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        for (String script : TEST_DATA_SCRIPTS) {
            populator.addScript(new ClassPathResource(script));
        }
        populator.execute(dataSource);
    }

    @AfterEach
    @DisplayName("清理测试数据")
    void cleanupTestData() {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new ClassPathResource(CLEANUP_SCRIPT));
        populator.execute(dataSource);
    }

    protected void executeScript(String scriptPath) {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new ClassPathResource(scriptPath));
        populator.execute(dataSource);
    }

    protected void executeSql(String sql) {
        jdbcTemplate.execute(sql);
    }

    protected long countRows(String tableName) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + tableName,
                Long.class
        );
        return count != null ? count : 0;
    }

    protected boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables " +
                "WHERE table_schema = DATABASE() AND table_name = ?",
                Integer.class,
                tableName
        );
        return count != null && count > 0;
    }
}
