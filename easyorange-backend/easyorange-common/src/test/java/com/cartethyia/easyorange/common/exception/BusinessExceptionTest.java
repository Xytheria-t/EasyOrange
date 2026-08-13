package com.cartethyia.easyorange.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.cartethyia.easyorange.common.enums.ResultCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.ErrorResponse;

/**
 * {@link BusinessException} 单元测试
 *
 * @author cartethyia
 */
@DisplayName("BusinessException Tests")
class BusinessExceptionTest {

    @Nested
    @DisplayName("Factory Method Tests")
    class FactoryMethodTests {

        @Test
        @DisplayName("of with message should create exception with correct message")
        void of_withMessage_createsExceptionWithCorrectMessage() {
            // Arrange
            String message = "业务异常测试";

            // Act
            BusinessException exception = BusinessException.of(message);

            // Assert
            assertThat(exception.getMessage()).isEqualTo(message);
            assertThat(exception.getCode()).isEqualTo(ResultCode.BUSINESS_ERROR.getCode());
        }

        @Test
        @DisplayName("of with resultCode should create exception with correct code and message")
        void of_withResultCode_createsExceptionWithCorrectCodeAndMessage() {
            // Arrange
            ResultCode resultCode = ResultCode.UNAUTHORIZED;

            // Act
            BusinessException exception = BusinessException.of(resultCode);

            // Assert
            assertThat(exception.getCode()).isEqualTo(resultCode.getCode());
            assertThat(exception.getMessage()).isEqualTo(resultCode.getMessage());
        }

        @Test
        @DisplayName("of with message and cause should create exception with cause")
        void of_withMessageAndCause_createsExceptionWithCause() {
            // Arrange
            String message = "业务异常";
            Throwable cause = new IllegalArgumentException("参数错误");

            // Act
            BusinessException exception = BusinessException.of(message, cause);

            // Assert
            assertThat(exception.getMessage()).isEqualTo(message);
            assertThat(exception.getCause()).isEqualTo(cause);
        }

        @Test
        @DisplayName("of with resultCode and message should create exception with both")
        void of_withResultCodeAndMessage_createsExceptionWithBoth() {
            // Arrange
            ResultCode resultCode = ResultCode.FORBIDDEN;
            String customMessage = "自定义错误消息";

            // Act
            BusinessException exception = BusinessException.of(resultCode, customMessage);

            // Assert
            assertThat(exception.getCode()).isEqualTo(resultCode.getCode());
            assertThat(exception.getMessage()).contains(customMessage);
        }

        @Test
        @DisplayName("of with resultCode, message and cause should create exception with all")
        void of_withResultCodeMessageAndCause_createsExceptionWithAll() {
            // Arrange
            ResultCode resultCode = ResultCode.NOT_FOUND;
            String customMessage = "资源不存在";
            Throwable cause = new RuntimeException("底层异常");

            // Act
            BusinessException exception = BusinessException.of(resultCode, customMessage, cause);

            // Assert
            assertThat(exception.getCode()).isEqualTo(resultCode.getCode());
            assertThat(exception.getMessage()).contains(customMessage);
            assertThat(exception.getCause()).isEqualTo(cause);
        }
    }

    @Nested
    @DisplayName("Default Code Tests")
    class DefaultCodeTests {

        @Test
        @DisplayName("defaultCode should return BUSINESS_ERROR code")
        void defaultCode_shouldReturnBusinessErrorCode() {
            // Arrange
            BusinessException exception = BusinessException.of("测试异常");

            // Assert
            assertThat(exception.defaultCode()).isEqualTo(ResultCode.BUSINESS_ERROR.getCode());
        }
    }

    @Nested
    @DisplayName("StatusCode Tests")
    class StatusCodeTests {

        @Test
        @DisplayName("B 前缀错误码应映射到 400")
        void bCode_shouldMapToBadRequest() {
            // Arrange
            BusinessException exception = BusinessException.of("业务异常");

            // Assert
            assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("A0403 错误码应映射到 403")
        void a0403_shouldMapToForbidden() {
            // Arrange
            BusinessException exception = BusinessException.of(ResultCode.FORBIDDEN);

            // Assert
            assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("A0429 错误码应映射到 429")
        void a0429_shouldMapToTooManyRequests() {
            // Arrange
            BusinessException exception = BusinessException.of(ResultCode.TOO_MANY_REQUESTS);

            // Assert
            assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        }

        @Test
        @DisplayName("C 前缀错误码应映射到 500")
        void cCode_shouldMapToInternalServerError() {
            // Arrange
            BusinessException exception = BusinessException.of(ResultCode.INTERNAL_SERVER_ERROR);

            // Assert
            assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        @Test
        @DisplayName("D 前缀错误码应映射到 502")
        void dCode_shouldMapToBadGateway() {
            // Arrange
            BusinessException exception = BusinessException.of(ResultCode.UPSTREAM_ERROR);

            // Assert
            assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        }
    }

    @Nested
    @DisplayName("ErrorResponse Tests")
    class ErrorResponseTests {

        @Test
        @DisplayName("业务异常应实现 Spring ErrorResponse（状态码单一来源）")
        void shouldImplementErrorResponse() {
            // Arrange
            BusinessException exception = BusinessException.of(ResultCode.FORBIDDEN);

            // Assert
            assertThat(exception).isInstanceOf(ErrorResponse.class);
            assertThat(((ErrorResponse) exception).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }
    }

    @Nested
    @DisplayName("Inheritance Tests")
    class InheritanceTests {

        @Test
        @DisplayName("BusinessException should extend BaseBusinessException")
        void shouldExtendBaseBusinessException() {
            // Arrange
            BusinessException exception = BusinessException.of("测试");

            // Assert
            assertThat(exception).isInstanceOf(BaseBusinessException.class);
        }
    }
}
