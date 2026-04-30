package com.cartethyia.easyorange.user.common.validation;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cartethyia.easyorange.user.infrastructure.persistence.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UniqueFieldValidatorTest {

    @Mock
    private UserMapper userMapper;

    private UniqueFieldValidator validator;

    @BeforeEach
    void setUp() {
        validator = new UniqueFieldValidator(userMapper);
    }

    @Nested
    @DisplayName("initialize() tests")
    class InitializeTests {

        @Test
        @DisplayName("initializes with valid annotation values")
        void initializesWithValidAnnotation() {
            var annotation = new TestUniqueAnnotation("username", "用户名已存在", "id");
            validator.initialize(annotation);

            assertThat(validator).isNotNull();
        }

        @Test
        @DisplayName("throws when annotation is null")
        void throwsWhenAnnotationIsNull() {
            assertThatThrownBy(() -> validator.initialize(null))
                .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("isValid() - null and empty handling")
    class NullAndEmptyTests {

        @Test
        @DisplayName("null object - returns true (skip validation)")
        void isValid_nullObject_returnsTrue() {
            validator.initialize(new TestUniqueAnnotation("username", "用户名已存在", "id"));
            assertThat(validator.isValid(null, null)).isTrue();
        }

        @Test
        @DisplayName("null field name - returns true")
        void isValid_nullFieldName_returnsTrue() {
            validator.initialize(new TestUniqueAnnotation(null, "error", "id"));
            assertThat(validator.isValid(new Object(), null)).isTrue();
        }

        @Test
        @DisplayName("empty field name - returns true")
        void isValid_emptyFieldName_returnsTrue() {
            validator.initialize(new TestUniqueAnnotation("", "error", "id"));
            assertThat(validator.isValid(new Object(), null)).isTrue();
        }

        @Test
        @DisplayName("blank field name - returns true")
        void isValid_blankFieldName_returnsTrue() {
            validator.initialize(new TestUniqueAnnotation("   ", "error", "id"));
            assertThat(validator.isValid(new Object(), null)).isTrue();
        }
    }

    @Nested
    @DisplayName("isValid() - username uniqueness")
    class UsernameUniquenessTests {

        @Test
        @DisplayName("unique username - returns true")
        void isValid_uniqueUsername_returnsTrue() {
            when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

            validator.initialize(new TestUniqueAnnotation("username", "用户名已存在", "id"));
            var request = new RegisterRequest("newuser", "aA123456");

            assertThat(validator.isValid(request, null)).isTrue();
            verify(userMapper).selectCount(any(LambdaQueryWrapper.class));
        }

        @Test
        @DisplayName("duplicate username - returns false")
        void isValid_duplicateUsername_returnsFalse() {
            when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

            validator.initialize(new TestUniqueAnnotation("username", "用户名已存在", "id"));
            var request = new RegisterRequest("existinguser", "aA123456");

            assertThat(validator.isValid(request, null)).isFalse();
            verify(userMapper).selectCount(any(LambdaQueryWrapper.class));
        }

        @Test
        @DisplayName("blank username - returns true (skip)")
        void isValid_blankUsername_returnsTrue() {
            validator.initialize(new TestUniqueAnnotation("username", "用户名已存在", "id"));
            var request = new RegisterRequest("   ", "aA123456");

            assertThat(validator.isValid(request, null)).isTrue();
            verifyNoInteractions(userMapper);
        }
    }

    @Nested
    @DisplayName("isValid() - email uniqueness")
    class EmailUniquenessTests {

        @Test
        @DisplayName("unique email - returns true")
        void isValid_uniqueEmail_returnsTrue() {
            when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

            validator.initialize(new TestUniqueAnnotation("email", "邮箱已存在", "id"));
            var request = new RegisterRequest("user1", "aA123456", "test@example.com", null, null);

            assertThat(validator.isValid(request, null)).isTrue();
        }

        @Test
        @DisplayName("duplicate email - returns false")
        void isValid_duplicateEmail_returnsFalse() {
            when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

            validator.initialize(new TestUniqueAnnotation("email", "邮箱已存在", "id"));
            var request = new RegisterRequest("user1", "aA123456", "test@example.com", null, null);

            assertThat(validator.isValid(request, null)).isFalse();
        }
    }

    @Nested
    @DisplayName("isValid() - phone uniqueness")
    class PhoneUniquenessTests {

        @Test
        @DisplayName("unique phone - returns true")
        void isValid_uniquePhone_returnsTrue() {
            when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

            validator.initialize(new TestUniqueAnnotation("phone", "手机号已存在", "id"));
            var request = new RegisterRequest("user1", "aA123456", null, "13800138000", null);

            assertThat(validator.isValid(request, null)).isTrue();
        }

        @Test
        @DisplayName("duplicate phone - returns false")
        void isValid_duplicatePhone_returnsFalse() {
            when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

            validator.initialize(new TestUniqueAnnotation("phone", "手机号已存在", "id"));
            var request = new RegisterRequest("user1", "aA123456", null, "13800138000", null);

            assertThat(validator.isValid(request, null)).isFalse();
        }
    }

    @Nested
    @DisplayName("isValid() - field existence checks")
    class FieldExistenceTests {

        @Test
        @DisplayName("non-existent field - returns true with warning logged")
        void isValid_nonExistentField_returnsTrue() {
            validator.initialize(new TestUniqueAnnotation("nonExistent", "error", "id"));
            var request = new RegisterRequest("user1", "aA123456", null, null, null);

            assertThat(validator.isValid(request, null)).isTrue();
            verifyNoInteractions(userMapper);
        }

        @Test
        @DisplayName("field is null value - returns true (skip)")
        void isValid_nullFieldValue_returnsTrue() {
            validator.initialize(new TestUniqueAnnotation("username", "用户名已存在", "id"));
            var request = new RegisterRequest(null, "aA123456", null, null, null);

            assertThat(validator.isValid(request, null)).isTrue();
            verifyNoInteractions(userMapper);
        }

        @Test
        @DisplayName("field is empty value - returns true (skip)")
        void isValid_emptyFieldValue_returnsTrue() {
            validator.initialize(new TestUniqueAnnotation("username", "用户名已存在", "id"));
            var request = new RegisterRequest("", "aA123456", null, null, null);

            assertThat(validator.isValid(request, null)).isTrue();
            verifyNoInteractions(userMapper);
        }
    }

    @Nested
    @DisplayName("isValid() - unsupported field type")
    class UnsupportedFieldTests {

        @Test
        @DisplayName("field not in FIELD_GETTERS map - returns true")
        void isValid_unsupportedField_returnsTrue() {
            validator.initialize(new TestUniqueAnnotation("password", "error", "id"));
            var request = new RegisterRequest("user1", "aA123456", null, null, null);

            assertThat(validator.isValid(request, null)).isTrue();
            verifyNoInteractions(userMapper);
        }
    }

    private record RegisterRequest(String username, String password, String email, String phone, Long id) {
        RegisterRequest(String username, String password) {
            this(username, password, null, null, null);
        }
    }

    private record TestUniqueAnnotation(String field, String message, String idField) implements Unique {
        @Override public String message() { return message; }
        @Override public Class<?>[] groups() { return new Class[0]; }
        @Override public Class<? extends jakarta.validation.Payload>[] payload() { return new Class[0]; }
        @Override public Class<? extends java.lang.annotation.Annotation> annotationType() { return Unique.class; }
    }
}
