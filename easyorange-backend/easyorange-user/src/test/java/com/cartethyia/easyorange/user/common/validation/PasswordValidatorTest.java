package com.cartethyia.easyorange.user.common.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordValidatorTest {

    private final PasswordValidator validator = new PasswordValidator();

    @Test
    @DisplayName("valid password - lowercase, uppercase, digit, length 6")
    void isValid_validPassword_returnsTrue() {
        assertThat(validator.isValid("aA1234", null)).isTrue();
    }

    @Test
    @DisplayName("valid password - exactly 20 chars")
    void isValid_validPasswordMaxLen_returnsTrue() {
        assertThat(validator.isValid("aA1" + "x".repeat(17), null)).isTrue();
    }

    @Test
    @DisplayName("valid password - all lowercase, uppercase, digits")
    void isValid_validPasswordComplex_returnsTrue() {
        assertThat(validator.isValid("abcXYZ789", null)).isTrue();
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
    @DisplayName("too short - invalid")
    void isValid_tooShort_returnsFalse() {
        assertThat(validator.isValid("aA1", null)).isFalse();
    }

    @Test
    @DisplayName("too long - invalid (21 chars)")
    void isValid_tooLong_returnsFalse() {
        assertThat(validator.isValid("aA1" + "x".repeat(18), null)).isFalse();
    }

    @Test
    @DisplayName("special chars only - invalid")
    void isValid_specialCharsOnly_returnsFalse() {
        assertThat(validator.isValid("!@#$%^&*()", null)).isFalse();
    }

    @Test
    @DisplayName("contains special chars - valid (special chars allowed)")
    void isValid_withSpecialChars_returnsTrue() {
        assertThat(validator.isValid("aA1!@#", null)).isTrue();
    }

    @Test
    @DisplayName("unicode characters - valid (regex . matches any char including unicode)")
    void isValid_unicodeChars_returnsTrue() {
        assertThat(validator.isValid("密码123Ab", null)).isTrue();
    }
}
