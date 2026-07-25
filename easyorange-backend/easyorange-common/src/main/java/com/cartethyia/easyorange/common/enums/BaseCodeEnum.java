package com.cartethyia.easyorange.common.enums;

/**
 * code 枚举公共接口 — 统一 fromCode 查找逻辑。
 * <p>
 * 所有 domain 层枚举（XxxStatus / XxxType / XxxMethod / XxxLevel）实现此接口，
 * 避免每个枚举重复 for 循环 + null 检查 + throw 模板。
 * <p>
 * 用法：
 * <pre>{@code
 * public enum OrderStatus implements BaseCodeEnum {
 *     PENDING_PAYMENT("PENDING_PAYMENT", "待付款"),
 *     // ...
 *     @JsonValue private final String code;
 *     private final String desc;
 *
 *     public static OrderStatus fromCode(String code) {
 *         return BaseCodeEnum.fromCode(OrderStatus.class, code);
 *     }
 * }
 * }</pre>
 */
public interface BaseCodeEnum {

    String getCode();

    /**
     * 通用 fromCode 查找 — 未匹配时抛 IllegalArgumentException（fail fast，不返回 null）。
     */
    static <E extends BaseCodeEnum> E fromCode(Class<E> enumType, String code) {
        if (code == null) {
            throw new IllegalArgumentException(enumType.getSimpleName() + " code must not be null");
        }
        for (E e : enumType.getEnumConstants()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Unknown " + enumType.getSimpleName() + " code: " + code);
    }
}
