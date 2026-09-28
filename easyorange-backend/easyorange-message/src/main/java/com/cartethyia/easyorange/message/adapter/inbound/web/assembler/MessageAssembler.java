package com.cartethyia.easyorange.message.adapter.inbound.web.assembler;

import com.cartethyia.easyorange.message.adapter.inbound.web.dto.request.QueryMessageRequest;
import com.cartethyia.easyorange.message.adapter.inbound.web.dto.request.SendMessageRequest;
import com.cartethyia.easyorange.message.application.command.SendMessageCommand;
import com.cartethyia.easyorange.message.domain.enums.ReadStatus;
import com.cartethyia.easyorange.message.domain.valueobject.MessageQuery;
import org.springframework.stereotype.Component;

/**
 * 消息入参 DTO → 应用层命令 / 领域查询参数的转换。
 * <p>
 * 取舍：手写普通类，映射是逐字段复制，注解处理器只会生成一层空转发。
 * <p>
 * 边界：非法 isRead code 由 {@link ReadStatus#fromCode(String)} 抛 IllegalArgumentException，
 * 交全局异常处理器映射为 400，不在这里吞掉。
 */
@Component
public class MessageAssembler {

    public SendMessageCommand toSendCommand(SendMessageRequest request) {
        return new SendMessageCommand(
                request.receiverId(), request.type(), request.title(), request.content(), request.businessId());
    }

    public MessageQuery toMessageQuery(QueryMessageRequest request) {
        ReadStatus isRead =
                request.getIsRead() != null ? ReadStatus.fromCode(String.valueOf(request.getIsRead())) : null;
        return new MessageQuery(request.getPageNum(), request.getPageSize(), request.getType(), isRead);
    }
}
