package com.cartethyia.easyorange.message.domain.repository.query;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.message.dto.request.QueryMessageRequest;
import com.cartethyia.easyorange.message.dto.vo.MessageVO;
import com.cartethyia.easyorange.message.dto.vo.UnreadCountVO;
import com.cartethyia.easyorange.message.entity.Message;

import java.util.List;
import java.util.Map;

public interface MessageQueryRepository {

    Message findById(Long id);

    PageResult<Message> findByReceiverId(QueryMessageRequest request, Long userId);

    PageResult<Message> findUnreadByReceiverId(QueryMessageRequest request, Long userId);

    UnreadCountVO countUnreadByReceiverId(Long userId);
}
