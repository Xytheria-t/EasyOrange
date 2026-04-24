package com.cartethyia.easyorange.common.enums;

/**
 * 响应码接口
 * <p>
 * 所有业务错误码枚举都应实现此接口，以统一响应格式。
 * </p>
 *
 * <p>错误码编码规范（字母+数字分段）：</p>
 * <ul>
 *   <li>Axxxx - 客户端错误（参数校验、认证授权等）</li>
 *   <li>Bxxxx - 业务错误（用户、商品、订单等业务逻辑）</li>
 *   <li>Cxxxx - 系统错误（服务器、数据库、内部异常）</li>
 *   <li>Dxxxx - 第三方服务错误（支付、短信、OSS等）</li>
 * </ul>
 *
 * @author cartethyia
 */
public interface IResultCode {

    String getCode();

    String getMessage();

    default boolean isSuccess() {
        return "A0000".equals(getCode());
    }

    /**
     * 根据错误码映射到 HTTP 状态码
     *
     * @param code 错误码
     * @return HTTP 状态码
     */
    static int mapToHttpStatus(String code) {
        if (code == null) return 500;
        if ("A0000".equals(code)) return 200;
        return switch (code.charAt(0)) {
            case 'A' -> switch (code) {
                case "A0401", "A0402" -> 401;
                case "A0403" -> 403;
                case "A0404" -> 404;
                case "A0405" -> 405;
                default -> 400;
            };
            case 'C' -> 500;
            case 'D' -> 502;
            default -> 400;
        };
    }
}
