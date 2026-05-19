package com.cartethyia.easyorange.user.adapter.inbound.web.validation;

import com.cartethyia.easyorange.user.domain.constants.UserConstant;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class UsernameValidator implements ConstraintValidator<Username, String> {

    private static final Pattern PATTERN = Pattern.compile("^[a-zA-Z0-9_]+$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        return value.length() >= UserConstant.USERNAME_MIN_LENGTH
            && value.length() <= UserConstant.USERNAME_MAX_LENGTH
            && PATTERN.matcher(value).matches();
    }
}