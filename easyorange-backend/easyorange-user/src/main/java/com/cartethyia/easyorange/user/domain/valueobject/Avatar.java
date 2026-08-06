package com.cartethyia.easyorange.user.domain.valueobject;

import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.user.domain.enums.UserResultCode;

/**
 * 头像值对象 — 承载「头像」概念的领域规则(大小限制)。
 * <p>
 * 规则与错误码/文案单一来源:校验通过 {@link #validate(byte[])} 在副作用前完成,
 * 落库后经 {@link #uploaded(String, byte[], String)} 构造。
 */
public record Avatar(String url, long sizeBytes, String contentType) {

    /** 头像大小上限(字节)。 */
    public static final long MAX_SIZE_BYTES = 5L * 1024 * 1024;

    /** 上传前校验内容(无副作用)。空内容或超限即抛领域异常。 */
    public static void validate(byte[] content) {
        if (content == null || content.length == 0) {
            throw BusinessException.of(UserResultCode.AVATAR_EMPTY);
        }
        if (content.length > MAX_SIZE_BYTES) {
            throw BusinessException.of(UserResultCode.AVATAR_TOO_LARGE);
        }
    }

    /** 上传成功后构造已落库的头像值对象。 */
    public static Avatar uploaded(String url, byte[] content, String contentType) {
        return new Avatar(url, content.length, contentType);
    }
}
