package com.cartethyia.easyorange.ai.prompt;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Prompt YAML 内容回归测试 — 守卫 6 个 AI 服务 prompt 模板已加载且内容完整。
 * <p>
 * 该测试与 {@link YamlPromptRegistry} 同包，可调用 package-private {@code init()}
 * 触发 classpath 加载，验证生产环境真实的 YAML 文件可被解析。
 */
@DisplayName("Prompt YAML 内容回归测试")
class PromptContentTest {

    private static YamlPromptRegistry registry;

    @BeforeAll
    static void loadRegistry() {
        registry = new YamlPromptRegistry();
        registry.init(); // package-private — 触发 classpath:prompts/*.yml 加载
    }

    @Test
    @DisplayName("6 个 AI 服务 prompt 模板全部加载成功")
    void allSixPromptsLoaded() {
        assertThat(registry.getLatest("ai_pricing_system")).isPresent();
        assertThat(registry.getLatest("ai_copy_generation_system")).isPresent();
        assertThat(registry.getLatest("ai_review_system")).isPresent();
        assertThat(registry.getLatest("ai_qa_system")).isPresent();
        assertThat(registry.getLatest("auto_listing_visual")).isPresent();
        assertThat(registry.getLatest("auto_listing_system")).isPresent();
    }

    @ParameterizedTest
    @CsvSource({
        "ai_pricing_system, 智能估值助手",
        "ai_copy_generation_system, 智能文案生成助手",
        "ai_review_system, 资产审核助手",
        "ai_qa_system, 智能客服助手",
        "auto_listing_visual, 商品类型和名称",
        "auto_listing_system, 智能上架助手"
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
    @DisplayName("所有 prompt 版本号为 v1.0.0")
    void allPromptsAtVersionV1_0_0() {
        for (var name : new String[] {
            "ai_pricing_system", "ai_copy_generation_system", "ai_review_system",
            "ai_qa_system", "auto_listing_visual", "auto_listing_system"
        }) {
            var template = registry.getLatest(name).orElseThrow();
            assertThat(template.version()).as("prompt '%s' 版本号", name).isEqualTo("v1.0.0");
        }
    }

    @Test
    @DisplayName("JSON 输出类 prompt 包含 JSON 格式说明")
    void jsonPromptsContainJsonFormatSpec() {
        var pricing = registry.getLatest("ai_pricing_system").orElseThrow().template();
        var copy = registry.getLatest("ai_copy_generation_system").orElseThrow().template();
        var review = registry.getLatest("ai_review_system").orElseThrow().template();
        var listing = registry.getLatest("auto_listing_system").orElseThrow().template();

        assertThat(pricing).contains("JSON 格式返回", "suggestedPrice");
        assertThat(copy).contains("JSON 格式返回", "title");
        assertThat(review).contains("JSON 格式返回", "suggestedAction");
        assertThat(listing).contains("JSON 格式返回", "title");
    }
}
