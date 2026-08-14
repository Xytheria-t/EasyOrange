package com.cartethyia.easyorange.ai.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 知识库文档索引状态 — PENDING 待索引（写入成功/启动补索引重试）/ INDEXED 已入 ES / FAILED 摄入失败。
 */
@Getter
@RequiredArgsConstructor
public enum KnowledgeDocStatus {
    PENDING("PENDING"),
    INDEXED("INDEXED"),
    FAILED("FAILED");

    @EnumValue
    private final String code;

    public static KnowledgeDocStatus fromCode(String code) {
        for (var status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown KnowledgeDocStatus: " + code);
    }
}
