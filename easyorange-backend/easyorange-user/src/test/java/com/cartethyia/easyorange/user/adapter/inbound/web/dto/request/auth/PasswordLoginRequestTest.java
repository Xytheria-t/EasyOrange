package com.cartethyia.easyorange.user.adapter.inbound.web.dto.request.auth;

import com.cartethyia.easyorange.user.domain.valueobject.LoginCredential;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordLoginRequestTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    @DisplayName("identifier should be @NotBlank")
    void identifierNotBlank() {
        var violations = validator.validateValue(PasswordLoginRequest.class, "identifier", "");
        assertThat(violations).isNotEmpty();
        assertThat(violations.iterator().next().getMessage()).isEqualTo("账号不能为空");
    }

    @Test
    @DisplayName("password should be @NotBlank")
    void passwordNotBlank() {
        var violations = validator.validateValue(PasswordLoginRequest.class, "password", "");
        assertThat(violations).isNotEmpty();
        assertThat(violations.iterator().next().getMessage()).isEqualTo("密码不能为空");
    }

    @Test
    @DisplayName("toCredential should create PasswordLogin with correct fields")
    void toCredential() {
        var request = new PasswordLoginRequest("testUser", "Password123!");
        var credential = request.toCredential();

        assertThat(credential).isInstanceOf(LoginCredential.Password.class);
        var passwordLogin = (LoginCredential.Password) credential;
        assertThat(passwordLogin.identifier()).isEqualTo("testUser");
        assertThat(passwordLogin.password()).isEqualTo("Password123!");
    }
}
