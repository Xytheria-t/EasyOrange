package com.cartethyia.easyorange.user.adapter.inbound.web.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = UniqueFieldValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Unique.List.class)
public @interface Unique {

    String message() default "字段值已存在";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    String field();

    String idField() default "id";

    Class<?> entityClass();

    @Documented
    @Target({ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @interface List {
        Unique[] value();
    }
}
