package com.cartethyia.easyorange.user.adapter.inbound.web.validation;

import com.cartethyia.easyorange.user.domain.constant.UserConstant;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = PasswordValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Password {

    String message() default "密码长度" + UserConstant.PASSWORD_MIN_LENGTH + "-" + UserConstant.PASSWORD_MAX_LENGTH + "位";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
