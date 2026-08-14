package com.cartethyia.easyorange.message.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.message.application.port.query.MessageQueryRepository;
import com.cartethyia.easyorange.message.application.query.dto.ConversationListVO;
import com.cartethyia.easyorange.message.application.query.dto.ConversationVO;
import com.cartethyia.easyorange.message.domain.aggregate.Message;
import com.cartethyia.easyorange.message.domain.enums.MessageStatus;
import com.cartethyia.easyorange.message.domain.enums.MessageType;
import com.cartethyia.easyorange.message.domain.enums.ReadStatus;
import com.cartethyia.easyorange.message.domain.port.UserInfoPort;
import com.cartethyia.easyorange.message.domain.valueobject.UserInfo;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConversationQueryHandler 单元测试")
class ConversationQueryHandlerTest {

    @Mock
    private MessageQueryRepository queryRepository;

    @Mock
    private UserInfoPort userInfoPort;

    @InjectMocks
    private ConversationQueryHandler handler;

    private static final String CURRENT_USER_ID = "1";
    private static final String OTHER_USER_ID = "2";
    private static final String THIRD_USER_ID = "3";

    private Message createMessage(String senderId, String receiverId, String content, String id) {
        return Message.fromRaw(
                id,
                senderId,
                receiverId,
                MessageType.CHAT,
                "",
                content,
                ReadStatus.UNREAD,
                null,
                null,
                MessageStatus.SENT,
                null,
                LocalDateTime.now());
    }

    @Nested
    @DisplayName("getConversation")
    class GetConversationTests {

        @Test
        @DisplayName("返回两个用户之间的消息")
        void getConversation_returnsMessages() {
            Message msg1 = createMessage(CURRENT_USER_ID, OTHER_USER_ID, "你好", "1");
            Message msg2 = createMessage(OTHER_USER_ID, CURRENT_USER_ID, "嗨", "2");

            when(queryRepository.findConversation(CURRENT_USER_ID, OTHER_USER_ID))
                    .thenReturn(List.of(msg1, msg2));
            when(userInfoPort.getUserInfoMap(any()))
                    .thenReturn(Map.of(
                            CURRENT_USER_ID, new UserInfo(CURRENT_USER_ID, "当前用户", null),
                            OTHER_USER_ID, new UserInfo(OTHER_USER_ID, "对方用户", "avatar.jpg")));

            List<ConversationVO> result = handler.getConversation(CURRENT_USER_ID, OTHER_USER_ID);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getContent()).isEqualTo("你好");
            assertThat(result.get(0).getSenderId()).isEqualTo(CURRENT_USER_ID);
            assertThat(result.get(0).getReceiverId()).isEqualTo(OTHER_USER_ID);
        }

        @Test
        @DisplayName("没有消息时返回空列表")
        void getConversation_noMessages_returnsEmpty() {
            when(queryRepository.findConversation(anyString(), anyString())).thenReturn(List.of());

            List<ConversationVO> result = handler.getConversation(CURRENT_USER_ID, OTHER_USER_ID);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getConversations")
    class GetConversationsTests {

        @Test
        @DisplayName("返回会话列表，按最新消息分组")
        void getConversations_returnsGroupedList() {
            Message msgWithUser2 = createMessage(CURRENT_USER_ID, OTHER_USER_ID, "最后一条给2", "3");
            Message msgFromUser2 = createMessage(OTHER_USER_ID, CURRENT_USER_ID, "消息from2", "2");
            Message msgWithUser3 = createMessage(THIRD_USER_ID, CURRENT_USER_ID, "消息from3", "1");

            when(queryRepository.findRecentForUser(CURRENT_USER_ID))
                    .thenReturn(List.of(msgWithUser2, msgFromUser2, msgWithUser3));
            when(userInfoPort.getUserInfoMap(any()))
                    .thenReturn(new HashMap<>(Map.of(
                            OTHER_USER_ID, new UserInfo(OTHER_USER_ID, "用户2", "a.jpg"),
                            THIRD_USER_ID, new UserInfo(THIRD_USER_ID, "用户3", "b.jpg"))));

            List<ConversationListVO> result = handler.getConversations(CURRENT_USER_ID);

            assertThat(result).hasSize(2);
            ConversationListVO convWithUser2 = result.stream()
                    .filter(c -> c.getTargetUserId().equals(OTHER_USER_ID))
                    .findFirst()
                    .orElseThrow();
            ConversationListVO convWithUser3 = result.stream()
                    .filter(c -> c.getTargetUserId().equals(THIRD_USER_ID))
                    .findFirst()
                    .orElseThrow();

            assertThat(convWithUser2.getTargetUserName()).isEqualTo("用户2");
            assertThat(convWithUser3.getTargetUserName()).isEqualTo("用户3");
        }

        @Test
        @DisplayName("系统消息（senderId 为 null）不 NPE 并归并到 system 会话")
        void getConversations_systemMessage_noNpe() {
            Message sysMsg = Message.fromRaw(
                    "5",
                    null,
                    CURRENT_USER_ID,
                    MessageType.SYSTEM,
                    "系统通知",
                    "订单已支付",
                    ReadStatus.UNREAD,
                    null,
                    null,
                    null,
                    null,
                    LocalDateTime.now());
            Message chatMsg = createMessage(OTHER_USER_ID, CURRENT_USER_ID, "嗨", "2");

            when(queryRepository.findRecentForUser(CURRENT_USER_ID)).thenReturn(List.of(sysMsg, chatMsg));
            when(userInfoPort.getUserInfoMap(any()))
                    .thenReturn(Map.of(OTHER_USER_ID, new UserInfo(OTHER_USER_ID, "用户2", null)));

            List<ConversationListVO> result = handler.getConversations(CURRENT_USER_ID);

            assertThat(result).hasSize(2);
            ConversationListVO systemConv = result.stream()
                    .filter(c -> c.getTargetUserId().equals("system"))
                    .findFirst()
                    .orElseThrow();
            assertThat(systemConv.getTargetUserName()).isEqualTo("系统通知");
            assertThat(systemConv.getUnreadCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("没有会话时返回空列表")
        void getConversations_noMessages_returnsEmpty() {
            when(queryRepository.findRecentForUser(CURRENT_USER_ID)).thenReturn(List.of());

            List<ConversationListVO> result = handler.getConversations(CURRENT_USER_ID);

            assertThat(result).isEmpty();
        }
    }
}
