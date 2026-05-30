package com.cartethyia.easyorange.user.adapter.inbound.web.validation;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordValidatorTest {

    @Mock
    private ConstraintValidatorContext context;
    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder violationBuilder;

    private final PasswordValidator validator = new PasswordValidator(Set.of());
    private final PasswordValidator validatorWithWeakPasswords = new PasswordValidator(Set.of("Password123!", "Qwerty123!"));

    @Test
    @DisplayName("valid password - lowercase, uppercase, digit, special char, length 8")
    void isValid_validPassword_returnsTrue() {
        assertThat(validator.isValid("aA1234!@", null)).isTrue();
    }

    @Test
    @DisplayName("valid password - exactly 128 chars")
    void isValid_validPasswordMaxLen_returnsTrue() {
        String password = "aA1!" + "x".repeat(124);
        assertThat(validator.isValid(password, null)).isTrue();
    }

    @Test
    @DisplayName("valid password - all lowercase, uppercase, digits, special char")
    void isValid_validPasswordComplex_returnsTrue() {
        assertThat(validator.isValid("abcXYZ789!@", null)).isTrue();
    }

    @Test
    @DisplayName("null value - skips validation")
    void isValid_null_returnsTrue() {
        assertThat(validator.isValid(null, null)).isTrue();
    }

    @Test
    @DisplayName("blank value - skips validation")
    void isValid_blank_returnsTrue() {
        assertThat(validator.isValid("   ", null)).isTrue();
    }

    @Test
    @DisplayName("empty value - skips validation")
    void isValid_empty_returnsTrue() {
        assertThat(validator.isValid("", null)).isTrue();
    }

    @Test
    @DisplayName("missing lowercase - invalid")
    void isValid_missingLowercase_returnsFalse() {
        assertThat(validator.isValid("ABC12345", null)).isFalse();
    }

    @Test
    @DisplayName("missing uppercase - invalid")
    void isValid_missingUppercase_returnsFalse() {
        assertThat(validator.isValid("abc12345", null)).isFalse();
    }

    @Test
    @DisplayName("missing digit - invalid")
    void isValid_missingDigit_returnsFalse() {
        assertThat(validator.isValid("abcABCDEF", null)).isFalse();
    }

    @Test
    @DisplayName("missing special char - invalid")
    void isValid_missingSpecialChar_returnsFalse() {
        assertThat(validator.isValid("abcABCDEF123", null)).isFalse();
    }

    @Test
    @DisplayName("too short - invalid (7 chars)")
    void isValid_tooShort_returnsFalse() {
        assertThat(validator.isValid("aA1!@#", null)).isFalse();
    }

    @Test
    @DisplayName("too long - invalid (129 chars)")
    void isValid_tooLong_returnsFalse() {
        String password = "aA1!" + "x".repeat(125);
        assertThat(validator.isValid(password, null)).isFalse();
    }

    @Test
    @DisplayName("special chars only - invalid")
    void isValid_specialCharsOnly_returnsFalse() {
        assertThat(validator.isValid("!@#$%^&*()", null)).isFalse();
    }

    @Test
    @DisplayName("contains special chars - valid")
    void isValid_withSpecialChars_returnsTrue() {
        assertThat(validator.isValid("aA1234!@", null)).isTrue();
    }

    @Test
    @DisplayName("unicode characters with special char - valid")
    void isValid_unicodeChars_returnsTrue() {
        assertThat(validator.isValid("密码123Ab!", null)).isTrue();
    }

    @Test
    @DisplayName("weak password rejected")
    void isValid_weakPassword_returnsFalse() {
        when(context.buildConstraintViolationWithTemplate("密码过于简单，请使用更强的密码")).thenReturn(violationBuilder);
        assertThat(validatorWithWeakPasswords.isValid("Password123!", context)).isFalse();
    }

    @Test
    @DisplayName("non-weak password matching regex - not in weak list, should pass")
    void isValid_nonWeakPassword_returnsTrue() {
        assertThat(validatorWithWeakPasswords.isValid("StrongPass123!", context)).isTrue();
    }
}
