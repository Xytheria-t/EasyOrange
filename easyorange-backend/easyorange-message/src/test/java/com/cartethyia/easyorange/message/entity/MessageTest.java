package com.cartethyia.easyorange.message.entity;

import com.cartethyia.easyorange.message.domain.event.MessageDeletedEvent;
import com.cartethyia.easyorange.message.domain.event.MessageReadEvent;
import com.cartethyia.easyorange.message.domain.event.MessageRecalledEvent;
import com.cartethyia.easyorange.message.domain.event.MessageSentEvent;
import com.cartethyia.easyorange.message.domain.exception.MessageDomainException;
import com.cartethyia.easyorange.message.domain.exception.UnauthorizedOperationException;
import com.cartethyia.easyorange.message.enums.MessageStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Message 实体单元测试")
class MessageTest {

    private static final Long SENDER_ID = 1L;
    private static final Long RECEIVER_ID = 2L;
    private static final Long OTHER_USER_ID = 3L;

    @Nested
    @DisplayName("create")
    class CreateTests {

        @Test
        @DisplayName("正常创建普通消息")
        void create_validParams_returnsMessage() {
            Message message = Message.create(SENDER_ID, RECEIVER_ID, 2, "你好", "hello world", 100L);

            assertThat(message.getSenderId()).isEqualTo(SENDER_ID);
            assertThat(message.getReceiverId()).isEqualTo(RECEIVER_ID);
            assertThat(message.getType()).isEqualTo(2);
            assertThat(message.getTitle()).isEqualTo("你好");
            assertThat(message.getContent()).isEqualTo("hello world");
            assertThat(message.getBusinessId()).isEqualTo(100L);
            assertThat(message.getIsRead()).isEqualTo(MessageStatus.UNREAD.getCode());
            // msgStatus is set to "SENT" on creation; verify through recall behavior instead
        }

        @Test
        @DisplayName("创建消息时 XSS 转义标题和内容")
        void create_xssEscape_titleAndContentEscaped() {
            Message message = Message.create(SENDER_ID, RECEIVER_ID, 2,
                    "<script>alert('xss')</script>", "<b>bold</b>", null);

            assertThat(message.getTitle()).doesNotContain("<script>");
            assertThat(message.getTitle()).contains("&lt;");
            assertThat(message.getContent()).doesNotContain("<b>");
            assertThat(message.getContent()).contains("&lt;b&gt;");
        }
    }

    @Nested
    @DisplayName("createSystem")
    class CreateSystemTests {

        @Test
        @DisplayName("正常创建系统消息")
        void createSystem_validParams_returnsSystemMessage() {
            Message message = Message.createSystem(RECEIVER_ID, "系统通知", "您的商品已审核通过", null);

            assertThat(message.getSenderId()).isNull();
            assertThat(message.getReceiverId()).isEqualTo(RECEIVER_ID);
            assertThat(message.getType()).isEqualTo(1);
            assertThat(message.getTitle()).isEqualTo("系统通知");
            assertThat(message.getContent()).isEqualTo("您的商品已审核通过");
            assertThat(message.getIsRead()).isEqualTo(MessageStatus.UNREAD.getCode());
        }

        @Test
        @DisplayName("系统消息 XSS 转义")
        void createSystem_xssEscape_contentEscaped() {
            Message message = Message.createSystem(RECEIVER_ID, "<script>alert(1)</script>", "<img onerror='alert(1)'>", null);

            assertThat(message.getTitle()).doesNotContain("<script>");
            assertThat(message.getContent()).doesNotContain("<img");
        }
    }

    @Nested
    @DisplayName("send")
    class SendTests {

        @Test
        @DisplayName("send 返回 MessageSentEvent")
        void send_returnsEvent() {
            Message message = Message.create(SENDER_ID, RECEIVER_ID, 2, "标题", "内容", null);

            MessageSentEvent event = message.send();

            assertThat(event).isNotNull();
            assertThat(event.getSenderId()).isEqualTo(SENDER_ID);
            assertThat(event.getReceiverId()).isEqualTo(RECEIVER_ID);
        }
    }

