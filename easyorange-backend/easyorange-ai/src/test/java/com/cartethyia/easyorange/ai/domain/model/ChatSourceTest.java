package com.cartethyia.easyorange.ai.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 来源类型的 JSON 形态 —— 前端 {@code parseSources} 按小写字面量判 asset / knowledge，
 * 枚举名默认下发大写会让商品引用全被判成知识库来源（点不动）。
 */
class ChatSourceTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("序列化下发小写 type，与前端字面量对齐")
    void serializesLowercaseType() throws Exception {
        var source = new ChatSource(ChatSource.Type.ASSET, "product-1", "iPhone 15");

        String json = mapper.writeValueAsString(source);

        assertThat(json).contains("\"type\":\"asset\"");
        assertThat(mapper.readTree(json).get("type").asText()).isEqualTo("asset");
    }

    @Test
    @DisplayName("知识库来源同样下发小写")
    void serializesKnowledgeLowercase() throws Exception {
        var source = new ChatSource(ChatSource.Type.KNOWLEDGE, "doc-1", "退款规则");

        assertThat(mapper.writeValueAsString(source)).contains("\"type\":\"knowledge\"");
    }

    @Test
    @DisplayName("反序列化接受小写 code")
    void deserializesLowercaseCode() throws Exception {
        var source = mapper.readValue("{\"type\":\"asset\",\"id\":\"p1\"}", ChatSource.class);

        assertThat(source.type()).isEqualTo(ChatSource.Type.ASSET);
    }

    @Test
    @DisplayName("未知来源类型显式拒绝，不静默降级")
    void rejectsUnknownCode() {
        assertThatThrownBy(() -> ChatSource.Type.fromCode("banner"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("banner");
    }
}
