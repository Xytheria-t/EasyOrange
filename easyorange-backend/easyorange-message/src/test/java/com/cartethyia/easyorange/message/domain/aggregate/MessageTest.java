package com.cartethyia.easyorange.message.domain.aggregate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cartethyia.easyorange.message.domain.enums.MessageStatus;
import com.cartethyia.easyorange.message.domain.enums.MessageType;
import com.cartethyia.easyorange.message.domain.enums.ReadStatus;
import com.cartethyia.easyorange.message.domain.exception.MessageDomainException;
import com.cartethyia.easyorange.message.domain.exception.UnauthorizedOperationException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Message 聚合根单元测试")
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
            Message message = Message.create(SENDER_ID, RECEIVER_ID, MessageType.CHAT, "你好", "hello world", "100");

            assertThat(message.senderId()).isEqualTo(SENDER_ID);
            assertThat(message.receiverId()).isEqualTo(RECEIVER_ID);
            assertThat(message.type()).isEqualTo(MessageType.CHAT);
            assertThat(message.title()).isEqualTo("你好");
            assertThat(message.content()).isEqualTo("hello world");
            assertThat(message.businessId()).isEqualTo("100");
            assertThat(message.isRead()).isEqualTo(ReadStatus.UNREAD);
        }

        @Test
        @DisplayName("标题和内容原样存储，XSS 防护在渲染端文本输出")
        void create_storesRawText_xssHandledAtRender() {
            Message message =
                    Message.create(SENDER_ID, RECEIVER_ID, MessageType.CHAT, "<script>alert('xss')</script>", "<b>bold</b>", null);

            assertThat(message.title()).isEqualTo("<script>alert('xss')</script>");
            assertThat(message.content()).isEqualTo("<b>bold</b>");
        }
    }

    @Nested
    @DisplayName("createSystem")
    class CreateSystemTests {

        @Test
        @DisplayName("正常创建系统消息")
        void createSystem_validParams_returnsSystemMessage() {
            Message message = Message.createSystem(RECEIVER_ID, "系统通知", "您的商品已审核通过", null);

            assertThat(message.senderId()).isNull();
            assertThat(message.receiverId()).isEqualTo(RECEIVER_ID);
            assertThat(message.type()).isEqualTo(MessageType.SYSTEM);
            assertThat(message.title()).isEqualTo("系统通知");
            assertThat(message.content()).isEqualTo("您的商品已审核通过");
            assertThat(message.isRead()).isEqualTo(ReadStatus.UNREAD);
        }

        @Test
        @DisplayName("系统消息标题和内容原样存储")
        void createSystem_storesRawContent() {
            Message message = Message.createSystem(RECEIVER_ID, "<script>alert(1)</script>", "<img onerror='alert(1)'>", null);

            assertThat(message.title()).isEqualTo("<script>alert(1)</script>");
            assertThat(message.content()).isEqualTo("<img onerror='alert(1)'>");
        }
    }

    @Nested
    @DisplayName("recall")
    class RecallTests {

        @Test
        @DisplayName("2分钟内可以撤回消息")
        void recall_within2Minutes_success() {
            Message aggregate = testMessage(LocalDateTime.now().minusMinutes(1));

            var result = aggregate.recall(SENDER_ID, "conv_1_2");

            assertThat(result.event()).isNotNull();
            assertThat(result.event().messageId()).isEqualTo("100");
            assertThat(result.event().operatorId()).isEqualTo(SENDER_ID);
            assertThat(result.event().conversationId()).isEqualTo("conv_1_2");
            assertThat(result.event().recalledAt()).isNotNull();
        }

        @Test
        @DisplayName("非发送者不能撤回消息")
        void recall_notSender_throws() {
            Message aggregate = testMessage(LocalDateTime.now());

            assertThatThrownBy(() -> aggregate.recall(OTHER_USER_ID, "conv_1_2"))
                    .isInstanceOf(UnauthorizedOperationException.class)
                    .hasMessageContaining("不能撤回他人的消息");
        }

        @Test
        @DisplayName("超过2分钟不能撤回消息")
        void recall_over2Minutes_throws() {
            Message aggregate = testMessage(LocalDateTime.now().minusMinutes(3));

            assertThatThrownBy(() -> aggregate.recall(SENDER_ID, "conv_1_2"))
                    .isInstanceOf(MessageDomainException.class)
                    .hasMessageContaining("超过可撤回时间");
        }

        @Test
        @DisplayName("已撤回的消息不能再次撤回")
        void recall_alreadyRecalled_throws() {
            Message aggregate = Message.fromRaw(
                    "100",
                    SENDER_ID,
                    RECEIVER_ID,
                    MessageType.CHAT,
                    "标题",
                    "内容",
                    ReadStatus.UNREAD,
                    null,
                    null,
                    MessageStatus.RECALLED,
                    LocalDateTime.now(),
                    LocalDateTime.now().minusMinutes(1));

            assertThatThrownBy(() -> aggregate.recall(SENDER_ID, "conv_1_2"))
                    .isInstanceOf(MessageDomainException.class)
                    .hasMessageContaining("已被撤回");
        }

        @Test
        @DisplayName("撤回后返回的事件包含正确信息")
        void recall_returnsEventWithCorrectInfo() {
            Message aggregate = testMessage(LocalDateTime.now().minusMinutes(1));

            var result = aggregate.recall(SENDER_ID, "conv_1_2");

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
            Message aggregate = testMessage(LocalDateTime.now());

            Message read = aggregate.read(RECEIVER_ID);

            assertThat(read.isRead()).isEqualTo(ReadStatus.READ);
            assertThat(read.readTime()).isNotNull();
            assertThat(read.id()).isEqualTo("100");
        }

        @Test
        @DisplayName("非接收者不能标记已读")
        void read_notReceiver_throws() {
            Message aggregate = testMessage(LocalDateTime.now());

            assertThatThrownBy(() -> aggregate.read(OTHER_USER_ID))
                    .isInstanceOf(UnauthorizedOperationException.class)
                    .hasMessageContaining("Only receiver can read");
        }

        @Test
        @DisplayName("已读消息再调用 read 幂等返回自身")
        void read_alreadyRead_returnsSame() {
            Message aggregate = Message.fromRaw(
                    "100",
                    SENDER_ID,
                    RECEIVER_ID,
                    MessageType.CHAT,
                    "标题",
                    "内容",
                    ReadStatus.READ,
                    LocalDateTime.now(),
                    null,
                    MessageStatus.SENT,
                    null,
                    LocalDateTime.now());

            Message result = aggregate.read(RECEIVER_ID);

            assertThat(result).isSameAs(aggregate);
        }
    }

    @Nested
    @DisplayName("delete")
    class DeleteTests {

        @Test
        @DisplayName("接收者可以删除消息")
        void delete_byReceiver_doesNotThrow() {
            Message aggregate = testMessage(LocalDateTime.now());

            assertThatCode(() -> aggregate.delete(RECEIVER_ID)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("非接收者不能删除消息")
        void delete_notReceiver_throws() {
            Message aggregate = testMessage(LocalDateTime.now());

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
            Message aggregate = testMessage(LocalDateTime.now());

            assertThat(aggregate.isUnread()).isTrue();
        }

        @Test
        @DisplayName("已读消息 isUnread 返回 false")
        void isUnread_readMessage_returnsFalse() {
            Message aggregate = Message.fromRaw(
                    "100",
                    SENDER_ID,
                    RECEIVER_ID,
                    MessageType.CHAT,
                    "标题",
                    "内容",
                    ReadStatus.READ,
                    LocalDateTime.now(),
                    null,
                    MessageStatus.SENT,
                    null,
                    LocalDateTime.now());

            assertThat(aggregate.isUnread()).isFalse();
        }

        @Test
        @DisplayName("isOwnedBy 判断接收者")
        void isOwnedBy_receiver_returnsTrue() {
            Message aggregate = testMessage(LocalDateTime.now());

            assertThat(aggregate.isOwnedBy(RECEIVER_ID)).isTrue();
            assertThat(aggregate.isOwnedBy(OTHER_USER_ID)).isFalse();
        }

        @Test
        @DisplayName("isSender 判断发送者")
        void isSender_correctSender_returnsTrue() {
            Message aggregate = testMessage(LocalDateTime.now());

            assertThat(aggregate.isSender(SENDER_ID)).isTrue();
            assertThat(aggregate.isSender(RECEIVER_ID)).isFalse();
        }

        @Test
        @DisplayName("系统消息 isSender 返回 false")
        void isSender_systemMessage_returnsFalse() {
            Message aggregate = Message.fromRaw(
                    "100",
                    null,
                    RECEIVER_ID,
                    MessageType.SYSTEM,
                    "通知",
                    "内容",
                    ReadStatus.UNREAD,
                    null,
                    null,
                    null,
                    null,
                    LocalDateTime.now());

            assertThat(aggregate.isSender(SENDER_ID)).isFalse();
            assertThat(aggregate.isSender(null)).isFalse();
        }
    }

    private Message testMessage(LocalDateTime createTime) {
        return Message.fromRaw(
                "100",
                SENDER_ID,
                RECEIVER_ID,
                MessageType.CHAT,
                "标题",
                "内容",
                ReadStatus.UNREAD,
                null,
                null,
                MessageStatus.SENT,
                null,
                createTime);
    }
}
