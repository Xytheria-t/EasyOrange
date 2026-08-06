package com.cartethyia.easyorange.test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TestDataLoader {

    private static final String TEST_DATA_PATH = "db/test";
    private static final String CLEANUP_SCRIPT = "00_cleanup.sql";

    private final DataSource dataSource;

    public TestDataLoader(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void loadAllTestData() {
        log.info("开始加载所有测试数据...");
        List<String> scripts = getTestDataScripts();
        executeScripts(scripts);
        log.info("测试数据加载完成，共执行 {} 个脚本", scripts.size());
    }

    public void loadTestData(String... scriptNames) {
        log.info("开始加载指定测试数据: {}", String.join(", ", scriptNames));
        List<String> scripts = new ArrayList<>();
        for (String name : scriptNames) {
            scripts.add(TEST_DATA_PATH + "/" + name);
        }
        executeScripts(scripts);
        log.info("测试数据加载完成，共执行 {} 个脚本", scripts.size());
    }

    public void loadModuleTestData(String module) {
        log.info("开始加载模块测试数据: {}", module);
        List<String> scripts = getTestDataScripts().stream()
                .filter(s -> s.contains("_" + module + "_") || s.endsWith("_" + module + ".sql"))
                .collect(Collectors.toList());
        executeScripts(scripts);
        log.info("模块测试数据加载完成，共执行 {} 个脚本", scripts.size());
    }

    public void cleanupTestData() {
        log.info("开始清理测试数据...");
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new ClassPathResource(TEST_DATA_PATH + "/" + CLEANUP_SCRIPT));
        populator.execute(dataSource);
        log.info("测试数据清理完成");
    }

    public void executeScript(String scriptPath) {
        log.debug("执行脚本: {}", scriptPath);
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new ClassPathResource(scriptPath));
        populator.execute(dataSource);
    }

    private List<String> getTestDataScripts() {
        List<String> scripts = new ArrayList<>();
        try {
            ClassPathResource resource = new ClassPathResource(TEST_DATA_PATH);
            if (resource.exists()) {
                Path path = Paths.get(resource.getURL().toURI());
                try (Stream<Path> stream = Files.list(path)) {
                    scripts = stream.filter(Files::isRegularFile)
                            .filter(p -> p.toString().endsWith(".sql"))
                            .filter(p -> !p.getFileName().toString().startsWith("00_"))
                            .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                            .map(p -> TEST_DATA_PATH + "/" + p.getFileName().toString())
                            .collect(Collectors.toList());
                }
            }
        } catch (Exception e) {
            log.warn("获取测试数据脚本列表失败，使用默认顺序", e);
            scripts = getDefaultScriptOrder();
        }
        return scripts;
    }

    private List<String> getDefaultScriptOrder() {
        List<String> scripts = new ArrayList<>();
        scripts.add(TEST_DATA_PATH + "/01_users.sql");
        scripts.add(TEST_DATA_PATH + "/02_categories.sql");
        scripts.add(TEST_DATA_PATH + "/03_products.sql");
        scripts.add(TEST_DATA_PATH + "/04_orders_payments.sql");
        scripts.add(TEST_DATA_PATH + "/05_messages.sql");
        scripts.add(TEST_DATA_PATH + "/06_search_others.sql");
        return scripts;
    }

    private void executeScripts(List<String> scripts) {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        for (String script : scripts) {
            log.debug("添加脚本: {}", script);
            populator.addScript(new ClassPathResource(script));
        }
        populator.execute(dataSource);
    }
}
