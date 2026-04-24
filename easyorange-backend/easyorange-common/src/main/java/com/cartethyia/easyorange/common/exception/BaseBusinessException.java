package com.cartethyia.easyorange.common.exception;

import com.cartethyia.easyorange.common.enums.IResultCode;
import lombok.Getter;

/**
 * 业务异常抽象基类
 * <p>
 * 所有业务相关异常应继承此类，统一错误码和消息处理逻辑。
 * 子类只需覆盖 {@link #defaultCode()} 即可复用所有构造函数和工厂方法。
 * </p>
 *
 * @author cartethyia
 */
@Getter
public abstract class BaseBusinessException extends RuntimeException {

    private final String code;

    protected abstract String defaultCode();

    protected BaseBusinessException(String message) {
        super(message);
        this.code = defaultCode();
    }

    protected BaseBusinessException(String message, Throwable cause) {
        super(message, cause);
        this.code = defaultCode();
    }

    protected BaseBusinessException(IResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    protected BaseBusinessException(IResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }

    protected BaseBusinessException(IResultCode resultCode, String message, Throwable cause) {
        super(message, cause);
        this.code = resultCode.getCode();
    }

    /**
     * 根据错误码返回对应的 HTTP 状态码
     *
     * @return HTTP 状态码
     */
    public int httpStatus() {
        return IResultCode.mapToHttpStatus(code);
    }
}
