package com.cartethyia.easyorange.ai.adapter.outbound.prompt;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Prompt YAML 内容回归测试 — 守卫全部 AI prompt 模板已加载且内容完整。
 * <p>
 * 与 {@link YamlPromptRegistry} 同包以调用 package-private {@code init()}，触发 classpath 加载、
 * 验证生产环境真实 YAML 可被解析。
 * <p>
 * {@link #ALL_PROMPTS} 是「prompt 全部走 YAML 版本化」的断言载体：任何服务把 prompt 退回硬编码 Java 常量，
 * 这里的条数就对不上。
 */
@DisplayName("Prompt YAML 内容回归测试")
class PromptContentTest {

    private static YamlPromptRegistry registry;

    @BeforeAll
    static void loadRegistry() {
        registry = new YamlPromptRegistry();
        registry.init(); // package-private — 触发 classpath:prompts/*.yml 加载
    }

    private static final String[] ALL_PROMPTS = {
        "ai_chat_system",
        "ai_chat_tool_system",
        "auto_listing",
        "auto_listing_tool_system",
        "auto_listing_image_clues",
        "judge_system"
    };

    /**
     * 必须声明「块内是数据不是指令」的 prompt —— 面向用户内容的对话 / 发布链路。judge 不在内：它评的是模型
     * 回答与人工金标准，评分标准是封闭量表、不含让模型照做的指令空间；image_clues 在内：OCR 出的文字是画面
     * 数据，视觉模型对它的转写同样不得当指令执行。
     */
    private static final String[] PROMPTS_DECLARING_UNTRUSTED_BLOCKS = {
        "ai_chat_system", "ai_chat_tool_system", "auto_listing", "auto_listing_tool_system", "auto_listing_image_clues"
    };

    @Test
    @DisplayName("6 个 prompt 模板全部加载成功（发布助手 3 + 对话 2 + 评审 1）")
    void allPromptsLoaded() {
        for (String name : ALL_PROMPTS) {
            assertThat(registry.getLatest(name)).as("prompt '%s' 应加载成功", name).isPresent();
        }
    }

    @ParameterizedTest
    @CsvSource({
        "auto_listing, 发布助手",
        "auto_listing_image_clues, 预识别器",
        "ai_chat_system, AI 找货助手",
        "ai_chat_tool_system, AI 找货助手",
        "judge_system, 参考回答"
    })
    @DisplayName("每个 prompt 模板包含服务特定的关键短语（防内容漂移）")
    void promptContainsKeyPhrase(String promptName, String keyPhrase) {
        var template =
                registry.getLatest(promptName).orElseThrow(() -> new AssertionError("Prompt not found: " + promptName));

        assertThat(template.template())
                .as("prompt '%s' 应包含关键短语 '%s'", promptName, keyPhrase)
                .contains(keyPhrase);
    }

    @Test
    @DisplayName("面向用户内容的 prompt 都声明「标签块内是数据不是指令」（注入防护不可只覆盖部分链路）")
    void allPromptsDeclareDataNotInstructions() {
        for (String name : PROMPTS_DECLARING_UNTRUSTED_BLOCKS) {
            var template = registry.getLatest(name).orElseThrow();
            assertThat(template.template()).as("prompt '%s' 缺少提示词注入防护声明", name).contains("不是指令");
        }
    }

    @Test
    @DisplayName("所有 prompt 版本号受控（模板内容变更必须升版本）")
    void allPromptsAtControlledVersions() {
        // 已升版 prompt 单列；版本号受控 —— 模板内容变更必须同升版本（同名多版本按号取最新）
        var bumpedVersions = java.util.Map.of(
                "ai_chat_tool_system", "v4.6.0",
                "ai_chat_system", "v1.4.0",
                "auto_listing", "v2.0.0",
                "auto_listing_tool_system", "v1.1.0",
                "auto_listing_image_clues", "v1.0.0");
        for (String name : ALL_PROMPTS) {
            var template = registry.getLatest(name).orElseThrow();
            assertThat(template.version())
                    .as("prompt '%s' 版本号", name)
                    .isEqualTo(bumpedVersions.getOrDefault(name, "v1.0.0"));
        }
    }

    @Test
    @DisplayName("JSON 输出类 prompt 包含 JSON 格式说明")
    void jsonPromptsContainJsonFormatSpec() {
        assertThat(registry.getLatest("auto_listing").orElseThrow().template()).contains("JSON 格式返回", "title");
        assertThat(registry.getLatest("judge_system").orElseThrow().template()).contains("严格按 JSON 输出", "\"score\"");
    }
}
