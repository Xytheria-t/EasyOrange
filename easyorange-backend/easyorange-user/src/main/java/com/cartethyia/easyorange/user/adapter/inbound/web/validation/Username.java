package com.cartethyia.easyorange.user.adapter.inbound.web.validation;

import com.cartethyia.easyorange.user.domain.constant.UserConstant;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = UsernameValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Username {

    String message() default "用户名必须包含字母、数字或下划线，长度" + UserConstant.USERNAME_MIN_LENGTH + "-" + UserConstant.USERNAME_MAX_LENGTH + "位";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}