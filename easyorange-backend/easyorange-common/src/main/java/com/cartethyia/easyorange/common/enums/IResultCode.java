package com.cartethyia.easyorange.common.enums;

public interface IResultCode {

    String getCode();

    String getMessage();

    default boolean isSuccess() {
        return "A0000".equals(getCode());
    }

    /**
     * 根据错误码前缀映射到 HTTP 状态码
     *
     * <p>A - 成功/客户端语义: A0401/A0402→401, A0403→403, A0404→404, A0405→405, A0500→400, 其余A→200</p>
     * <p>B - 业务错误: 400</p>
     * <p>C - 系统错误: 500</p>
     * <p>D - 第三方错误: 502</p>
     * <p>未知前缀: 400</p>
     * <p>null: 500</p>
     *
     * @param code 业务错误码
     * @return 映射后的 HTTP 状态码
     */
    static int mapToHttpStatus(String code) {
        if (code == null) {
            return 500;
        }
        if (code.isEmpty()) {
            return 400;
        }
        return switch (code.charAt(0)) {
            case 'A' -> {
                if (code.length() >= 5) {
                    yield switch (code.substring(2, 5)) {
                        case "401", "402" -> 401; // A0402 为认证相关错误，统一映射 401
                        case "403"        -> 403;
                        case "404"        -> 404;
                        case "405"        -> 405;
                        case "500"        -> 400;
                        default           -> 200;
                    };
                }
                yield 200;
            }
            case 'C' -> 500;
            case 'D' -> 502;
            default  -> 400;
        };
    }
}