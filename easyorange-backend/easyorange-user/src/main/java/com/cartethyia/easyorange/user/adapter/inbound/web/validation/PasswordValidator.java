package com.cartethyia.easyorange.user.adapter.inbound.web.validation;

import com.cartethyia.easyorange.user.constant.UserConstant;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Set;
import java.util.regex.Pattern;

public class PasswordValidator implements ConstraintValidator<Password, String> {

    private static final Pattern PATTERN = Pattern.compile(UserConstant.PASSWORD_REGEX);

    private static final Set<String> WEAK_PASSWORDS = Set.of(
        "Password1!", "Password123!", "Qwerty123!", "Admin123!",
        "Welcome1!", "Letmein1!", "Abc123!@", "Test123!",
        "Passw0rd!", "P@ssw0rd!", "Password!1", "Admin@123"
    );

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }

        if (!PATTERN.matcher(value).matches()) {
            return false;
        }

        if (WEAK_PASSWORDS.contains(value)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("密码过于简单，请使用更强的密码")
                   .addConstraintViolation();
            return false;
        }

        return true;
    }
}
