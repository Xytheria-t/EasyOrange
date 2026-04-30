package com.cartethyia.easyorange.message.domain.exception;

import com.cartethyia.easyorange.message.enums.MessageResultCode;
import lombok.Getter;

@Getter
public class UnauthorizedOperationException extends MessageDomainException {

    public UnauthorizedOperationException(String message) {
        super(MessageResultCode.MESSAGE_NOT_OWNER, message);
    }
}
