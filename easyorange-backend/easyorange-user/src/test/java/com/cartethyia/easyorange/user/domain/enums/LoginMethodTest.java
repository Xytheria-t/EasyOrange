package com.cartethyia.easyorange.user.domain.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoginMethodTest {

    @Test
    @DisplayName("fromCode returns PASSWORD for 'password'")
    void fromCode_returnsPassword() {
        assertThat(LoginMethod.fromCode("password")).isEqualTo(LoginMethod.PASSWORD);
    }

    @Test
    @DisplayName("fromCode returns SMS for 'sms'")
    void fromCode_returnsSms() {
        assertThat(LoginMethod.fromCode("sms")).isEqualTo(LoginMethod.SMS);
    }

    @Test
    @DisplayName("fromCode throws IllegalArgumentException for null code")
    void fromCode_throwsOnNull() {
        assertThatThrownBy(() -> LoginMethod.fromCode(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("LoginMethod code must not be null");
    }

    @Test
    @DisplayName("fromCode throws IllegalArgumentException for unknown code")
    void fromCode_throwsOnUnknown() {
        assertThatThrownBy(() -> LoginMethod.fromCode("unknown"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unknown LoginMethod code: unknown");
    }
}