    @Nested
    @DisplayName("recall")
    class RecallTests {

        @Test
        @DisplayName("2分钟内可以撤回消息")
        void recall_within2Minutes_success() {
            Message message = Message.create(SENDER_ID, RECEIVER_ID, 2, "标题", "内容", null);
            message.setCreateTime(LocalDateTime.now().minusMinutes(1));
            message.setId(100L);

            MessageRecalledEvent event = message.recall(SENDER_ID, "conv_1_2");

            assertThat(event).isNotNull();
            assertThat(event.getMessageId()).isEqualTo(100L);
            assertThat(event.getOperatorId()).isEqualTo(SENDER_ID);
            assertThat(event.getConversationId()).isEqualTo("conv_1_2");
            assertThat(event.getRecalledAt()).isNotNull();
        }

        @Test
        @DisplayName("非发送者不能撤回消息")
        void recall_notSender_throws() {
            Message message = Message.create(SENDER_ID, RECEIVER_ID, 2, "标题", "内容", null);
            message.setCreateTime(LocalDateTime.now());

            assertThatThrownBy(() -> message.recall(OTHER_USER_ID, "conv_1_2"))
                    .isInstanceOf(UnauthorizedOperationException.class)
                    .hasMessageContaining("不能撤回他人的消息");
        }

        @Test
        @DisplayName("超过2分钟不能撤回消息")
        void recall_over2Minutes_throws() {
            Message message = Message.create(SENDER_ID, RECEIVER_ID, 2, "标题", "内容", null);
            message.setCreateTime(LocalDateTime.now().minusMinutes(3));

            assertThatThrownBy(() -> message.recall(SENDER_ID, "conv_1_2"))
                    .isInstanceOf(MessageDomainException.class)
                    .hasMessageContaining("超过可撤回时间");
        }

        @Test
        @DisplayName("已撤回的消息不能再次撤回")
        void recall_alreadyRecalled_throws() {
            Message message = Message.create(SENDER_ID, RECEIVER_ID, 2, "标题", "内容", null);
            message.setCreateTime(LocalDateTime.now().minusMinutes(1));
            message.recall(SENDER_ID, "conv_1_2");

            assertThatThrownBy(() -> message.recall(SENDER_ID, "conv_1_2"))
                    .isInstanceOf(MessageDomainException.class)
                    .hasMessageContaining("已被撤回");
        }

        @Test
        @DisplayName("撤回后返回的事件包含正确信息")
        void recall_returnsEventWithCorrectInfo() {
            Message message = Message.create(SENDER_ID, RECEIVER_ID, 2, "标题", "内容", null);
            message.setId(100L);
            message.setCreateTime(LocalDateTime.now().minusMinutes(1));

            MessageRecalledEvent event = message.recall(SENDER_ID, "conv_1_2");

            assertThat(event).isNotNull();
            assertThat(event.getMessageId()).isEqualTo(100L);
            assertThat(event.getOperatorId()).isEqualTo(SENDER_ID);
            assertThat(event.getConversationId()).isEqualTo("conv_1_2");
        }
    }

    @Nested
    @DisplayName("read")
    class ReadTests {

        @Test
        @DisplayName("接收者可以标记为已读")
        void read_byReceiver_success() {
            Message message = Message.create(SENDER_ID, RECEIVER_ID, 2, "标题", "内容", null);
            message.setId(100L);

            MessageReadEvent event = message.read(RECEIVER_ID);

            assertThat(event).isNotNull();
            assertThat(event.getMessageId()).isEqualTo(100L);
            assertThat(event.getReaderId()).isEqualTo(RECEIVER_ID);
            assertThat(message.getIsRead()).isEqualTo(MessageStatus.READ.getCode());
            assertThat(message.getReadTime()).isNotNull();
        }

