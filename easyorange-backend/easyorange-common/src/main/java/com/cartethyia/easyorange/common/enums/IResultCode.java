package com.cartethyia.easyorange.common.enums;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

import org.springframework.http.HttpStatus;

public interface IResultCode {

    String getCode();

    String getMessage();

    default boolean isSuccess() {
        return ResultCode.SUCCESS.getCode().equals(getCode());
    }

    /**
     * 当前错误码对应的 HTTP 状态码，按错误码前缀映射。
     *
     * @see #resolveStatus(String)
     */
    default HttpStatus httpStatus() {
        return resolveStatus(getCode());
    }

    /**
     * 按错误码映射 HTTP 状态码（错误码体系单一来源，GlobalExceptionHandler 与
     * {@code ErrorResponse} 均消费此映射）：
     * <pre>
     * A 段：码内第 2-5 位数字自动推导，仅接受合法 4xx（A0401/A04011→401、A0403→403、
     *       A0404→404、A0405→405、A0429→429），其余 A 码 → 400
     * B → 400 | C → 500 | D → 502 | 未知/空 → 400
     * </pre>
     * A 段由码内数字推导，码与映射永不漂移，新增 A04xx 家族码无需改此方法。
     */
    static HttpStatus resolveStatus(String code) {
        if (code == null || code.isEmpty()) {
            return BAD_REQUEST;
        }
        return switch (code.charAt(0)) {
            case 'A' -> resolveA4xx(code);
            case 'C' -> INTERNAL_SERVER_ERROR;
            case 'D' -> BAD_GATEWAY;
            default -> BAD_REQUEST;
        };
    }

    /**
     * A 段数字自动推导：取 {@code code[1,5)} 解析为 HTTP 状态码，仅接受合法 4xx
     * （A0401→401 / A04011→401 子码 / A0429→429）；解析失败、非法或非 4xx → 400。
     */
    private static HttpStatus resolveA4xx(String code) {
        if (code.length() < 5) {
            return BAD_REQUEST;
        }
        final int status;
        try {
            status = Integer.parseInt(code.substring(1, 5));
        } catch (NumberFormatException e) {
            return BAD_REQUEST;
        }
        HttpStatus resolved = HttpStatus.resolve(status);
        return resolved != null && resolved.is4xxClientError() ? resolved : BAD_REQUEST;
    }
}
