package com.cartethyia.easyorange.ai.prompt;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("YamlPromptRegistry 测试")
class YamlPromptRegistryTest {

    @TempDir
    Path tempDir;

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
    @DisplayName("不存在的模板名返回 Optional.empty()")
    void get_nonExistentName_returnsEmpty() throws IOException {
        writeYaml("greeting.yml", """
                name: greeting
                version: v1.0.0
                description: test
                template: hello
                """);

        var registry = new YamlPromptRegistry(tempDir);

        assertThat(registry.getLatest("nonexistent")).isEmpty();
    }

    @Test
    @DisplayName("空目录不报错，查询返回空")
    void emptyDirectory_queriesReturnEmpty() {
        var registry = new YamlPromptRegistry(tempDir);

        assertThat(registry.getLatest("any")).isEmpty();
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