        @Test
        @DisplayName("非接收者不能标记已读")
        void read_notReceiver_throws() {
            Message message = Message.create(SENDER_ID, RECEIVER_ID, 2, "标题", "内容", null);

            assertThatThrownBy(() -> message.read(OTHER_USER_ID))
                    .isInstanceOf(UnauthorizedOperationException.class)
                    .hasMessageContaining("Only receiver can read");
        }

        @Test
        @DisplayName("已读消息再调用 read 返回 null")
        void read_alreadyRead_returnsNull() {
            Message message = Message.create(SENDER_ID, RECEIVER_ID, 2, "标题", "内容", null);
            message.read(RECEIVER_ID);

            MessageReadEvent event = message.read(RECEIVER_ID);

            assertThat(event).isNull();
        }
    }

    @Nested
    @DisplayName("delete")
    class DeleteTests {

        @Test
        @DisplayName("接收者可以删除消息")
        void delete_byReceiver_success() {
            Message message = Message.create(SENDER_ID, RECEIVER_ID, 2, "标题", "内容", null);
            message.setId(100L);

            MessageDeletedEvent event = message.delete(RECEIVER_ID);

            assertThat(event).isNotNull();
            assertThat(event.getMessageId()).isEqualTo(100L);
            assertThat(event.getDeleterId()).isEqualTo(RECEIVER_ID);
        }

        @Test
        @DisplayName("非接收者不能删除消息")
        void delete_notReceiver_throws() {
            Message message = Message.create(SENDER_ID, RECEIVER_ID, 2, "标题", "内容", null);

            assertThatThrownBy(() -> message.delete(SENDER_ID))
                    .isInstanceOf(UnauthorizedOperationException.class)
                    .hasMessageContaining("Not authorized to delete");
        }
    }

    @Nested
    @DisplayName("isUnread / isOwnedBy / isSender")
    class StateCheckTests {

        @Test
        @DisplayName("新建消息未读")
        void isUnread_newMessage_returnsTrue() {
            Message message = Message.create(SENDER_ID, RECEIVER_ID, 2, "标题", "内容", null);

            assertThat(message.isUnread()).isTrue();
        }

        @Test
        @DisplayName("已读消息 isUnread 返回 false")
        void isUnread_readMessage_returnsFalse() {
            Message message = Message.create(SENDER_ID, RECEIVER_ID, 2, "标题", "内容", null);
            message.markAsRead();

            assertThat(message.isUnread()).isFalse();
        }

        @Test
        @DisplayName("isOwnedBy 判断接收者")
        void isOwnedBy_receiver_returnsTrue() {
            Message message = Message.create(SENDER_ID, RECEIVER_ID, 2, "标题", "内容", null);

            assertThat(message.isOwnedBy(RECEIVER_ID)).isTrue();
            assertThat(message.isOwnedBy(OTHER_USER_ID)).isFalse();
        }

        @Test
        @DisplayName("isSender 判断发送者")
        void isSender_correctSender_returnsTrue() {
            Message message = Message.create(SENDER_ID, RECEIVER_ID, 2, "标题", "内容", null);

            assertThat(message.isSender(SENDER_ID)).isTrue();
            assertThat(message.isSender(RECEIVER_ID)).isFalse();
        }

        @Test
        @DisplayName("系统消息 isSender 返回 false")
        void isSender_systemMessage_returnsFalse() {
            Message message = Message.createSystem(RECEIVER_ID, "通知", "内容", null);

            assertThat(message.isSender(SENDER_ID)).isFalse();
            assertThat(message.isSender(null)).isFalse();
        }
    }

    @Nested
    @DisplayName("markAsRead")
    class MarkAsReadTests {

        @Test
        @DisplayName("markAsRead 直接标记已读")
        void markAsRead_setsReadStatus() {
            Message message = Message.create(SENDER_ID, RECEIVER_ID, 2, "标题", "内容", null);

            message.markAsRead();

            assertThat(message.getIsRead()).isEqualTo(MessageStatus.READ.getCode());
            assertThat(message.getReadTime()).isNotNull();
        }
    }
}
