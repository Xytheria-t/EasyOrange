package com.cartethyia.easyorange.message.domain.repository.query;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.message.domain.aggregate.Message;
import com.cartethyia.easyorange.message.domain.valueobject.MessageQuery;
import com.cartethyia.easyorange.message.domain.valueobject.UnreadCount;
import java.util.List;

public interface MessageQueryRepository {

    Message findById(String id);

    PageResult<Message> findByReceiverId(MessageQuery query, String userId);

    PageResult<Message> findUnreadByReceiverId(MessageQuery query, String userId);

    UnreadCount countUnreadByReceiverId(String userId);

    /** 两个用户之间的全部消息（create_time 升序）——会话详情。 */
    List<Message> findConversation(String userId, String otherUserId);

    /** 用户参与（收发任一方向）的最近消息（create_time 降序）——会话列表。 */
    List<Message> findRecentForUser(String userId);
}
