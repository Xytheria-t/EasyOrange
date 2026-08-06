package com.cartethyia.easyorange.user.adapter.inbound.web.dto.request.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.cartethyia.easyorange.user.domain.valueobject.LoginCredential;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SmsLoginRequestTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    @DisplayName("phone should be @Pattern(UserConstant.PHONE_REGEX) (invalid format)")
    void phoneInvalid() {
        var violations = validator.validateValue(SmsLoginRequest.class, "phone", "not-a-phone");
        assertThat(violations).isNotEmpty();
    }

    @Test
    @DisplayName("phone should be valid format")
    void phoneValid() {
        var violations = validator.validateValue(SmsLoginRequest.class, "phone", "13800138000");
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("verifyCode should be @NotBlank")
    void verifyCodeNotBlank() {
        var violations = validator.validateValue(SmsLoginRequest.class, "verifyCode", "");
        assertThat(violations).isNotEmpty();
    }

    @Test
    @DisplayName("toCredential should create SmsLogin with correct fields")
    void toCredential() {
        var request = new SmsLoginRequest("13800138000", "123456");
        var credential = request.toCredential();

        assertThat(credential).isInstanceOf(LoginCredential.Sms.class);
        var smsLogin = (LoginCredential.Sms) credential;
        assertThat(smsLogin.phone()).isEqualTo("13800138000");
        assertThat(smsLogin.verifyCode()).isEqualTo("123456");
    }
}
