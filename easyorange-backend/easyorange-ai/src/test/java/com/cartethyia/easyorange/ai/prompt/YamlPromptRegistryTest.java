package com.cartethyia.easyorange.ai.prompt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("YamlPromptRegistry 测试")
class YamlPromptRegistryTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("加载 YAML 文件后按 name+version 精确查找")
    void get_byNameAndVersion_returnsTemplate() throws IOException {
        writeYaml("greeting.yml", """
                name: greeting
                version: v1.0.0
                description: 问候模板
                template: |
                  你好，{name}！
                """);

        var registry = new YamlPromptRegistry(tempDir);

        var result = registry.get("greeting", "v1.0.0");

        assertThat(result).isPresent();
        assertThat(result.get().name()).isEqualTo("greeting");
        assertThat(result.get().version()).isEqualTo("v1.0.0");
        assertThat(result.get().description()).isEqualTo("问候模板");
        assertThat(result.get().template()).contains("{name}");
    }

    @Test
    @DisplayName("getLatest 返回版本号最大的模板")
    void getLatest_multipleVersions_returnsHighestVersion() throws IOException {
        writeYaml("greeting_v1.yml", """
                name: greeting
                version: v1.0.0
                description: v1
                template: v1
                """);
        writeYaml("greeting_v2.yml", """
                name: greeting
                version: v1.2.0
                description: v2
                template: v2
                """);
        writeYaml("greeting_v3.yml", """
                name: greeting
                version: v1.1.0
                description: v1.1
                template: v1.1
                """);

        var registry = new YamlPromptRegistry(tempDir);

        var result = registry.getLatest("greeting");

        assertThat(result).isPresent();
        assertThat(result.get().version()).isEqualTo("v1.2.0");
    }

    @Test
    @DisplayName("listVersions 返回指定模板名的所有版本")
    void listVersions_returnsAllVersions() throws IOException {
        writeYaml("tag_v1.yml", """
                name: tag
                version: v1.0.0
                description: v1
                template: v1
                """);
        writeYaml("tag_v2.yml", """
                name: tag
                version: v2.0.0
                description: v2
                template: v2
                """);
        writeYaml("other.yml", """
                name: other
                version: v1.0.0
                description: other
                template: other
                """);

        var registry = new YamlPromptRegistry(tempDir);

        var versions = registry.listVersions("tag");

        assertThat(versions).hasSize(2);
        assertThat(versions).extracting(PromptTemplate::version)
                .containsExactlyInAnyOrder("v1.0.0", "v2.0.0");
    }

    @Test
    @DisplayName("不存在的模板名返回 Optional.empty()")
    void get_nonExistentName_returnsEmpty() throws IOException {
        writeYaml("greeting.yml", """
                name: greeting
                version: v1.0.0
                description: test
                template: hello
                """);

        var registry = new YamlPromptRegistry(tempDir);

        assertThat(registry.get("nonexistent", "v1.0.0")).isEmpty();
        assertThat(registry.getLatest("nonexistent")).isEmpty();
        assertThat(registry.listVersions("nonexistent")).isEmpty();
    }

    @Test
    @DisplayName("不存在的版本返回 Optional.empty()")
    void get_nonExistentVersion_returnsEmpty() throws IOException {
        writeYaml("greeting.yml", """
                name: greeting
                version: v1.0.0
                description: test
                template: hello
                """);

        var registry = new YamlPromptRegistry(tempDir);

        assertThat(registry.get("greeting", "v9.9.9")).isEmpty();
    }

    @Test
    @DisplayName("空目录不报错，查询返回空")
    void emptyDirectory_queriesReturnEmpty() {
        var registry = new YamlPromptRegistry(tempDir);

        assertThat(registry.get("any", "v1.0.0")).isEmpty();
        assertThat(registry.getLatest("any")).isEmpty();
        assertThat(registry.listVersions("any")).isEmpty();
    }

    @Test
    @DisplayName("跨大版本号比较 — v2.0.0 大于 v1.9.9")
    void getLatest_crossMajorVersion_returnsHighest() throws IOException {
        writeYaml("a.yml", """
                name: tpl
                version: v1.9.9
                description: v1.9.9
                template: old
                """);
        writeYaml("b.yml", """
                name: tpl
                version: v2.0.0
                description: v2.0.0
                template: new
                """);

        var registry = new YamlPromptRegistry(tempDir);

        var result = registry.getLatest("tpl");

        assertThat(result).isPresent();
        assertThat(result.get().version()).isEqualTo("v2.0.0");
    }

    private void writeYaml(String fileName, String content) throws IOException {
        Files.writeString(tempDir.resolve(fileName), content);
    }
}
