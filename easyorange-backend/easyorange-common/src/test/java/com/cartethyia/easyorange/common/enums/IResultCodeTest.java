package com.cartethyia.easyorange.common.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * {@link IResultCode} 单元测试
 *
 * @author cartethyia
 */
@DisplayName("IResultCode Tests")
class IResultCodeTest {

    @Nested
    @DisplayName("ResultCode Enum Tests")
    class ResultCodeEnumTests {

        @Test
        @DisplayName("SUCCESS should have correct code and message")
        void SUCCESS_shouldHaveCorrectCodeAndMessage() {
            // Arrange & Act
            ResultCode success = ResultCode.SUCCESS;

            // Assert
            assertThat(success.getCode()).isEqualTo("A0000");
            assertThat(success.getMessage()).isEqualTo("成功");
        }

        @Test
        @DisplayName("BUSINESS_ERROR should have correct code and message")
        void BUSINESS_ERROR_shouldHaveCorrectCodeAndMessage() {
            // Arrange & Act
            ResultCode businessError = ResultCode.BUSINESS_ERROR;

            // Assert
            assertThat(businessError.getCode()).isEqualTo("B0002");
            assertThat(businessError.getMessage()).isEqualTo("业务异常");
        }

        @Test
        @DisplayName("INTERNAL_SERVER_ERROR should have correct code and message")
        void INTERNAL_SERVER_ERROR_shouldHaveCorrectCodeAndMessage() {
            // Arrange & Act
            ResultCode systemError = ResultCode.INTERNAL_SERVER_ERROR;

            // Assert
            assertThat(systemError.getCode()).isEqualTo("C0500");
            assertThat(systemError.getMessage()).isEqualTo("服务器内部错误");
        }

        @Test
        @DisplayName("isSuccess should return true for SUCCESS")
        void isSuccess_shouldReturnTrueForSuccess() {
            // Arrange
            ResultCode success = ResultCode.SUCCESS;

            // Act
            boolean result = success.isSuccess();

            // Assert
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("isSuccess should return false for error codes")
        void isSuccess_shouldReturnFalseForErrorCodes() {
            // Arrange
            ResultCode error = ResultCode.FAIL;

            // Act
            boolean result = error.isSuccess();

            // Assert
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("FileResultCode Enum Tests")
    class FileResultCodeEnumTests {

        @Test
        @DisplayName("FILE_UPLOAD_FAILED should have correct code and message")
        void FILE_UPLOAD_FAILED_shouldHaveCorrectCodeAndMessage() {
            // Arrange & Act
            FileResultCode fileUploadFailed = FileResultCode.FILE_UPLOAD_FAILED;

            // Assert
            assertThat(fileUploadFailed.getCode()).isEqualTo("B5001");
            assertThat(fileUploadFailed.getMessage()).isEqualTo("文件上传失败");
        }

        @Test
        @DisplayName("FILE_SIZE_EXCEEDED should have correct code and message")
        void FILE_SIZE_EXCEEDED_shouldHaveCorrectCodeAndMessage() {
            // Arrange & Act
            FileResultCode fileSizeExceeded = FileResultCode.FILE_SIZE_EXCEEDED;

            // Assert
            assertThat(fileSizeExceeded.getCode()).isEqualTo("B5005");
            assertThat(fileSizeExceeded.getMessage()).isEqualTo("文件大小超出限制");
        }

        @Test
        @DisplayName("FILE_TYPE_NOT_ALLOWED should have correct code and message")
        void FILE_TYPE_NOT_ALLOWED_shouldHaveCorrectCodeAndMessage() {
            // Arrange & Act
            FileResultCode fileTypeNotAllowed = FileResultCode.FILE_TYPE_NOT_ALLOWED;

            // Assert
            assertThat(fileTypeNotAllowed.getCode()).isEqualTo("B5004");
            assertThat(fileTypeNotAllowed.getMessage()).isEqualTo("文件类型不允许");
        }

        @Test
        @DisplayName("isSuccess should return false for all file errors")
        void isSuccess_shouldReturnFalseForAllFileErrors() {
            // Arrange
            FileResultCode[] codes = FileResultCode.values();

            // Act & Assert
            for (FileResultCode code : codes) {
                assertThat(code.isSuccess()).isFalse();
            }
        }
    }

    /**
     * 测试用的 IResultCode 实现
     */
    private static class TestResultCode implements IResultCode {
        private final String code;
        private final String message;

        public TestResultCode(String code, String message) {
            this.code = code;
            this.message = message;
        }

        @Override
        public String getCode() {
            return code;
        }

        @Override
        public String getMessage() {
            return message;
        }
    }
}
