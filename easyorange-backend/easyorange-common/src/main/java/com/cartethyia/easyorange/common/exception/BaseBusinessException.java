package com.cartethyia.easyorange.common.exception;

import com.cartethyia.easyorange.common.enums.IResultCode;
import com.cartethyia.easyorange.common.enums.ResultCode;
import lombok.Getter;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponse;

@Getter
public abstract class BaseBusinessException extends RuntimeException implements ErrorResponse {

    private final String code;

    /**
     * HTTP 状态码按错误码前缀映射（A0401→401 / A0403→403 / B→400 / C→500 / D→502），
     * 映射单一来源见 {@link IResultCode#resolveStatus(String)}。
     */
    @Override
    public HttpStatusCode getStatusCode() {
        return IResultCode.resolveStatus(code);
    }

    /**
     * 标准 ProblemDetail 体（RFC 9457）。线路契约仍为 {@code Result<T>} 信封，
     * 此实现供 ErrorResponse 机制与未来 ProblemDetail 场景消费。
     */
    @Override
    public ProblemDetail getBody() {
        return ProblemDetail.forStatusAndDetail(getStatusCode(), getMessage());
    }

    protected BaseBusinessException(String message) {
        super(message);
        this.code = defaultCode();
    }

    protected BaseBusinessException(IResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    protected BaseBusinessException(String message, Throwable cause) {
        super(message, cause);
        this.code = defaultCode();
    }

    protected BaseBusinessException(IResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }

    protected BaseBusinessException(IResultCode resultCode, String message, Throwable cause) {
        super(message, cause);
        this.code = resultCode.getCode();
    }

    protected String defaultCode() {
        return ResultCode.BUSINESS_ERROR.getCode();
    }
}
