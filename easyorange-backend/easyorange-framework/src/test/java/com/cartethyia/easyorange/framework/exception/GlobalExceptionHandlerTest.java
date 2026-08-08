package com.cartethyia.easyorange.framework.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.cartethyia.easyorange.common.enums.ResultCode;
import com.cartethyia.easyorange.common.exception.BaseBusinessException;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.result.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@DisplayName("GlobalExceptionHandler 单元测试")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Nested
    @DisplayName("handle(BaseBusinessException)")
    class BaseBusinessExceptionTests {

        @Test
        @DisplayName("BaseBusinessException 子类应返回 400 而非 500")
        void handleBaseBusinessException_returnsBadRequest() {
            BaseBusinessException ex = new TestBusinessException("测试业务异常");

            ResponseEntity<Result<Void>> response = handler.handle(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().code()).isEqualTo("TEST_CODE");
            assertThat(response.getBody().message()).isEqualTo("测试业务异常");
            assertThat(response.getBody().isSuccess()).isFalse();
        }

        @Test
        @DisplayName("A0403 错误码应映射到 403")
        void handleBaseBusinessException_a403_mapsToForbidden() {
            BaseBusinessException ex = BusinessException.of(ResultCode.FORBIDDEN, "禁止访问");

            ResponseEntity<Result<Void>> response = handler.handle(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(response.getBody().code()).isEqualTo(ResultCode.FORBIDDEN.getCode());
        }

        @Test
        @DisplayName("未知 A 码应归为 400 而非 200（防止静默成功）")
        void handleBaseBusinessException_unknownACode_mapsToBadRequest() {
            BaseBusinessException ex = new TestBusinessException("测试") {
                @Override
                protected String defaultCode() {
                    return "A0999";
                }
            };

            ResponseEntity<Result<Void>> response = handler.handle(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("BusinessException 通过 handle 方法处理（继承覆盖）")
    class BusinessExceptionRegressionTests {

        @Test
        @DisplayName("BusinessException 仍正常返回对应状态码")
        void handleBusinessException_stillWorks() {
            BusinessException ex = BusinessException.of(ResultCode.BUSINESS_ERROR, "业务错误");

            ResponseEntity<Result<Void>> response = handler.handle(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().code()).isEqualTo(ResultCode.BUSINESS_ERROR.getCode());
            assertThat(response.getBody().message()).isEqualTo("业务错误");
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
}