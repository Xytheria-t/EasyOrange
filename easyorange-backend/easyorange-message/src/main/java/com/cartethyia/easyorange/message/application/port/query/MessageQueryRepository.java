package com.cartethyia.easyorange.message.application.port.query;

import com.cartethyia.easyorange.common.result.PageResult;
import com.cartethyia.easyorange.message.domain.aggregate.Message;
import com.cartethyia.easyorange.message.domain.valueobject.MessageQuery;
import com.cartethyia.easyorange.message.domain.valueobject.UnreadCount;
import java.util.List;
import java.util.Map;

public interface MessageQueryRepository {

    Message findById(String id);

    PageResult<Message> findByReceiverId(MessageQuery query, String userId);

    PageResult<Message> findUnreadByReceiverId(MessageQuery query, String userId);

    UnreadCount countUnreadByReceiverId(String userId);

    /** 两个用户之间的近期消息（create_time 升序，最新 500 条窗口）——会话详情。 */
    List<Message> findConversation(String userId, String otherUserId);

    /** 每个会话对方的最新一条消息（create_time 降序，库端聚合）——会话列表。 */
    List<Message> findLatestPerConversation(String userId);

    /** 按会话对方聚合的未读数：key 为对方用户 ID（系统消息归并为 "system"）。 */
    Map<String, Integer> countUnreadByConversation(String userId);
}
