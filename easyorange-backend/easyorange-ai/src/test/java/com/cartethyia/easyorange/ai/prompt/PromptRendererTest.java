package com.cartethyia.easyorange.ai.prompt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PromptRenderer 测试")
class PromptRendererTest {

    @Test
    @DisplayName("单变量渲染正确替换占位符")
    void render_singleVariable_replacesPlaceholder() {
        var template = new PromptTemplate(
                "test", "v1.0.0", "你好，{name}！", "测试模板");

        var result = PromptRenderer.render(template, Map.of("name", "世界"));

        assertThat(result).isEqualTo("你好，世界！");
    }

    @Test
    @DisplayName("多变量渲染正确替换所有占位符")
    void render_multipleVariables_replacesAllPlaceholders() {
        var template = new PromptTemplate(
                "test", "v1.0.0",
                "商品：{productName}\n描述：{description}\n价格：{price}",
                "测试模板");

        var result = PromptRenderer.render(template, Map.of(
                "productName", "iPhone 15",
                "description", "九五新",
                "price", "¥5999"
        ));

        assertThat(result).isEqualTo("商品：iPhone 15\n描述：九五新\n价格：¥5999");
    }

    @Test
    @DisplayName("缺失变量时抛出 IllegalArgumentException")
    void render_missingVariable_throwsException() {
        var template = new PromptTemplate(
                "test", "v1.0.0", "你好，{name}，你来自{city}", "测试模板");

        assertThatThrownBy(() -> PromptRenderer.render(template, Map.of("name", "世界")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("city");
    }

    @Test
    @DisplayName("变量值含 $ 符号时正常渲染不被特殊解释")
    void render_valueWithDollarSign_rendersCorrectly() {
        var template = new PromptTemplate(
                "test", "v1.0.0", "价格：{price}", "测试模板");

        var result = PromptRenderer.render(template, Map.of("price", "$100"));

        assertThat(result).isEqualTo("价格：$100");
    }

    @Test
    @DisplayName("变量值含反斜杠时正常渲染不被特殊解释")
    void render_valueWithBackslash_rendersCorrectly() {
        var template = new PromptTemplate(
                "test", "v1.0.0", "路径：{path}", "测试模板");

        var result = PromptRenderer.render(template, Map.of("path", "C:\\Users\\test"));

        assertThat(result).isEqualTo("路径：C:\\Users\\test");
    }

    @Test
    @DisplayName("变量值含 $ 和反斜杠组合时正常渲染")
    void render_valueWithDollarAndBackslash_rendersCorrectly() {
        var template = new PromptTemplate(
                "test", "v1.0.0", "输入：{input}", "测试模板");

        var result = PromptRenderer.render(template, Map.of("input", "price=$5.00\\n"));

        assertThat(result).isEqualTo("输入：price=$5.00\\n");
    }

    @Test
    @DisplayName("无占位符的模板原样返回")
    void render_noPlaceholders_returnsAsIs() {
        var template = new PromptTemplate(
                "test", "v1.0.0", "这是一个没有占位符的模板", "测试模板");

        var result = PromptRenderer.render(template, Map.of());

        assertThat(result).isEqualTo("这是一个没有占位符的模板");
    }

    @Test
    @DisplayName("同一变量多次出现时全部替换")
    void render_repeatedVariable_replacesAllOccurrences() {
        var template = new PromptTemplate(
                "test", "v1.0.0", "{name}说：你好，{name}！", "测试模板");

        var result = PromptRenderer.render(template, Map.of("name", "小明"));

        assertThat(result).isEqualTo("小明说：你好，小明！");
    }
}
