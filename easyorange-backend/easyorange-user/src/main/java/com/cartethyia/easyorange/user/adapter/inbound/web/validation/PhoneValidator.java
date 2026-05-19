package com.cartethyia.easyorange.user.adapter.inbound.web.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class PhoneValidator implements ConstraintValidator<Phone, String> {

    private Pattern pattern;

    @Override
    public void initialize(Phone annotation) {
        this.pattern = Pattern.compile(annotation.regexp());
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        return pattern.matcher(value).matches();
    }
}