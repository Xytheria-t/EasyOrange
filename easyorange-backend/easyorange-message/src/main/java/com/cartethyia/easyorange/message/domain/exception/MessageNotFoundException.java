package com.cartethyia.easyorange.message.domain.exception;

import com.cartethyia.easyorange.message.enums.MessageResultCode;
import lombok.Getter;

@Getter
public class MessageNotFoundException extends MessageDomainException {

    public MessageNotFoundException(String messageId) {
        super(MessageResultCode.MESSAGE_NOT_FOUND, "消息不存在: id=" + messageId);
    }
}
