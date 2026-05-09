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
     * A - 成功/客户端语义: A0401->401, A0403->403, A0404->404, A0405->405, A0500->400, 其余A->200
     * B - 业务错误: 400
     * C - 系统错误: 500
     * D - 第三方错误: 502
     * 未知前缀: 400
     * null: 500
     */
    static int mapToHttpStatus(String code) {
        if (code == null) {
            return 500;
        }
        if (code.isEmpty()) {
            return 400;
        }
        char prefix = code.charAt(0);
        switch (prefix) {
            case 'A':
                if (code.length() >= 5) {
                    String suffix = code.substring(2, 5);
                    switch (suffix) {
                        case "401":
                        case "402": return 401;
                        case "403": return 403;
                        case "404": return 404;
                        case "405": return 405;
                        case "500": return 400;
                        default: return 200;
                    }
                }
                return 200;
            case 'B':
                return 400;
            case 'C':
                return 500;
            case 'D':
                return 502;
            default:
                return 400;
        }
    }
}