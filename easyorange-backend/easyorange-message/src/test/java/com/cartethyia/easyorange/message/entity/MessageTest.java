package com.cartethyia.easyorange.message.entity;

import com.cartethyia.easyorange.message.domain.aggregate.MessageAggregate;
import com.cartethyia.easyorange.message.domain.aggregate.MessageAggregate.MessageCreateResult;
import com.cartethyia.easyorange.message.domain.aggregate.MessageAggregate.MessageReadResult;
import com.cartethyia.easyorange.message.domain.aggregate.MessageAggregate.MessageRecallResult;
import com.cartethyia.easyorange.message.domain.event.MessageDeletedEvent;
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

@DisplayName("MessageAggregate 聚合根单元测试")
class MessageTest {

    private static final String SENDER_ID = "1";
    private static final String RECEIVER_ID = "2";
    private static final String OTHER_USER_ID = "3";

    @Nested
    @DisplayName("create")
    class CreateTests {

        @Test
        @DisplayName("正常创建普通消息")
        void create_validParams_returnsMessage() {
            MessageCreateResult result = MessageAggregate.create(SENDER_ID, RECEIVER_ID, 2, "你好", "hello world", "100");

            assertThat(result.aggregate().senderId()).isEqualTo(SENDER_ID);
            assertThat(result.aggregate().receiverId()).isEqualTo(RECEIVER_ID);
            assertThat(result.aggregate().type()).isEqualTo(2);
            assertThat(result.aggregate().title()).isEqualTo("你好");
            assertThat(result.aggregate().content()).isEqualTo("hello world");
            assertThat(result.aggregate().businessId()).isEqualTo("100");
            assertThat(result.aggregate().isRead()).isEqualTo(MessageStatus.UNREAD.getCode());
        }

        @Test
        @DisplayName("创建消息时 XSS 转义标题和内容")
        void create_xssEscape_titleAndContentEscaped() {
            MessageCreateResult result = MessageAggregate.create(SENDER_ID, RECEIVER_ID, 2,
                    "<script>alert('xss')</script>", "<b>bold</b>", null);

            assertThat(result.aggregate().title()).doesNotContain("<script>");
            assertThat(result.aggregate().title()).contains("&lt;");
            assertThat(result.aggregate().content()).doesNotContain("<b>");
            assertThat(result.aggregate().content()).contains("&lt;b&gt;");
        }
    }

    @Nested
    @DisplayName("createSystem")
    class CreateSystemTests {

        @Test
        @DisplayName("正常创建系统消息")
        void createSystem_validParams_returnsSystemMessage() {
            MessageCreateResult result = MessageAggregate.createSystem(RECEIVER_ID, "系统通知", "您的商品已审核通过", null);

            assertThat(result.aggregate().senderId()).isNull();
            assertThat(result.aggregate().receiverId()).isEqualTo(RECEIVER_ID);
            assertThat(result.aggregate().type()).isEqualTo(1);
            assertThat(result.aggregate().title()).isEqualTo("系统通知");
            assertThat(result.aggregate().content()).isEqualTo("您的商品已审核通过");
            assertThat(result.aggregate().isRead()).isEqualTo(MessageStatus.UNREAD.getCode());
        }

        @Test
        @DisplayName("系统消息 XSS 转义")
        void createSystem_xssEscape_contentEscaped() {
            MessageCreateResult result = MessageAggregate.createSystem(RECEIVER_ID, "<script>alert(1)</script>", "<img onerror='alert(1)'>", null);

            assertThat(result.aggregate().title()).doesNotContain("<script>");
            assertThat(result.aggregate().content()).doesNotContain("<img");
        }
    }

    @Nested
    @DisplayName("send")
    class SendTests {

        @Test
        @DisplayName("send 返回 MessageSentEvent")
        void send_returnsEvent() {
            MessageAggregate aggregate = MessageAggregate.fromRaw(
                    "100", SENDER_ID, RECEIVER_ID, 2, "标题", "内容",
                    MessageStatus.UNREAD.getCode(), null, null,
                    MessageStatus.SENT.getCode(), null, LocalDateTime.now());

            MessageSentEvent event = aggregate.send();

            assertThat(event).isNotNull();
            assertThat(event.senderId()).isEqualTo(SENDER_ID);
            assertThat(event.receiverId()).isEqualTo(RECEIVER_ID);
        }
    }

    @Nested
    @DisplayName("recall")
    class RecallTests {

