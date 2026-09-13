package com.cartethyia.easyorange.test;

import static org.assertj.core.api.Assertions.assertThat;

import com.cartethyia.easyorange.message.application.command.MessageCommandHandler;
import com.cartethyia.easyorange.message.application.command.SendSystemMessageCommand;
import com.cartethyia.easyorange.message.application.port.query.MessageQueryRepository;
import com.cartethyia.easyorange.message.domain.aggregate.Message;
import com.cartethyia.easyorange.message.domain.aggregate.OfflineMessage;
import com.cartethyia.easyorange.message.domain.repository.OfflineMessageRepository;
import com.cartethyia.easyorange.message.domain.valueobject.MessageQuery;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 站内信落库集成测试 —— 兜住全 Mockito 单测覆盖不到的 SQL 语义：{@code eo_message.id} / {@code eo_offline_message.id}
 * 为 {@code IdType.INPUT}（数据库不回填），主键必须由应用层装配前生成，否则 insert 直接报
 * 「Column 'id' cannot be null」，且异常被消费者的兜底逻辑吞掉、接口照常返回成功。
 * <p>
 * 接收方在无 WebSocket 会话的测试环境下恒为离线，故一次系统消息发送同时覆盖消息落库与离线行落库两条路径。
 */
@DisplayName("站内信落库集成测试（真实 MySQL）")
class MessagePersistenceIT extends AbstractIntegrationTest {

    private static final String TITLE = "IT 系统通知";

    @Autowired
    private MessageCommandHandler messageCommandHandler;

    @Autowired
    private MessageQueryRepository messageQueryRepository;

    @Autowired
    private OfflineMessageRepository offlineMessageRepository;

    @Test
    @DisplayName("系统消息落库：消息行与离线行均带应用层生成的 UUID v7 主键")
    void systemMessage_persistedWithGeneratedId() {
        String receiverId = UUID.randomUUID().toString();

        messageCommandHandler.handle(new SendSystemMessageCommand(receiverId, TITLE, "集成测试内容", null));

        var page = messageQueryRepository.findByReceiverId(new MessageQuery(1, 10, null, null), receiverId);
        assertThat(page.records()).as("系统消息必须落库").hasSize(1);

        Message saved = page.records().getFirst();
        assertThat(saved.id()).as("主键由应用层生成").isNotBlank();
        assertThat(UUID.fromString(saved.id()).version()).as("主键为 UUID v7").isEqualTo(7);
        assertThat(saved.receiverId()).isEqualTo(receiverId);
        assertThat(saved.title()).isEqualTo(TITLE);

        List<OfflineMessage> pending = offlineMessageRepository.findPendingByUserId(receiverId);
        assertThat(pending).as("接收方离线时写入离线行").hasSize(1);
        assertThat(pending.getFirst().id()).as("离线行主键由应用层生成").isNotBlank();
        assertThat(pending.getFirst().messageId()).as("离线行指向刚落库的消息").isEqualTo(saved.id());
    }
}
