package com.cartethyia.easyorange.message.adapter.outbound.persistence.typehandler;

import com.cartethyia.easyorange.framework.mybatis.CodeEnumTypeHandler;
import com.cartethyia.easyorange.message.enums.MessageStatus;
import org.apache.ibatis.type.MappedTypes;

@MappedTypes(MessageStatus.class)
@SuppressWarnings("unused")
public class MessageStatusTypeHandler extends CodeEnumTypeHandler<MessageStatus> {

    public MessageStatusTypeHandler() {
        super(MessageStatus::getCode, MessageStatus::fromCode);
    }
}
