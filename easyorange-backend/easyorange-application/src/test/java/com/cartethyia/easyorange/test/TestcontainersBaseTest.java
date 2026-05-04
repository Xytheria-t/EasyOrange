package com.cartethyia.easyorange.test;

import org.junit.jupiter.api.DisplayName;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@DisplayName("Testcontainers MySQL 配置基类")
public abstract class TestcontainersBaseTest {

    private static final String MYSQL_IMAGE = "mysql:8.0";

    @Container
    protected static final MySQLContainer<?> mysqlContainer = new MySQLContainer<>(
            DockerImageName.parse(MYSQL_IMAGE)
                    .asCompatibleSubstituteFor("mysql")
    )
            .withDatabaseName("easyorange_test")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);

    static {
        mysqlContainer.start();
        System.setProperty("spring.datasource.url", mysqlContainer.getJdbcUrl());
        System.setProperty("spring.datasource.username", mysqlContainer.getUsername());
        System.setProperty("spring.datasource.password", mysqlContainer.getPassword());
        System.setProperty("spring.datasource.driver-class-name", "com.mysql.cj.jdbc.Driver");
    }
}
