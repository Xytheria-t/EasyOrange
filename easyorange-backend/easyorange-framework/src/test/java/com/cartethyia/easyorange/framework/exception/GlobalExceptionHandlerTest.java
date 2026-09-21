package com.cartethyia.easyorange.framework.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.cartethyia.easyorange.common.enums.ResultCode;
import com.cartethyia.easyorange.common.exception.BaseBusinessException;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.common.result.Result;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.core.MethodParameter;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.validation.method.MethodValidationResult;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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

    @Nested
    @DisplayName("handle(框架异常)")
    class FrameworkExceptionTests {

        @Test
        @DisplayName("AuthenticationException 应返回 401 而非 500")
        void handleAuthenticationException_returnsUnauthorized() {
            var ex = new InsufficientAuthenticationException("未登录");

            ResponseEntity<Result<Void>> response = handler.handle(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody().code()).isEqualTo(ResultCode.UNAUTHORIZED.getCode());
        }

        @Test
        @DisplayName("HttpMediaTypeNotSupportedException 应返回 415")
        void handleMediaTypeNotSupported_returnsUnsupportedMediaType() {
            var ex = new HttpMediaTypeNotSupportedException(MediaType.APPLICATION_XML, List.of());

            ResponseEntity<Result<Void>> response = handler.handle(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
            assertThat(response.getBody().code()).isEqualTo(ResultCode.PARAM_ERROR.getCode());
        }

        @Test
        @DisplayName("IllegalArgumentException 应落 500 兜底（非业务异常，提示编程错误）")
        void handleIllegalArgumentException_returnsInternalServerError() {
            var ex = new IllegalArgumentException("非法参数");

            ResponseEntity<Result<Void>> response = handler.handle(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody().code()).isEqualTo(ResultCode.INTERNAL_SERVER_ERROR.getCode());
        }

        @Test
        @DisplayName("NoResourceFoundException 应返回 404")
        void handleNoResourceFound_returnsNotFound() {
            var ex = new NoResourceFoundException(HttpMethod.GET, "/no-such.png", "/no-such.png");

            ResponseEntity<Result<Void>> response = handler.handle(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody().code()).isEqualTo(ResultCode.NOT_FOUND.getCode());
        }

        @Test
        @DisplayName("A0429 错误码应映射到 429")
        void handleTooManyRequests_mapsTo429() {
            BaseBusinessException ex = BusinessException.of(ResultCode.TOO_MANY_REQUESTS, "请求过于频繁");

            ResponseEntity<Result<Void>> response = handler.handle(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
            assertThat(response.getBody().code()).isEqualTo(ResultCode.TOO_MANY_REQUESTS.getCode());
        }
    }

    @Nested
    @DisplayName("handle(持久化/上传异常)")
    class PersistenceAndUploadExceptionTests {

        @Test
        @DisplayName("DuplicateKeyException 应返回 400（唯一键兜底）而非 500")
        void handleDuplicateKey_returnsBadRequest() {
            var ex = new DuplicateKeyException("Duplicate entry 'x' for key 'uk_eo_user_username'");

            ResponseEntity<Result<Void>> response = handler.handle(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().code()).isEqualTo(ResultCode.VALIDATE_FAILED.getCode());
        }

        @Test
        @DisplayName("CannotAcquireLockException（MySQL 死锁/锁等待超时）应映射并发冲突 B0006 而非 500")
        void handleCannotAcquireLock_mapsToConcurrentUpdate() {
            var ex = new CannotAcquireLockException("Deadlock found when trying to get lock");

            ResponseEntity<Result<Void>> response = handler.handle(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().code()).isEqualTo(ResultCode.CONCURRENT_UPDATE.getCode());
            assertThat(response.getBody().message()).contains("重试");
        }

        @Test
        @DisplayName("MaxUploadSizeExceededException 应返回 413 + A0413 + 上限提示（不得落 500 被前端重试）")
        void handleMaxUploadSize_returnsContentTooLarge() {
            var ex = new MaxUploadSizeExceededException(10 * 1024 * 1024L);

            ResponseEntity<Result<Void>> response = handler.handle(ex);

            // 断言数字 413 而非只比常量：413 在 RFC 9110 里由 Payload Too Large 更名为 Content Too Large，
            // 常量名随 Spring 版本摇摆，状态码语义不能跟着漂
            assertThat(response.getStatusCode().value()).isEqualTo(413);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONTENT_TOO_LARGE);
            assertThat(response.getBody().code()).isEqualTo(ResultCode.CONTENT_TOO_LARGE.getCode());
            assertThat(response.getBody().message()).contains("10MB");
        }

        @Test
        @DisplayName("上限未知（maxUploadSize<=0）时不拼上限后缀，仍返回 413")
        void handleMaxUploadSize_unknownLimit_omitsSuffix() {
            var ex = new MaxUploadSizeExceededException(-1L);

            ResponseEntity<Result<Void>> response = handler.handle(ex);

            assertThat(response.getStatusCode().value()).isEqualTo(413);
            assertThat(response.getBody().message()).isEqualTo(ResultCode.CONTENT_TOO_LARGE.getMessage());
        }

        @Test
        @DisplayName("上限不足 1MB 时按 KB 展示")
        void handleMaxUploadSize_kbLimit_usesKbSuffix() {
            var ex = new MaxUploadSizeExceededException(512 * 1024L);

            ResponseEntity<Result<Void>> response = handler.handle(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONTENT_TOO_LARGE);
            assertThat(response.getBody().message()).contains("512KB");
        }

        @Test
        @DisplayName("MultipartException（multipart 解析失败）应返回 400 而非 500")
        void handleMultipart_returnsBadRequest() {
            var ex = new MultipartException("Failed to parse multipart servlet request");

            ResponseEntity<Result<Void>> response = handler.handle(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().code()).isEqualTo(ResultCode.VALIDATE_FAILED.getCode());
        }
    }

    @Nested
    @DisplayName("handle(方法参数级校验)")
    class MethodValidationExceptionTests {

        @Test
        @DisplayName("HandlerMethodValidationException 应返回 400 + B0003 并带出参数消息（不得落 500）")
        void handleHandlerMethodValidation_returnsBadRequestWithParameterMessages() throws Exception {
            var ex = handlerMethodValidationException("手机号格式不正确");

            ResponseEntity<Result<Void>> response = handler.handle(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().code()).isEqualTo(ResultCode.VALIDATE_FAILED.getCode());
            assertThat(response.getBody().message()).contains("手机号格式不正确");
        }

        @Test
        @DisplayName("只有跨参数约束结果（无参数级结果）时回退为通用参数校验失败提示")
        void handleHandlerMethodValidation_noParameterResults_fallsBackToGenericMessage() throws Exception {
            // Spring 保证每个 ParameterValidationResult 至少有一条 resolvableError，
            // 故"无消息"只可能在 getParameterValidationResults() 为空（仅跨参数约束）时出现，用子类固定该形状
            var ex = new HandlerMethodValidationException(handlerMethodValidationResult("手机号格式不正确")) {
                @Override
                public List<ParameterValidationResult> getParameterValidationResults() {
                    return List.of();
                }
            };

            ResponseEntity<Result<Void>> response = handler.handle(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().message()).isEqualTo(ResultCode.VALIDATE_FAILED.getMessage());
        }

        /** 用 Spring 公开 API 组装真实异常：MethodValidationResult.create + ParameterValidationResult。 */
        private static HandlerMethodValidationException handlerMethodValidationException(String defaultMessage)
                throws Exception {
            return new HandlerMethodValidationException(handlerMethodValidationResult(defaultMessage));
        }

        private static MethodValidationResult handlerMethodValidationResult(String defaultMessage) throws Exception {
            var method = Fixture.class.getDeclaredMethod("endpoint", String.class);
            var parameter = new MethodParameter(method, 0);
            var resolvables =
                    List.<MessageSourceResolvable>of(new DefaultMessageSourceResolvable(null, null, defaultMessage));
            var result = new ParameterValidationResult(parameter, "123", resolvables, null, null, null, null);
            return MethodValidationResult.create(new Fixture(), method, List.of(result));
        }

        private static class Fixture {
            @SuppressWarnings("unused")
            void endpoint(String phone) {}
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
