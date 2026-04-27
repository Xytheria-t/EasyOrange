package com.cartethyia.easyorange.common.exception;

import com.cartethyia.easyorange.common.enums.IResultCode;
import com.cartethyia.easyorange.common.enums.ResultCode;

/**
 * 业务异常类
 * <p>
 * 统一使用 {@code of()} 工厂方法创建实例。
 * </p>
 *
 * <pre>{@code
 * // 用法示例
 * throw BusinessException.of("用户不存在");
 * throw BusinessException.of(UserResultCode.USER_NOT_FOUND);
 * throw BusinessException.of(UserResultCode.USER_NOT_FOUND, "ID: " + userId);
 * }</pre>
 *
 * @author cartethyia
 */
public class BusinessException extends BaseBusinessException {

    // ==================== 构造函数 ====================

    protected BusinessException(String message) {
        super(message);
    }

    protected BusinessException(IResultCode resultCode) {
        super(resultCode);
    }

    protected BusinessException(String message, Throwable cause) {
        super(message, cause);
    }

    protected BusinessException(IResultCode resultCode, String message) {
        super(resultCode, message);
    }

    protected BusinessException(IResultCode resultCode, String message, Throwable cause) {
        super(resultCode, message, cause);
    }

    // ==================== 工厂方法 ====================

    public static BusinessException of(String message) {
        return new BusinessException(message);
    }

    public static BusinessException of(IResultCode resultCode) {
        return new BusinessException(resultCode);
    }

    public static BusinessException of(String message, Throwable cause) {
        return new BusinessException(message, cause);
    }

    public static BusinessException of(IResultCode resultCode, String message) {
        return new BusinessException(resultCode, message);
    }

    public static BusinessException of(IResultCode resultCode, String message, Throwable cause) {
        return new BusinessException(resultCode, message, cause);
    }

    // ==================== 覆盖方法 ====================

    @Override
    protected String defaultCode() {
        return ResultCode.BUSINESS_ERROR.getCode();
    }
}
