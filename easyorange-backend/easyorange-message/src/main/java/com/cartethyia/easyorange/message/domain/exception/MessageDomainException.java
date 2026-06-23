package com.cartethyia.easyorange.message.domain.exception;

import com.cartethyia.easyorange.common.exception.BaseBusinessException;
import com.cartethyia.easyorange.common.enums.IResultCode;
import com.cartethyia.easyorange.message.enums.MessageResultCode;
import lombok.Getter;

@Getter
public class MessageDomainException extends BaseBusinessException {

    public MessageDomainException(String message) {
        super(message);
    }

    protected MessageDomainException(IResultCode resultCode) {
        super(resultCode);
    }

    protected MessageDomainException(IResultCode resultCode, String message) {
        super(resultCode, message);
    }

    public static MessageDomainException of(String message) {
        return new MessageDomainException(message);
    }

    @Override
    protected String defaultCode() {
        return MessageResultCode.MESSAGE_DOMAIN_ERROR.getCode();
    }
}
