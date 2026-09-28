package com.cartethyia.easyorange.message.domain.aggregate;

import static org.assertj.core.api.Assertions.assertThat;

import com.cartethyia.easyorange.message.domain.enums.PushStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 离线消息聚合根 —— 纯领域单元测试
 * <p>
 * 覆盖工厂方法与唯一一条状态迁移（PENDING → PUSHED）；重试语义已随机制删除一并移除测试。
 */
@DisplayName("OfflineMessage — 离线消息聚合根")
class OfflineMessageTest {

    // ==================== Factory: create ====================

    @Test
    @DisplayName("create 应设置 PENDING 状态，id 由调用方（应用层 IdGenerator）传入")
    void create_shouldSetPendingState() {
        var aggregate = OfflineMessage.create("id-1", "u001", "m001", "websocket");

        assertThat(aggregate.id()).isEqualTo("id-1");
        assertThat(aggregate.userId()).isEqualTo("u001");
        assertThat(aggregate.messageId()).isEqualTo("m001");
        assertThat(aggregate.pushChannel()).isEqualTo("websocket");
        assertThat(aggregate.pushStatus()).isEqualTo(PushStatus.PENDING);
    }

    // ==================== Factory: fromRaw ====================

    @Test
    @DisplayName("fromRaw 应正确重建所有字段")
    void fromRaw_shouldReconstructAllFields() {
        var aggregate = OfflineMessage.fromRaw("id-1", "u001", "m001", "websocket", PushStatus.PUSHED);

        assertThat(aggregate.id()).isEqualTo("id-1");
        assertThat(aggregate.userId()).isEqualTo("u001");
        assertThat(aggregate.messageId()).isEqualTo("m001");
        assertThat(aggregate.pushChannel()).isEqualTo("websocket");
        assertThat(aggregate.pushStatus()).isEqualTo(PushStatus.PUSHED);
    }

    // ==================== State Transitions: markAsPushed ====================

    @Test
    @DisplayName("markAsPushed 应将状态设为 PUSHED，不变字段保持不变")
    void markAsPushed_shouldChangeStatus() {
        var aggregate = OfflineMessage.create("id-1", "u001", "m001", "websocket");

        var pushed = aggregate.markAsPushed();

        assertThat(pushed.pushStatus()).isEqualTo(PushStatus.PUSHED);
        assertThat(pushed.id()).isEqualTo(aggregate.id());
        assertThat(pushed.userId()).isEqualTo(aggregate.userId());
        assertThat(pushed.messageId()).isEqualTo(aggregate.messageId());
        assertThat(pushed.pushChannel()).isEqualTo(aggregate.pushChannel());
        // 不可变：原对象不受影响
        assertThat(aggregate.pushStatus()).isEqualTo(PushStatus.PENDING);
    }
}
