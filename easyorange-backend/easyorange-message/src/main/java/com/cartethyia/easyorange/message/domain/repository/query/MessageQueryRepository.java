package com.cartethyia.easyorange.message.domain.repository.query;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.message.domain.aggregate.MessageAggregate;
import com.cartethyia.easyorange.message.adapter.inbound.web.dto.request.QueryMessageRequest;
import com.cartethyia.easyorange.message.application.query.dto.UnreadCountVO;

public interface MessageQueryRepository {

    MessageAggregate findById(String id);

    PageResult<MessageAggregate> findByReceiverId(QueryMessageRequest request, String userId);

    PageResult<MessageAggregate> findUnreadByReceiverId(QueryMessageRequest request, String userId);

    UnreadCountVO countUnreadByReceiverId(String userId);
}
