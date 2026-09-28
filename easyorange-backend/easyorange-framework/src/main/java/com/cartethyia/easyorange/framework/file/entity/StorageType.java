package com.cartethyia.easyorange.framework.file.entity;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 存储类型 — {@code eo_upload_file.storage_type} 的取值面（DB VARCHAR(32)，列注释 LOCAL/S3/OSS）。
 * <p>
 * 只声明已落地的 {@link #LOCAL}：出站端口 {@code FileStoragePort} 目前只有本地实现，
 * 写死字符串会让「接入第二种存储」在编译期毫无提示。
 */
@Getter
@AllArgsConstructor
public enum StorageType {
    LOCAL("LOCAL");

    @EnumValue
    @JsonValue
    private final String code;

    public static StorageType fromCode(String code) {
        for (var type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown StorageType code: " + code);
    }
}
