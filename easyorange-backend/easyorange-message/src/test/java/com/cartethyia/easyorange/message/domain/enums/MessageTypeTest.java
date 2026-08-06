package com.cartethyia.easyorange.message.domain.enums;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MessageType 枚举测试")
class MessageTypeTest {

    @Test
    @DisplayName("所有枚举值有正确的 code 和 desc")
    void allValues_haveCorrectCodeAndDesc() {
        assertThat(MessageType.SYSTEM.getCode()).isEqualTo("1");
        assertThat(MessageType.SYSTEM.getDesc()).isEqualTo("系统通知");

        assertThat(MessageType.CHAT.getCode()).isEqualTo("2");
        assertThat(MessageType.CHAT.getDesc()).isEqualTo("聊天消息");

        assertThat(MessageType.ORDER.getCode()).isEqualTo("3");
        assertThat(MessageType.ORDER.getDesc()).isEqualTo("订单消息");

        assertThat(MessageType.PAYMENT.getCode()).isEqualTo("4");
        assertThat(MessageType.PAYMENT.getDesc()).isEqualTo("支付消息");

        assertThat(MessageType.ACTIVITY.getCode()).isEqualTo("5");
        assertThat(MessageType.ACTIVITY.getDesc()).isEqualTo("活动通知");
    }

    @Test
    @DisplayName("fromCode 正确映射")
    void fromCode_validCode_returnsCorrectEnum() {
        assertThat(MessageType.fromCode("1")).isEqualTo(MessageType.SYSTEM);
        assertThat(MessageType.fromCode("2")).isEqualTo(MessageType.CHAT);
        assertThat(MessageType.fromCode("3")).isEqualTo(MessageType.ORDER);
        assertThat(MessageType.fromCode("4")).isEqualTo(MessageType.PAYMENT);
        assertThat(MessageType.fromCode("5")).isEqualTo(MessageType.ACTIVITY);
    }

    @Test
    @DisplayName("fromCode 抛出异常当 code 不存在或为 null")
    void fromCode_unknownCode_throwsException() {
        assertThatThrownBy(() -> MessageType.fromCode("999")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MessageType.fromCode(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("getDescByCode 返回正确描述")
    void getDescByCode_validCode_returnsDesc() {
        assertThat(MessageType.getDescByCode("1")).isEqualTo("系统通知");
        assertThat(MessageType.getDescByCode("2")).isEqualTo("聊天消息");
        assertThat(MessageType.getDescByCode("3")).isEqualTo("订单消息");
        assertThat(MessageType.getDescByCode("4")).isEqualTo("支付消息");
        assertThat(MessageType.getDescByCode("5")).isEqualTo("活动通知");
    }

    @Test
    @DisplayName("getDescByCode 返回未知类型")
    void getDescByCode_unknownCode_returnsUnknown() {
        assertThat(MessageType.getDescByCode("999")).isEqualTo("未知类型");
    }
}
