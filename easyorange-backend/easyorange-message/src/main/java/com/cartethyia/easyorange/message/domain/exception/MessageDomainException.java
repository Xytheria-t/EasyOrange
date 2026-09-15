package com.cartethyia.easyorange.message.domain.exception;

import com.cartethyia.easyorange.common.enums.IResultCode;
import com.cartethyia.easyorange.common.exception.BaseBusinessException;
import com.cartethyia.easyorange.message.domain.enums.MessageResultCode;
import lombok.Getter;

/**
 * 消息域业务异常 — 模块唯一领域异常类（构造走 {@link #of} 与具名工厂，不新增叶子类）。
 * <p>
 * websocket 侧的 {@code @MessageExceptionHandler(MessageDomainException.class)} 按本类型路由错误帧：
 * 统一家族的收益在这里运行时可见——任何具名工厂抛出的错误都由同一处理器兜住。
 */
@Getter
public class MessageDomainException extends BaseBusinessException {

    protected MessageDomainException(String message) {
        super(message);
    }

    protected MessageDomainException(IResultCode resultCode) {
        super(resultCode);
    }

    protected MessageDomainException(IResultCode resultCode, String message) {
        super(resultCode, message);
    }

    @Override
    protected String defaultCode() {
        return MessageResultCode.MESSAGE_DOMAIN_ERROR.getCode();
    }

    public static MessageDomainException of(String message) {
        return new MessageDomainException(message);
    }

    /** 消息不存在 — 消息带 id 便于定位。 */
    public static MessageDomainException notFound(String messageId) {
        return new MessageDomainException(MessageResultCode.MESSAGE_NOT_FOUND, "消息不存在: id=" + messageId);
    }

    /** 非消息参与方的越权操作 — 文案由调用场景决定。 */
    public static MessageDomainException notOwner(String message) {
        return new MessageDomainException(MessageResultCode.MESSAGE_NOT_OWNER, message);
    }
}
