package com.cartethyia.easyorange.user.adapter.inbound.web.validation;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Constraint;
import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PasswordAnnotationTest {

    @Test
    @DisplayName("@Password validatedBy PasswordValidator")
    void passwordValidatedByPasswordValidator() {
        var targets = Password.class.getAnnotation(Constraint.class).validatedBy();
        assertThat(targets).containsExactly(PasswordValidator.class);
    }

    @Test
    @DisplayName("@Password targets FIELD and PARAMETER")
    void passwordTargetsFieldAndParameter() {
        var targets =
                Password.class.getAnnotation(java.lang.annotation.Target.class).value();
        assertThat(targets).containsExactlyInAnyOrder(ElementType.FIELD, ElementType.PARAMETER);
    }

    @Test
    @DisplayName("@Password has correct retention policy")
    void passwordHasCorrectRetention() {
        assertThat(Password.class
                        .getAnnotation(java.lang.annotation.Retention.class)
                        .value())
                .isEqualTo(RetentionPolicy.RUNTIME);
    }

    @Test
    @DisplayName("@Password has correct default message")
    void passwordHasCorrectDefaultMessage() throws NoSuchMethodException {
        assertThat(Password.class.getMethod("message").getDefaultValue()).isEqualTo("密码长度8-128位");
    }

    @Test
    @DisplayName("@Password has default groups")
    void passwordHasDefaultGroups() throws NoSuchMethodException {
        var groups = (Class<?>[]) Password.class.getMethod("groups").getDefaultValue();
        assertThat(groups).isEmpty();
    }

    @Test
    @DisplayName("@Password has default payload")
    void passwordHasDefaultPayload() throws NoSuchMethodException {
        var payload = (Class<?>[]) Password.class.getMethod("payload").getDefaultValue();
        assertThat(payload).isEmpty();
    }
}