        @Test
        @DisplayName("2分钟内可以撤回消息")
        void recall_within2Minutes_success() {
            MessageAggregate aggregate = MessageAggregate.fromRaw(
                    "100", SENDER_ID, RECEIVER_ID, 2, "标题", "内容",
                    MessageStatus.UNREAD.getCode(), null, null,
                    MessageStatus.SENT.getCode(), null, LocalDateTime.now().minusMinutes(1));

            MessageRecallResult result = aggregate.recall(SENDER_ID, "conv_1_2");

            assertThat(result.event()).isNotNull();
            assertThat(result.event().messageId()).isEqualTo("100");
            assertThat(result.event().operatorId()).isEqualTo(SENDER_ID);
            assertThat(result.event().conversationId()).isEqualTo("conv_1_2");
            assertThat(result.event().recalledAt()).isNotNull();
        }

        @Test
        @DisplayName("非发送者不能撤回消息")
        void recall_notSender_throws() {
            MessageAggregate aggregate = MessageAggregate.fromRaw(
                    "100", SENDER_ID, RECEIVER_ID, 2, "标题", "内容",
                    MessageStatus.UNREAD.getCode(), null, null,
                    MessageStatus.SENT.getCode(), null, LocalDateTime.now());

            assertThatThrownBy(() -> aggregate.recall(OTHER_USER_ID, "conv_1_2"))
                    .isInstanceOf(UnauthorizedOperationException.class)
                    .hasMessageContaining("不能撤回他人的消息");
        }

        @Test
        @DisplayName("超过2分钟不能撤回消息")
        void recall_over2Minutes_throws() {
            MessageAggregate aggregate = MessageAggregate.fromRaw(
                    "100", SENDER_ID, RECEIVER_ID, 2, "标题", "内容",
                    MessageStatus.UNREAD.getCode(), null, null,
                    MessageStatus.SENT.getCode(), null, LocalDateTime.now().minusMinutes(3));

            assertThatThrownBy(() -> aggregate.recall(SENDER_ID, "conv_1_2"))
                    .isInstanceOf(MessageDomainException.class)
                    .hasMessageContaining("超过可撤回时间");
        }

        @Test
        @DisplayName("已撤回的消息不能再次撤回")
        void recall_alreadyRecalled_throws() {
            MessageAggregate aggregate = MessageAggregate.fromRaw(
                    "100", SENDER_ID, RECEIVER_ID, 2, "标题", "内容",
                    MessageStatus.UNREAD.getCode(), null, null,
                    MessageStatus.RECALLED.getCode(), LocalDateTime.now(), LocalDateTime.now().minusMinutes(1));

            assertThatThrownBy(() -> aggregate.recall(SENDER_ID, "conv_1_2"))
                    .isInstanceOf(MessageDomainException.class)
                    .hasMessageContaining("已被撤回");
        }

        @Test
        @DisplayName("撤回后返回的事件包含正确信息")
        void recall_returnsEventWithCorrectInfo() {
            MessageAggregate aggregate = MessageAggregate.fromRaw(
                    "100", SENDER_ID, RECEIVER_ID, 2, "标题", "内容",
                    MessageStatus.UNREAD.getCode(), null, null,
                    MessageStatus.SENT.getCode(), null, LocalDateTime.now().minusMinutes(1));

            MessageRecallResult result = aggregate.recall(SENDER_ID, "conv_1_2");

            assertThat(result.event()).isNotNull();
            assertThat(result.event().messageId()).isEqualTo("100");
            assertThat(result.event().operatorId()).isEqualTo(SENDER_ID);
            assertThat(result.event().conversationId()).isEqualTo("conv_1_2");
        }
    }

    @Nested
    @DisplayName("read")
    class ReadTests {

        @Test
        @DisplayName("接收者可以标记为已读")
        void read_byReceiver_success() {
            MessageAggregate aggregate = MessageAggregate.fromRaw(
                    "100", SENDER_ID, RECEIVER_ID, 2, "标题", "内容",
                    MessageStatus.UNREAD.getCode(), null, null,
                    MessageStatus.SENT.getCode(), null, LocalDateTime.now());

            MessageReadResult result = aggregate.read(RECEIVER_ID);

            assertThat(result).isNotNull();
            assertThat(result.event().messageId()).isEqualTo("100");
            assertThat(result.event().readerId()).isEqualTo(RECEIVER_ID);
            assertThat(result.aggregate().isRead()).isEqualTo(MessageStatus.READ.getCode());
            assertThat(result.aggregate().readTime()).isNotNull();
        }

