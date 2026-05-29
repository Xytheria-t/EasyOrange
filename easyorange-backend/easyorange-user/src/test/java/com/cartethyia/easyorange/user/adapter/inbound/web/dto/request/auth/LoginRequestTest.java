package com.cartethyia.easyorange.user.adapter.inbound.web.dto.request.auth;

import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LoginRequestTest {

    @Test
    @DisplayName("loginMethod accessor has @NotBlank annotation")
    void loginMethodHasNotBlank() throws Exception {
        var accessor = LoginRequest.class.getMethod("loginMethod");
        var annotations = accessor.getAnnotationsByType(NotBlank.class);
        assertThat(annotations)
                .as("loginMethod accessor should be annotated with @NotBlank")
                .hasSize(1);
        assertThat(annotations[0].message()).isEqualTo("登录方式不能为空");
    }
}
