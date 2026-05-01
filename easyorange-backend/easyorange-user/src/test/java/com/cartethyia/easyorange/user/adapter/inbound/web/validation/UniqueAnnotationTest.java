package com.cartethyia.easyorange.user.adapter.inbound.web.validation;

import jakarta.validation.Constraint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;

import static org.assertj.core.api.Assertions.assertThat;

class UniqueAnnotationTest {

    @Test
    @DisplayName("@Unique validatedBy UniqueFieldValidator")
    void uniqueValidatedByUniqueFieldValidator() {
        var targets = Unique.class.getAnnotation(Constraint.class).validatedBy();
        assertThat(targets).containsExactly(UniqueFieldValidator.class);
    }

    @Test
    @DisplayName("@Unique targets TYPE (class-level)")
    void uniqueTargetsType() {
        var targets = Unique.class.getAnnotation(java.lang.annotation.Target.class).value();
        assertThat(targets).containsExactly(ElementType.TYPE);
    }

    @Test
    @DisplayName("@Unique has correct retention policy")
    void uniqueHasCorrectRetention() {
        assertThat(Unique.class.getAnnotation(java.lang.annotation.Retention.class).value())
            .isEqualTo(RetentionPolicy.RUNTIME);
    }

    @Test
    @DisplayName("@Unique has correct default message")
    void uniqueHasCorrectDefaultMessage() throws NoSuchMethodException {
        assertThat(Unique.class.getMethod("message").getDefaultValue())
            .isEqualTo("字段值已存在");
    }

    @Test
    @DisplayName("@Unique has default groups")
    void uniqueHasDefaultGroups() throws NoSuchMethodException {
        var groups = (Class<?>[]) Unique.class.getMethod("groups").getDefaultValue();
        assertThat(groups).isEmpty();
    }

    @Test
    @DisplayName("@Unique has default payload")
    void uniqueHasDefaultPayload() throws NoSuchMethodException {
        var payload = (Class<?>[]) Unique.class.getMethod("payload").getDefaultValue();
        assertThat(payload).isEmpty();
    }

    @Test
    @DisplayName("@Unique has required field attribute")
    void uniqueHasFieldAttribute() throws NoSuchMethodException {
        var method = Unique.class.getMethod("field");
        assertThat(method).isNotNull();
    }

    @Test
    @DisplayName("@Unique has idField attribute with default 'id'")
    void uniqueHasIdFieldDefault() throws NoSuchMethodException {
        assertThat(Unique.class.getMethod("idField").getDefaultValue())
            .isEqualTo("id");
    }
}
