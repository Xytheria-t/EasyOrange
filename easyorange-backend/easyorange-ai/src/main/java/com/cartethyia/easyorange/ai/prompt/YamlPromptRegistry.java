package com.cartethyia.easyorange.ai.prompt;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

/**
 * 基于 YAML 文件的 Prompt 模板注册中心。
 * <p>
 * 生产环境从 classpath:prompts/*.yml 加载所有模板；
 * 测试环境可通过 {@link #YamlPromptRegistry(Path)} 指定文件系统目录。
 * <p>
 * 启动时打印已加载的 Prompt 数量和版本号。
 */
@Slf4j
@Component
public class YamlPromptRegistry implements PromptRegistry {

    private static final String CLASSPATH_PATTERN = "classpath:prompts/*.yml";

    private final Path directory;
    private final boolean classpathMode;

    private Map<String, List<PromptTemplate>> templatesByName = Map.of();

    /**
     * 生产构造器 — 从 classpath:prompts/*.yml 加载。
     */
    public YamlPromptRegistry() {
        this.directory = null;
        this.classpathMode = true;
    }

    /**
     * 测试构造器 — 从指定文件系统目录加载。
     *
     * @param directory 包含 .yml 文件的目录
     */
    public YamlPromptRegistry(Path directory) {
        this.directory = directory;
        this.classpathMode = false;
        load();
    }

    @PostConstruct
    void init() {
        if (classpathMode) {
            load();
        }
    }

    private void load() {
        var yaml = new Yaml();
        var loaded = new HashMap<String, List<PromptTemplate>>();

        try (Stream<Path> files = classpathMode ? Stream.empty() : Files.list(directory)) {
            if (classpathMode) {
                loadFromClasspath(yaml, loaded);
            } else {
                loadFromDirectory(yaml, loaded, files);
            }
        } catch (IOException e) {
            log.error("加载 Prompt 模板失败", e);
        }

        var immutable = new HashMap<String, List<PromptTemplate>>();
        loaded.forEach((name, list) -> immutable.put(name, List.copyOf(list)));
        this.templatesByName = Map.copyOf(immutable);

        logTemplates();
    }

    @SuppressWarnings("unchecked")
    private void loadFromClasspath(Yaml yaml, Map<String, List<PromptTemplate>> loaded) throws IOException {
        var resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources(CLASSPATH_PATTERN);
        for (var resource : resources) {
            try (InputStream is = resource.getInputStream()) {
                var data = yaml.loadAs(is, Map.class);
                addTemplate(loaded, parseTemplate(data));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void loadFromDirectory(Yaml yaml, Map<String, List<PromptTemplate>> loaded, Stream<Path> files)
            throws IOException {
        var yamlFiles = files.filter(
                        p -> p.toString().endsWith(".yml") || p.toString().endsWith(".yaml"))
                .toList();
        for (var file : yamlFiles) {
            try (InputStream is = Files.newInputStream(file)) {
                var data = yaml.loadAs(is, Map.class);
                addTemplate(loaded, parseTemplate(data));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private PromptTemplate parseTemplate(Map<String, Object> data) {
        if (data == null) {
            throw new IllegalArgumentException("YAML 文件内容为空");
        }
        return new PromptTemplate(
                requireString(data, "name"),
                requireString(data, "version"),
                requireString(data, "template"),
                getString(data, "description", ""));
    }

    private String requireString(Map<String, Object> data, String key) {
        var value = data.get(key);
        if (value == null) {
            throw new IllegalArgumentException("YAML 缺少必需字段: " + key);
        }
        return String.valueOf(value);
    }

    private String getString(Map<String, Object> data, String key, String defaultValue) {
        var value = data.get(key);
        return value != null ? String.valueOf(value) : defaultValue;
    }

    private void addTemplate(Map<String, List<PromptTemplate>> loaded, PromptTemplate template) {
        loaded.computeIfAbsent(template.name(), k -> new ArrayList<>()).add(template);
    }

    private void logTemplates() {
        int total = templatesByName.values().stream().mapToInt(List::size).sum();
        if (total == 0) {
            log.info("未加载到任何 Prompt 模板");
            return;
        }
        var details = new StringJoiner(", ");
        templatesByName.forEach((name, list) ->
                details.add(name + list.stream().map(PromptTemplate::version).toList()));
        log.info("已加载 {} 个 Prompt 模板: {}", total, details);
    }

    @Override
    public Optional<PromptTemplate> getLatest(String name) {
        return templatesByName.getOrDefault(name, List.of()).stream()
                .max(Comparator.comparing(PromptTemplate::version, YamlPromptRegistry::compareVersions));
    }

    /**
     * 语义化版本比较 — 解析 "v1.2.3" 格式，按 major.minor.patch 比较。
     */
    private static int compareVersions(String v1, String v2) {
        var parts1 = parseVersion(v1);
        var parts2 = parseVersion(v2);
        for (int i = 0; i < 3; i++) {
            int diff = Integer.compare(parts1[i], parts2[i]);
            if (diff != 0) {
                return diff;
            }
        }
        return 0;
    }

    private static int[] parseVersion(String version) {
        var stripped = version.startsWith("v") ? version.substring(1) : version;
        var parts = stripped.split("\\.");
        var result = new int[] {0, 0, 0};
        for (int i = 0; i < Math.min(parts.length, 3); i++) {
            try {
                result[i] = Integer.parseInt(parts[i]);
            } catch (NumberFormatException ignored) {
            }
        }
        return result;
    }
}
