package com.cartethyia.easyorange.message.adapter.outbound.persistence.typehandler;

import com.cartethyia.easyorange.framework.mybatis.CodeEnumTypeHandler;
import com.cartethyia.easyorange.message.enums.MessageType;
import org.apache.ibatis.type.MappedTypes;

@MappedTypes(MessageType.class)
@SuppressWarnings("unused")
public class MessageTypeTypeHandler extends CodeEnumTypeHandler<MessageType> {

    public MessageTypeTypeHandler() {
        super(MessageType::getCode, MessageType::fromCode);
    }
}
