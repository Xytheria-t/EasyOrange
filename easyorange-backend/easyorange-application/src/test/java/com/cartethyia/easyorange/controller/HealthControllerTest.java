package com.cartethyia.easyorange.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("HealthController 测试")
class HealthControllerTest {

    @Test
    @DisplayName("health 端点应返回 UP 状态")
    void health_returnsUpStatus() {
        HealthController controller = new HealthController();
        var result = controller.health();

        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().get("status")).isEqualTo("UP");
        assertThat(result.getBody().get("timestamp")).isNotNull();
    }
}