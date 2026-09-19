package com.cartethyia.easyorange.ai.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("TokenEstimator (上下文 token 估算) -> 测试")
class TokenEstimatorTest {

    @Test
    @DisplayName("null / 空串 -> 0")
    void estimate_blank() {
        assertThat(TokenEstimator.estimate(null)).isZero();
        assertThat(TokenEstimator.estimate("")).isZero();
    }

    @Test
    @DisplayName("纯中文按 0.7 token/字 向上取整")
    void estimate_cjk() {
        // 3 字 × 0.7 = 2.1 -> 3
        assertThat(TokenEstimator.estimate("退款吗")).isEqualTo(3);
    }

    @Test
    @DisplayName("纯 ASCII 按 0.3 token/字符 向上取整")
    void estimate_ascii() {
        // 10 字符 × 0.3 = 3.0 -> 3
        assertThat(TokenEstimator.estimate("aaaaaaaaaa")).isEqualTo(3);
    }

    @Test
    @DisplayName("中英混排分段计价")
    void estimate_mixed() {
        // 2 CJK × 0.7 + 6 ascii × 0.3 = 3.2 -> 4
        assertThat(TokenEstimator.estimate("退款refund")).isEqualTo(4);
    }

    @Test
    @DisplayName("全角标点按 CJK 计")
    void estimate_fullwidthPunctuation() {
        // 2 CJK + 1 全角问号 × 0.7 = 2.1 -> 3
        assertThat(TokenEstimator.estimate("退吗？")).isEqualTo(3);
    }

    @Test
    @DisplayName("估算单调：文本越长 token 越多")
    void estimate_monotonic() {
        assertThat(TokenEstimator.estimate("a")).isLessThanOrEqualTo(TokenEstimator.estimate("aa"));
        assertThat(TokenEstimator.estimate("好")).isLessThanOrEqualTo(TokenEstimator.estimate("好好"));
    }
}
