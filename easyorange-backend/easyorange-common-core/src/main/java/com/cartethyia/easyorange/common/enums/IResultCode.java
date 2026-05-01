package com.cartethyia.easyorange.common.enums;

public interface IResultCode {

    String getCode();

    String getMessage();

    default boolean isSuccess() {
        return "A0000".equals(getCode());
    }

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
