package com.cartethyia.easyorange.message.application.query;

import com.cartethyia.easyorange.framework.util.TestSecurityUtil;
import com.cartethyia.easyorange.message.application.query.dto.ConversationListVO;
import com.cartethyia.easyorange.message.application.query.dto.ConversationVO;
import com.cartethyia.easyorange.message.domain.aggregate.Message;
import com.cartethyia.easyorange.message.domain.port.UserInfoPort;
import com.cartethyia.easyorange.message.domain.repository.query.MessageQueryRepository;
import com.cartethyia.easyorange.message.domain.valueobject.UserInfo;
import com.cartethyia.easyorange.message.enums.MessageStatus;
import com.cartethyia.easyorange.message.enums.ReadStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

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
                id, senderId, receiverId, 2, "", content,
                ReadStatus.UNREAD, null, null,
                MessageStatus.SENT, null, LocalDateTime.now());
    }

    @Nested
    @DisplayName("getConversation")
    class GetConversationTests {

        @Test
        @DisplayName("返回两个用户之间的消息")
        void getConversation_returnsMessages() {
            Message msg1 = createMessage(CURRENT_USER_ID, OTHER_USER_ID, "你好", "1");
            Message msg2 = createMessage(OTHER_USER_ID, CURRENT_USER_ID, "嗨", "2");

            when(queryRepository.findConversation(CURRENT_USER_ID, OTHER_USER_ID)).thenReturn(List.of(msg1, msg2));
            when(userInfoPort.getUserInfoMap(any()))
                    .thenReturn(Map.of(
                            CURRENT_USER_ID, new UserInfo(CURRENT_USER_ID, "当前用户", null),
                            OTHER_USER_ID, new UserInfo(OTHER_USER_ID, "对方用户", "avatar.jpg")
                    ));

            TestSecurityUtil.setSecurityContext(CURRENT_USER_ID);
            try {
                List<ConversationVO> result = handler.getConversation(OTHER_USER_ID);

                assertThat(result).hasSize(2);
                assertThat(result.get(0).getContent()).isEqualTo("你好");
                assertThat(result.get(0).getSenderId()).isEqualTo(CURRENT_USER_ID);
                assertThat(result.get(0).getReceiverId()).isEqualTo(OTHER_USER_ID);
            } finally {
                TestSecurityUtil.clearSecurityContext();
            }
        }

        @Test
        @DisplayName("没有消息时返回空列表")
        void getConversation_noMessages_returnsEmpty() {
            when(queryRepository.findConversation(anyString(), anyString())).thenReturn(List.of());

            TestSecurityUtil.setSecurityContext(CURRENT_USER_ID);
            try {
                List<ConversationVO> result = handler.getConversation(OTHER_USER_ID);

                assertThat(result).isEmpty();
            } finally {
                TestSecurityUtil.clearSecurityContext();
            }
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
                            THIRD_USER_ID, new UserInfo(THIRD_USER_ID, "用户3", "b.jpg")
                    )));

            TestSecurityUtil.setSecurityContext(CURRENT_USER_ID);
            try {
                List<ConversationListVO> result = handler.getConversations();

                assertThat(result).hasSize(2);
                ConversationListVO convWithUser2 = result.stream()
                        .filter(c -> c.getTargetUserId().equals(OTHER_USER_ID))
                        .findFirst().orElseThrow();
                ConversationListVO convWithUser3 = result.stream()
                        .filter(c -> c.getTargetUserId().equals(THIRD_USER_ID))
                        .findFirst().orElseThrow();

                assertThat(convWithUser2.getTargetUserName()).isEqualTo("用户2");
                assertThat(convWithUser3.getTargetUserName()).isEqualTo("用户3");
            } finally {
                TestSecurityUtil.clearSecurityContext();
            }
        }

        @Test
        @DisplayName("系统消息（senderId 为 null）不 NPE 并归并到 system 会话")
        void getConversations_systemMessage_noNpe() {
            Message sysMsg = Message.fromRaw(
                    "5", null, CURRENT_USER_ID, 1, "系统通知", "订单已支付",
                    ReadStatus.UNREAD, null, null, null, null, LocalDateTime.now());
            Message chatMsg = createMessage(OTHER_USER_ID, CURRENT_USER_ID, "嗨", "2");

            when(queryRepository.findRecentForUser(CURRENT_USER_ID)).thenReturn(List.of(sysMsg, chatMsg));
            when(userInfoPort.getUserInfoMap(any()))
                    .thenReturn(Map.of(OTHER_USER_ID, new UserInfo(OTHER_USER_ID, "用户2", null)));

            TestSecurityUtil.setSecurityContext(CURRENT_USER_ID);
            try {
                List<ConversationListVO> result = handler.getConversations();

                assertThat(result).hasSize(2);
                ConversationListVO systemConv = result.stream()
                        .filter(c -> c.getTargetUserId().equals("system"))
                        .findFirst().orElseThrow();
                assertThat(systemConv.getTargetUserName()).isEqualTo("系统通知");
                assertThat(systemConv.getUnreadCount()).isEqualTo(1);
            } finally {
                TestSecurityUtil.clearSecurityContext();
            }
        }

        @Test
        @DisplayName("没有会话时返回空列表")
        void getConversations_noMessages_returnsEmpty() {
            when(queryRepository.findRecentForUser(CURRENT_USER_ID)).thenReturn(List.of());

            TestSecurityUtil.setSecurityContext(CURRENT_USER_ID);
            try {
                List<ConversationListVO> result = handler.getConversations();

                assertThat(result).isEmpty();
            } finally {
                TestSecurityUtil.clearSecurityContext();
            }
        }
    }
}
