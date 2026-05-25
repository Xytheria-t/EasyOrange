package com.cartethyia.easyorange.framework.exception;

import com.cartethyia.easyorange.common.exception.BaseBusinessException;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.enums.ResultCode;
import com.cartethyia.easyorange.common.result.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GlobalExceptionHandler 单元测试")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Nested
    @DisplayName("handleBaseBusinessException")
    class BaseBusinessExceptionTests {

        @Test
        @DisplayName("BaseBusinessException 子类应返回 400 而非 500")
        void handleBaseBusinessException_returnsBadRequest() {
            BaseBusinessException ex = new TestBusinessException("测试业务异常");

            ResponseEntity<Result<Void>> response = handler.handleBaseBusinessException(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().code()).isEqualTo("TEST_CODE");
            assertThat(response.getBody().message()).isEqualTo("测试业务异常");
        }

        @Test
        @DisplayName("使用异常自身 httpStatus() 方法的状态码")
        void handleBaseBusinessException_usesExceptionHttpStatus() {
            BaseBusinessException ex = new TestConflictBusinessException("冲突错误");

            ResponseEntity<Result<Void>> response = handler.handleBaseBusinessException(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        }
    }

    @Nested
    @DisplayName("BusinessException 通过 handleBaseBusinessException 处理（继承覆盖）")
    class BusinessExceptionRegressionTests {

        @Test
        @DisplayName("BusinessException 仍正常返回对应状态码")
        void handleBusinessException_stillWorks() {
            BusinessException ex = BusinessException.of(ResultCode.BUSINESS_ERROR, "业务错误");

            ResponseEntity<Result<Void>> response = handler.handleBaseBusinessException(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().code()).isEqualTo(ResultCode.BUSINESS_ERROR.getCode());
        }
    }

    private static class TestBusinessException extends BaseBusinessException {
        public TestBusinessException(String message) {
            super(message);
        }

        @Override
        protected String defaultCode() {
            return "TEST_CODE";
        }
    }

    private static class TestConflictBusinessException extends BaseBusinessException {
        public TestConflictBusinessException(String message) {
            super(message);
        }

        @Override
        protected String defaultCode() {
            return "CONFLICT_CODE";
        }

        @Override
        public HttpStatus httpStatus() {
            return HttpStatus.CONFLICT;
        }
    }
}
