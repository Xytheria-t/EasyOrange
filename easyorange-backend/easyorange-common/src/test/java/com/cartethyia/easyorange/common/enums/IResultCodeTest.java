package com.cartethyia.easyorange.common.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

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
        @DisplayName("TOKEN_EXPIRED should have correct code and message")
        void TOKEN_EXPIRED_shouldHaveCorrectCodeAndMessage() {
            // Arrange & Act
            ResultCode tokenExpired = ResultCode.TOKEN_EXPIRED;

            // Assert
            assertThat(tokenExpired.getCode()).isEqualTo("A04011");
            assertThat(tokenExpired.getMessage()).isEqualTo("登录已过期");
        }

        @Test
        @DisplayName("UPSTREAM_ERROR should have correct code and message")
        void UPSTREAM_ERROR_shouldHaveCorrectCodeAndMessage() {
            // Arrange & Act
            ResultCode upstreamError = ResultCode.UPSTREAM_ERROR;

            // Assert
            assertThat(upstreamError.getCode()).isEqualTo("D0502");
            assertThat(upstreamError.getMessage()).isEqualTo("上游服务不可用");
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
        @DisplayName("FILE_NOT_FOUND should have correct code and message")
        void FILE_NOT_FOUND_shouldHaveCorrectCodeAndMessage() {
            // Arrange & Act
            FileResultCode fileNotFound = FileResultCode.FILE_NOT_FOUND;

            // Assert
            assertThat(fileNotFound.getCode()).isEqualTo("B5003");
            assertThat(fileNotFound.getMessage()).isEqualTo("文件不存在");
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

    @Nested
    @DisplayName("HTTP Status Mapping Tests")
    class HttpStatusMappingTests {

        @Test
        @DisplayName("A0401/A04011 should map to 401")
        void a0401AndA04011_shouldMapToUnauthorized() {
            assertThat(ResultCode.UNAUTHORIZED.httpStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(ResultCode.TOKEN_EXPIRED.httpStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("A0403 should map to 403")
        void a0403_shouldMapToForbidden() {
            assertThat(ResultCode.FORBIDDEN.httpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("A0404 should map to 404")
        void a0404_shouldMapToNotFound() {
            assertThat(ResultCode.NOT_FOUND.httpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("A0405 should map to 405")
        void a0405_shouldMapToMethodNotAllowed() {
            assertThat(ResultCode.METHOD_NOT_ALLOWED.httpStatus()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        }

        @Test
        @DisplayName("A0429 should map to 429")
        void a0429_shouldMapToTooManyRequests() {
            assertThat(ResultCode.TOO_MANY_REQUESTS.httpStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        }

        @Test
        @DisplayName("B prefix should map to 400")
        void bPrefix_shouldMapToBadRequest() {
            assertThat(ResultCode.BUSINESS_ERROR.httpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(FileResultCode.FILE_UPLOAD_FAILED.httpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("C prefix should map to 500")
        void cPrefix_shouldMapToInternalServerError() {
            assertThat(ResultCode.INTERNAL_SERVER_ERROR.httpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        @Test
        @DisplayName("D prefix should map to 502")
        void dPrefix_shouldMapToBadGateway() {
            assertThat(ResultCode.UPSTREAM_ERROR.httpStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
        }

        @Test
        @DisplayName("未知 A 码应归 400（防止静默成功）")
        void unknownACode_shouldMapToBadRequest() {
            IResultCode unknown = new TestResultCode("A0999", "未知");
            assertThat(unknown.httpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("A 段子码按前 4 位数字推导（A04011 → 401）")
        void aSubcode_shouldDeriveFromFirstFourDigits() {
            assertThat(IResultCode.resolveStatus("A04011")).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("A 段新增 4xx 家族码自动推导，无需枚举（A0406 → 406）")
        void aNew4xxFamily_shouldDeriveAutomatically() {
            assertThat(IResultCode.resolveStatus("A0406")).isEqualTo(HttpStatus.NOT_ACCEPTABLE);
        }

        @Test
        @DisplayName("A 段非数字码应归 400（不抛 NumberFormatException）")
        void aNonNumericCode_shouldMapToBadRequest() {
            assertThat(IResultCode.resolveStatus("A0X99")).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("null/空 code 应归 400")
        void nullOrEmptyCode_shouldMapToBadRequest() {
            assertThat(IResultCode.resolveStatus(null)).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(IResultCode.resolveStatus("")).isEqualTo(HttpStatus.BAD_REQUEST);
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
