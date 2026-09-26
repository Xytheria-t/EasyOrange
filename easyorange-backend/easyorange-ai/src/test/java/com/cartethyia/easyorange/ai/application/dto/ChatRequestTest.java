package com.cartethyia.easyorange.ai.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link ChatRequest} 的 Bean Validation 约束 — 服务端兜底（前端输入框未设 maxlength）。
 * 2000 字上限的成本口径见该字段的 javadoc。
 */
@DisplayName("ChatRequest 约束 -> 测试")
class ChatRequestTest {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("空白问题被拒（NotBlank）")
    void blankQuestionRejected() {
        var violations = VALIDATOR.validate(new ChatRequest("  ", "sess-1", false));

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("question");
    }

    @Test
    @DisplayName("超长问题被拒（2000 字上限；当前问题不参与 token 裁剪，长度是单次成本的护栏）")
    void oversizedQuestionRejected() {
        var violations = VALIDATOR.validate(new ChatRequest("问".repeat(2001), "sess-1", false));

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("question");
    }

    @Test
    @DisplayName("恰好 2000 字在限内（边界值）")
    void exactlyAtLimitAccepted() {
        assertThat(VALIDATOR.validate(new ChatRequest("问".repeat(2000), "sess-1", false))).isEmpty();
    }

    @Test
    @DisplayName("合法请求无约束违规")
    void validRequestAccepted() {
        assertThat(VALIDATOR.validate(new ChatRequest("怎么退款？", "sess-1", false))).isEmpty();
    }
}
