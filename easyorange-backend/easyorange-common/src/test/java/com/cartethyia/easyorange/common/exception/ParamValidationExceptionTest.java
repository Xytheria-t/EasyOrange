package com.cartethyia.easyorange.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.cartethyia.easyorange.common.enums.ResultCode;
import com.cartethyia.easyorange.common.exception.validation.ParamValidationException;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * {@link ParamValidationException} 单元测试
 *
 * @author cartethyia
 */
@DisplayName("ParamValidationException Tests")
class ParamValidationExceptionTest {

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Constructor with fieldErrors should create exception with correct errors")
        void constructor_withFieldErrors_createsExceptionWithCorrectErrors() {
            // Arrange
            Map<String, String> fieldErrors = new HashMap<>();
            fieldErrors.put("username", "用户名不能为空");
            fieldErrors.put("email", "邮箱格式不正确");

            // Act
            ParamValidationException exception = new ParamValidationException(fieldErrors);

            // Assert
            assertThat(exception.getFieldErrors()).isEqualTo(fieldErrors);
            assertThat(exception.getCode()).isEqualTo(ResultCode.VALIDATE_FAILED.getCode());
        }

        @Test
        @DisplayName("Constructor with null fieldErrors should create empty map")
        void constructor_withNullFieldErrors_createsEmptyMap() {
            // Act
            ParamValidationException exception = new ParamValidationException(null);

            // Assert
            assertThat(exception.getFieldErrors()).isEmpty();
            assertThat(exception.getFieldErrors()).isNotNull();
        }

        @Test
        @DisplayName("Constructor with message and fieldErrors should create exception with both")
        void constructor_withMessageAndFieldErrors_createsExceptionWithBoth() {
            // Arrange
            String message = "自定义校验失败消息";
            Map<String, String> fieldErrors = Map.of("age", "年龄必须大于 0");

            // Act
            ParamValidationException exception = new ParamValidationException(message, fieldErrors);

            // Assert
            assertThat(exception.getMessage()).contains(message);
            assertThat(exception.getFieldErrors()).containsEntry("age", "年龄必须大于 0");
        }

        @Test
        @DisplayName("Constructor should create immutable copy of fieldErrors")
        void constructor_shouldCreateImmutableCopyOfFieldErrors() {
            // Arrange
            Map<String, String> originalErrors = new HashMap<>();
            originalErrors.put("name", "名称不能为空");

            // Act
            ParamValidationException exception = new ParamValidationException(originalErrors);
            originalErrors.put("newField", "新字段错误");

            // Assert
            assertThat(exception.getFieldErrors()).doesNotContainKey("newField");
            assertThat(exception.getFieldErrors()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Get First Error Message Tests")
    class GetFirstErrorMessageTests {

        @Test
        @DisplayName("getFirstErrorMessage with multiple errors should return first")
        void getFirstErrorMessage_withMultipleErrors_returnsFirst() {
            // Arrange
            Map<String, String> fieldErrors = new HashMap<>();
            fieldErrors.put("username", "用户名错误");
            fieldErrors.put("email", "邮箱错误");
            ParamValidationException exception = new ParamValidationException(fieldErrors);

            // Act
            String firstError = exception.getFirstErrorMessage();

            // Assert
            assertThat(firstError).isIn("用户名错误", "邮箱错误");
        }

        @Test
        @DisplayName("getFirstErrorMessage with single error should return it")
        void getFirstErrorMessage_withSingleError_returnsIt() {
            // Arrange
            Map<String, String> fieldErrors = Map.of("password", "密码长度至少为 6 位");
            ParamValidationException exception = new ParamValidationException(fieldErrors);

            // Act
            String firstError = exception.getFirstErrorMessage();

            // Assert
            assertThat(firstError).isEqualTo("密码长度至少为 6 位");
        }

        @Test
        @DisplayName("getFirstErrorMessage with empty errors should return message")
        void getFirstErrorMessage_withEmptyErrors_returnsMessage() {
            // Arrange
            String message = "参数校验失败";
            ParamValidationException exception = new ParamValidationException(message, Map.of());

            // Act
            String firstError = exception.getFirstErrorMessage();

            // Assert
            assertThat(firstError).isEqualTo(message);
        }
    }

    @Nested
    @DisplayName("Get Field Errors Tests")
    class GetFieldErrorsTests {

        @Test
        @DisplayName("getFieldErrors should return copy of errors")
        void getFieldErrors_shouldReturnCopyOfErrors() {
            // Arrange
            Map<String, String> originalErrors = new HashMap<>();
            originalErrors.put("field1", "error1");
            ParamValidationException exception = new ParamValidationException(originalErrors);

            // Act
            Map<String, String> returnedErrors = exception.getFieldErrors();

            // Assert
            assertThat(returnedErrors).isEqualTo(originalErrors);
            assertThat(returnedErrors).isNotSameAs(originalErrors);
        }

        @Test
        @DisplayName("getFieldErrors should be unmodifiable")
        void getFieldErrors_shouldBeUnmodifiable() {
            // Arrange
            ParamValidationException exception = new ParamValidationException(Map.of("field", "error"));

            // Act & Assert
            org.junit.jupiter.api.Assertions.assertThrows(
                    UnsupportedOperationException.class,
                    () -> exception.getFieldErrors().put("newField", "newError"));
        }
    }

    @Nested
    @DisplayName("Default Code Tests")
    class DefaultCodeTests {

        @Test
        @DisplayName("defaultCode should return VALIDATE_FAILED code")
        void defaultCode_shouldReturnValidateFailedCode() {
            // Arrange
            ParamValidationException exception = new ParamValidationException(Map.of());

            // Assert
            assertThat(exception.getCode()).isEqualTo(ResultCode.VALIDATE_FAILED.getCode());
        }
    }

    @Nested
    @DisplayName("Inheritance Tests")
    class InheritanceTests {

        @Test
        @DisplayName("ParamValidationException should extend BaseBusinessException")
        void shouldExtendBaseBusinessException() {
            // Arrange
            ParamValidationException exception = new ParamValidationException(Map.of());

            // Assert
            assertThat(exception).isInstanceOf(BaseBusinessException.class);
        }
    }
}