        @Test
        @DisplayName("非接收者不能标记已读")
        void read_notReceiver_throws() {
            MessageAggregate aggregate = MessageAggregate.fromRaw(
                    "100", SENDER_ID, RECEIVER_ID, 2, "标题", "内容",
                    MessageStatus.UNREAD.getCode(), null, null,
                    MessageStatus.SENT.getCode(), null, LocalDateTime.now());

            assertThatThrownBy(() -> aggregate.read(OTHER_USER_ID))
                    .isInstanceOf(UnauthorizedOperationException.class)
                    .hasMessageContaining("Only receiver can read");
        }

        @Test
        @DisplayName("已读消息再调用 read 返回 null")
        void read_alreadyRead_returnsNull() {
            MessageAggregate aggregate = MessageAggregate.fromRaw(
                    "100", SENDER_ID, RECEIVER_ID, 2, "标题", "内容",
                    MessageStatus.READ.getCode(), LocalDateTime.now(), null,
                    MessageStatus.SENT.getCode(), null, LocalDateTime.now());

            MessageReadResult result = aggregate.read(RECEIVER_ID);

            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("delete")
    class DeleteTests {

        @Test
        @DisplayName("接收者可以删除消息")
        void delete_byReceiver_success() {
            MessageAggregate aggregate = MessageAggregate.fromRaw(
                    "100", SENDER_ID, RECEIVER_ID, 2, "标题", "内容",
                    MessageStatus.UNREAD.getCode(), null, null,
                    MessageStatus.SENT.getCode(), null, LocalDateTime.now());

            MessageDeletedEvent event = aggregate.delete(RECEIVER_ID);

            assertThat(event).isNotNull();
            assertThat(event.messageId()).isEqualTo("100");
            assertThat(event.deleterId()).isEqualTo(RECEIVER_ID);
        }

        @Test
        @DisplayName("非接收者不能删除消息")
        void delete_notReceiver_throws() {
            MessageAggregate aggregate = MessageAggregate.fromRaw(
                    "100", SENDER_ID, RECEIVER_ID, 2, "标题", "内容",
                    MessageStatus.UNREAD.getCode(), null, null,
                    MessageStatus.SENT.getCode(), null, LocalDateTime.now());

            assertThatThrownBy(() -> aggregate.delete(SENDER_ID))
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
            MessageAggregate aggregate = MessageAggregate.fromRaw(
                    "100", SENDER_ID, RECEIVER_ID, 2, "标题", "内容",
                    MessageStatus.UNREAD.getCode(), null, null,
                    MessageStatus.SENT.getCode(), null, LocalDateTime.now());

            assertThat(aggregate.isUnread()).isTrue();
        }

        @Test
        @DisplayName("已读消息 isUnread 返回 false")
        void isUnread_readMessage_returnsFalse() {
            MessageAggregate aggregate = MessageAggregate.fromRaw(
                    "100", SENDER_ID, RECEIVER_ID, 2, "标题", "内容",
                    MessageStatus.READ.getCode(), LocalDateTime.now(), null,
                    MessageStatus.SENT.getCode(), null, LocalDateTime.now());

            assertThat(aggregate.isUnread()).isFalse();
        }

        @Test
        @DisplayName("isOwnedBy 判断接收者")
        void isOwnedBy_receiver_returnsTrue() {
            MessageAggregate aggregate = MessageAggregate.fromRaw(
                    "100", SENDER_ID, RECEIVER_ID, 2, "标题", "内容",
                    MessageStatus.UNREAD.getCode(), null, null,
                    MessageStatus.SENT.getCode(), null, LocalDateTime.now());

            assertThat(aggregate.isOwnedBy(RECEIVER_ID)).isTrue();
            assertThat(aggregate.isOwnedBy(OTHER_USER_ID)).isFalse();
        }

        @Test
        @DisplayName("isSender 判断发送者")
        void isSender_correctSender_returnsTrue() {
            MessageAggregate aggregate = MessageAggregate.fromRaw(
                    "100", SENDER_ID, RECEIVER_ID, 2, "标题", "内容",
                    MessageStatus.UNREAD.getCode(), null, null,
                    MessageStatus.SENT.getCode(), null, LocalDateTime.now());

            assertThat(aggregate.isSender(SENDER_ID)).isTrue();
            assertThat(aggregate.isSender(RECEIVER_ID)).isFalse();
        }

        @Test
        @DisplayName("系统消息 isSender 返回 false")
        void isSender_systemMessage_returnsFalse() {
            MessageAggregate aggregate = MessageAggregate.fromRaw(
                    "100", null, RECEIVER_ID, 1, "通知", "内容",
                    MessageStatus.UNREAD.getCode(), null, null,
                    null, null, LocalDateTime.now());

            assertThat(aggregate.isSender(SENDER_ID)).isFalse();
            assertThat(aggregate.isSender(null)).isFalse();
        }
    }
}
