package com.cartethyia.easyorange.message.domain.repository.query;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.message.domain.aggregate.MessageAggregate;
import com.cartethyia.easyorange.message.domain.valueobject.MessageQuery;
import com.cartethyia.easyorange.message.domain.valueobject.UnreadCount;

public interface MessageQueryRepository {

    MessageAggregate findById(String id);

    PageResult<MessageAggregate> findByReceiverId(MessageQuery query, String userId);

    PageResult<MessageAggregate> findUnreadByReceiverId(MessageQuery query, String userId);

    UnreadCount countUnreadByReceiverId(String userId);
}
