package com.cartethyia.easyorange.common.enums;

/**
 * code 枚举公共接口 — 统一 {@code fromCode} 查找，避免每个枚举各写一遍 for 循环 + null 检查 + throw 模板。
 * domain 层的 XxxStatus / XxxType / XxxMethod / XxxLevel 一律实现本接口，code 与 desc 的对应由实现方声明。
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
