package com.cartethyia.easyorange.message.domain.aggregate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cartethyia.easyorange.message.domain.constant.MessageConstant;
import com.cartethyia.easyorange.message.domain.enums.PushStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 离线消息聚合根 —— 纯领域单元测试
 * <p>
 * 覆盖工厂方法、谓词、状态转换、重试语义四大类场景。
 */
@DisplayName("OfflineMessage — 离线消息聚合根")
class OfflineMessageTest {

    // ==================== Factory: create ====================

    @Test
    @DisplayName("create 应设置默认状态：PENDING、retryCount=0、id=null")
    void create_shouldSetDefaultState() {
        var aggregate = OfflineMessage.create("u001", "m001", "email");

        assertThat(aggregate.id()).isNull();
        assertThat(aggregate.userId()).isEqualTo("u001");
        assertThat(aggregate.messageId()).isEqualTo("m001");
        assertThat(aggregate.pushChannel()).isEqualTo("email");
        assertThat(aggregate.pushStatus()).isEqualTo(PushStatus.PENDING);
        assertThat(aggregate.retryCount()).isEqualTo(MessageConstant.DEFAULT_RETRY_COUNT);
        assertThat(aggregate.maxRetryCount()).isEqualTo(MessageConstant.DEFAULT_MAX_RETRY_COUNT);
    }

    // ==================== Factory: fromRaw ====================

    @Test
    @DisplayName("fromRaw 应正确重建所有字段")
    void fromRaw_shouldReconstructAllFields() {
        var aggregate = OfflineMessage.fromRaw("id-1", "u001", "m001", "sms", PushStatus.PUSHED, 2, 5);

        assertThat(aggregate.id()).isEqualTo("id-1");
        assertThat(aggregate.userId()).isEqualTo("u001");
        assertThat(aggregate.messageId()).isEqualTo("m001");
        assertThat(aggregate.pushChannel()).isEqualTo("sms");
        assertThat(aggregate.pushStatus()).isEqualTo(PushStatus.PUSHED);
        assertThat(aggregate.retryCount()).isEqualTo(2);
        assertThat(aggregate.maxRetryCount()).isEqualTo(5);
    }

    // ==================== Predicate: isPending ====================

    @Test
    @DisplayName("isPending — PENDING 状态应返回 true")
    void isPending_whenStatusIsPending_shouldReturnTrue() {
        var aggregate = OfflineMessage.create("u001", "m001", "email");

        assertThat(aggregate.isPending()).isTrue();
    }

    @Test
    @DisplayName("isPending — 非 PENDING 状态应返回 false")
    void isPending_whenStatusIsNotPending_shouldReturnFalse() {
        var pending = OfflineMessage.create("u001", "m001", "email");

        var pushed = pending.markAsPushed();
        assertThat(pushed.isPending()).isFalse();

        var failed = pending.markAsFailed();
        assertThat(failed.isPending()).isFalse();
    }

    // ==================== Predicate: canRetry ====================

    @Test
    @DisplayName("canRetry — retryCount 小于 maxRetryCount 应返回 true")
    void canRetry_whenUnderMax_shouldReturnTrue() {
        var aggregate =
                OfflineMessage.fromRaw("id-1", "u001", "m001", "email", PushStatus.FAILED, 0, 3);

        assertThat(aggregate.canRetry()).isTrue();
    }

    @Test
    @DisplayName("canRetry — retryCount 等于 maxRetryCount 应返回 false")
    void canRetry_whenAtMax_shouldReturnFalse() {
        var aggregate =
                OfflineMessage.fromRaw("id-1", "u001", "m001", "email", PushStatus.FAILED, 3, 3);

        assertThat(aggregate.canRetry()).isFalse();
    }

    // ==================== State Transitions: markAsPushed ====================

    @Test
    @DisplayName("markAsPushed 应将状态设为 PUSHED，retryCount 不变")
    void markAsPushed_shouldChangeStatus() {
        var aggregate = OfflineMessage.create("u001", "m001", "email");
        var pushed = aggregate.markAsPushed();

        assertThat(pushed.pushStatus()).isEqualTo(PushStatus.PUSHED);
        assertThat(pushed.retryCount()).isEqualTo(aggregate.retryCount());
        // 不变字段应保持不变
        assertThat(pushed.id()).isEqualTo(aggregate.id());
        assertThat(pushed.userId()).isEqualTo(aggregate.userId());
        assertThat(pushed.messageId()).isEqualTo(aggregate.messageId());
        assertThat(pushed.pushChannel()).isEqualTo(aggregate.pushChannel());
        assertThat(pushed.maxRetryCount()).isEqualTo(aggregate.maxRetryCount());
        // 原始对象不受影响（不可变性）
        assertThat(aggregate.pushStatus()).isEqualTo(PushStatus.PENDING);
    }

    // ==================== State Transitions: markAsFailed ====================

    @Test
    @DisplayName("markAsFailed 应将状态设为 FAILED，retryCount 不变")
    void markAsFailed_shouldChangeStatus() {
        var aggregate = OfflineMessage.create("u001", "m001", "email");
        var failed = aggregate.markAsFailed();

        assertThat(failed.pushStatus()).isEqualTo(PushStatus.FAILED);
        assertThat(failed.retryCount()).isEqualTo(aggregate.retryCount());
        // 不变字段应保持不变
        assertThat(failed.id()).isEqualTo(aggregate.id());
        assertThat(failed.userId()).isEqualTo(aggregate.userId());
        assertThat(failed.messageId()).isEqualTo(aggregate.messageId());
        assertThat(failed.pushChannel()).isEqualTo(aggregate.pushChannel());
        assertThat(failed.maxRetryCount()).isEqualTo(aggregate.maxRetryCount());
        // 原始对象不受影响
        assertThat(aggregate.pushStatus()).isEqualTo(PushStatus.PENDING);
    }

    // ==================== State Transitions: incrementRetry ====================

    @Test
    @DisplayName("incrementRetry 应增加 retryCount 且状态不变")
    void incrementRetry_shouldIncreaseCount() {
        var aggregate = OfflineMessage.create("u001", "m001", "email");
        var retried = aggregate.incrementRetry();

        assertThat(retried.retryCount()).isEqualTo(aggregate.retryCount() + 1);
        assertThat(retried.pushStatus()).isEqualTo(aggregate.pushStatus());
        // 不变字段应保持不变
        assertThat(retried.id()).isEqualTo(aggregate.id());
        assertThat(retried.userId()).isEqualTo(aggregate.userId());
        assertThat(retried.messageId()).isEqualTo(aggregate.messageId());
        assertThat(retried.pushChannel()).isEqualTo(aggregate.pushChannel());
        assertThat(retried.maxRetryCount()).isEqualTo(aggregate.maxRetryCount());
        // 原始对象不受影响
        assertThat(aggregate.retryCount()).isEqualTo(MessageConstant.DEFAULT_RETRY_COUNT);
    }

    @Test
    @DisplayName("incrementRetry 达上限时抛异常")
    void incrementRetry_atMax_shouldThrow() {
        var aggregate =
                OfflineMessage.fromRaw("id-1", "u001", "m001", "email", PushStatus.FAILED, 3, 3);

        assertThatThrownBy(aggregate::incrementRetry).isInstanceOf(IllegalStateException.class);
    }
}
