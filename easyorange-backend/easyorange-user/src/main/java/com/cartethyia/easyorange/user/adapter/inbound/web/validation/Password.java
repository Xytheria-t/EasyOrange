package com.cartethyia.easyorange.user.adapter.inbound.web.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = PasswordValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Password {

    String message() default "密码必须包含大小写字母、数字和特殊字符，长度8-128位";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
