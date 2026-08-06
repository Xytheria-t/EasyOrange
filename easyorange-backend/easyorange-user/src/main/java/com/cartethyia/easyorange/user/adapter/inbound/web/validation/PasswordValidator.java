package com.cartethyia.easyorange.user.adapter.inbound.web.validation;

import com.cartethyia.easyorange.user.domain.constant.UserConstant;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PasswordValidator implements ConstraintValidator<Password, String> {

    private static final Pattern PATTERN = Pattern.compile(UserConstant.PASSWORD_REGEX);

    private final Set<String> weakPasswords;

    public PasswordValidator(@Value("${easyorange.validation.password.weak-list:#{null}}") Set<String> weakPasswords) {
        this.weakPasswords = weakPasswords != null ? weakPasswords : Set.of();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }

        if (!PATTERN.matcher(value).matches()) {
            return false;
        }

        if (weakPasswords.contains(value)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("密码过于简单，请使用更强的密码").addConstraintViolation();
            return false;
        }

        return true;
    }
}
