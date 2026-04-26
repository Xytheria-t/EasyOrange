package com.cartethyia.easyorange.common.exception;

import com.cartethyia.easyorange.common.enums.FileResultCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link FileException} 单元测试
 *
 * @author cartethyia
 */
@DisplayName("FileException Tests")
class FileExceptionTest {

    @Nested
    @DisplayName("Factory Method Tests")
    class FactoryMethodTests {

        @Test
        @DisplayName("of with message should create exception with correct message")
        void of_withMessage_createsExceptionWithCorrectMessage() {
            // Arrange
            String message = "文件上传失败";

            // Act
            FileException exception = FileException.of(message);

            // Assert
            assertThat(exception.getMessage()).isEqualTo(message);
            assertThat(exception.getCode()).isEqualTo(FileResultCode.FILE_UPLOAD_FAILED.getCode());
        }

        @Test
        @DisplayName("of with message and cause should create exception with cause")
        void of_withMessageAndCause_createsExceptionWithCause() {
            // Arrange
            String message = "文件处理异常";
            Throwable cause = new IOException("IO 错误");

            // Act
            FileException exception = FileException.of(message, cause);

            // Assert
            assertThat(exception.getMessage()).isEqualTo(message);
            assertThat(exception.getCause()).isEqualTo(cause);
        }

        @Test
        @DisplayName("of with resultCode should create exception with correct code")
        void of_withResultCode_createsExceptionWithCorrectCode() {
            // Arrange
            FileResultCode resultCode = FileResultCode.FILE_SIZE_EXCEEDED;

            // Act
            FileException exception = FileException.of(resultCode);

            // Assert
            assertThat(exception.getCode()).isEqualTo(resultCode.getCode());
            assertThat(exception.getMessage()).isEqualTo(resultCode.getMessage());
        }

        @Test
        @DisplayName("of with resultCode and message should create exception with both")
        void of_withResultCodeAndMessage_createsExceptionWithBoth() {
            // Arrange
            FileResultCode resultCode = FileResultCode.FILE_TYPE_NOT_ALLOWED;
            String customMessage = "不支持的文件类型";

            // Act
            FileException exception = FileException.of(resultCode, customMessage);

            // Assert
            assertThat(exception.getCode()).isEqualTo(resultCode.getCode());
            assertThat(exception.getMessage()).contains(customMessage);
        }

        @Test
        @DisplayName("of with resultCode, message and cause should create exception with all")
        void of_withResultCodeMessageAndCause_createsExceptionWithAll() {
            // Arrange
            FileResultCode resultCode = FileResultCode.FILE_UPLOAD_FAILED;
            String customMessage = "上传过程中断";
            Throwable cause = new RuntimeException("网络错误");

            // Act
            FileException exception = FileException.of(resultCode, customMessage, cause);

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
        @DisplayName("defaultCode should return FILE_UPLOAD_FAILED code")
        void defaultCode_shouldReturnFileUploadFailedCode() {
            // Arrange
            FileException exception = FileException.of("测试异常");

            // Assert
            assertThat(exception.defaultCode()).isEqualTo(FileResultCode.FILE_UPLOAD_FAILED.getCode());
        }
    }

    @Nested
    @DisplayName("Inheritance Tests")
    class InheritanceTests {

        @Test
        @DisplayName("FileException should extend BaseBusinessException")
        void shouldExtendBaseBusinessException() {
            // Arrange
            FileException exception = FileException.of("测试");

            // Assert
            assertThat(exception).isInstanceOf(BaseBusinessException.class);
        }
    }
}
