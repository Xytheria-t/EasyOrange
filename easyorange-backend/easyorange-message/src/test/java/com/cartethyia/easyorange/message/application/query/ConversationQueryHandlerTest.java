package com.cartethyia.easyorange.message.application.query;

import com.cartethyia.easyorange.framework.util.TestSecurityUtil;
import com.cartethyia.easyorange.message.adapter.outbound.persistence.MessageMapper;
import com.cartethyia.easyorange.message.domain.port.output.UserInfoPort;
import com.cartethyia.easyorange.message.domain.valueobject.UserInfo;
import com.cartethyia.easyorange.message.dto.vo.ConversationListVO;
import com.cartethyia.easyorange.message.dto.vo.ConversationVO;
import com.cartethyia.easyorange.message.entity.Message;
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
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConversationQueryHandler 单元测试")
class ConversationQueryHandlerTest {

    @Mock
    private MessageMapper messageMapper;

    @Mock
    private UserInfoPort userInfoPort;

    @InjectMocks
    private ConversationQueryHandler handler;

    private static final Long CURRENT_USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long THIRD_USER_ID = 3L;

    private Message createMessage(Long senderId, Long receiverId, String content, Long id) {
        Message msg = Message.create(senderId, receiverId, 2, "", content, null);
        msg.setId(id);
        msg.setCreateTime(LocalDateTime.now());
        return msg;
    }

    @Nested
    @DisplayName("getConversation")
    class GetConversationTests {

        @Test
        @DisplayName("返回两个用户之间的消息")
        void getConversation_returnsMessages() {
            Message msg1 = createMessage(CURRENT_USER_ID, OTHER_USER_ID, "你好", 1L);
            Message msg2 = createMessage(OTHER_USER_ID, CURRENT_USER_ID, "嗨", 2L);

            when(messageMapper.selectList(any())).thenReturn(List.of(msg1, msg2));
            when(userInfoPort.getUserInfoMap(any(Set.class)))
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
            when(messageMapper.selectList(any())).thenReturn(List.of());

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
            Message msgWithUser2 = createMessage(CURRENT_USER_ID, OTHER_USER_ID, "最后一条给2", 3L);
            Message msgFromUser2 = createMessage(OTHER_USER_ID, CURRENT_USER_ID, "消息from2", 2L);
            Message msgWithUser3 = createMessage(THIRD_USER_ID, CURRENT_USER_ID, "消息from3", 1L);

            when(messageMapper.selectList(any())).thenReturn(List.of(msgWithUser2, msgFromUser2, msgWithUser3));
            when(userInfoPort.getUserInfoMap(any(Set.class)))
                    .thenReturn(new HashMap<>(Map.of(
                            CURRENT_USER_ID, new UserInfo(CURRENT_USER_ID, "我", null),
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
        @DisplayName("没有会话时返回空列表")
        void getConversations_noMessages_returnsEmpty() {
            when(messageMapper.selectList(any())).thenReturn(List.of());

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
