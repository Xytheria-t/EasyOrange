package com.cartethyia.easyorange.message.domain.repository.query;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.message.domain.aggregate.Message;
import com.cartethyia.easyorange.message.domain.valueobject.MessageQuery;
import com.cartethyia.easyorange.message.domain.valueobject.UnreadCount;

public interface MessageQueryRepository {

    Message findById(String id);

    PageResult<Message> findByReceiverId(MessageQuery query, String userId);

    PageResult<Message> findUnreadByReceiverId(MessageQuery query, String userId);

    UnreadCount countUnreadByReceiverId(String userId);
}
