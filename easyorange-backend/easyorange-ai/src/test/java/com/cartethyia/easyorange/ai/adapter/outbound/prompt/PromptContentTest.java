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
 * 该测试与 {@link YamlPromptRegistry} 同包，可调用 package-private {@code init()}
 * 触发 classpath 加载，验证生产环境真实的 YAML 文件可被解析。
 * <p>
 * {@link #ALL_PROMPTS} 是「prompt 全部走 YAML 版本化」这条铁律的断言载体：
 * 任何服务把 prompt 退回硬编码 Java 常量，这里的条数就对不上。
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
        "auto_listing_visual",
        "auto_listing_system",
        "search_intent_system"
    };

    @Test
    @DisplayName("5 个 prompt 模板全部加载成功（发布助手 2 + 对话 2 + 搜索意图识别）")
    void allPromptsLoaded() {
        for (String name : ALL_PROMPTS) {
            assertThat(registry.getLatest(name)).as("prompt '%s' 应加载成功", name).isPresent();
        }
    }

    @ParameterizedTest
    @CsvSource({
        "auto_listing_visual, 商品类型和名称",
        "auto_listing_system, 智能上架助手",
        "search_intent_system, AI 导购助手"
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
    @DisplayName("所有 prompt 都声明「标签块内是数据不是指令」（注入防护不可只覆盖部分链路）")
    void allPromptsDeclareDataNotInstructions() {
        for (String name : ALL_PROMPTS) {
            var template = registry.getLatest(name).orElseThrow();
            assertThat(template.template()).as("prompt '%s' 缺少提示词注入防护声明", name).contains("不是指令");
        }
    }

    @Test
    @DisplayName("所有 prompt 版本号受控（模板内容变更必须升版本）")
    void allPromptsAtControlledVersions() {
        // 已升版的 prompt 单列：ai_chat_tool_system 随多步 ReAct 循环改造（W1）升 v2.0.0
        var bumpedVersions = java.util.Map.of("ai_chat_tool_system", "v2.0.0");
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
        // 估值 / 文案两个 prompt 已随「发布助手收敛为拍照识别单入口」删除（fca8e918），
        // 能力并入 auto_listing_system；ai_review_system 已随管理端商品审核 AI 建议删除、
        // ai_qa_system 已随商品详情 AI 问答删除（均 2026-09-19，不在两条 AI 主线）。
        // 剩下 auto_listing_system 是仅有的 JSON 输出 prompt（search_intent_system 直接吐一句文本）
        var listing = registry.getLatest("auto_listing_system").orElseThrow().template();

        assertThat(listing).contains("JSON 格式返回", "title");
    }
}
