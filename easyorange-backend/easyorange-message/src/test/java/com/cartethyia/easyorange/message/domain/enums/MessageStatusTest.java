package com.cartethyia.easyorange.message.domain.enums;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MessageStatus 枚举测试")
class MessageStatusTest {

    @Test
    @DisplayName("只保留实际写入的两个状态（已读态由 is_read 列承载）")
    void values_containOnlyWrittenStates() {
        assertThat(MessageStatus.values()).containsExactlyInAnyOrder(MessageStatus.SENT, MessageStatus.RECALLED);
    }

    @Test
    @DisplayName("fromCode 正确映射 String 类型 code")
    void fromCode_stringCode_returnsCorrectEnum() {
        assertThat(MessageStatus.fromCode("SENT")).isEqualTo(MessageStatus.SENT);
        assertThat(MessageStatus.fromCode("RECALLED")).isEqualTo(MessageStatus.RECALLED);
    }

    @Test
    @DisplayName("fromCode 抛出异常当 code 不存在或为 null")
    void fromCode_unknownCode_throwsException() {
        assertThatThrownBy(() -> MessageStatus.fromCode("UNKNOWN")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MessageStatus.fromCode(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("已删除的取值（未读 / 已读 / 已送达）不再被接受")
    void fromCode_removedCodes_rejected() {
        assertThatThrownBy(() -> MessageStatus.fromCode("UNREAD")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MessageStatus.fromCode("READ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MessageStatus.fromCode("DELIVERED")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("getCode 返回正确类型")
    void getCode_returnsCorrectType() {
        assertThat(MessageStatus.SENT.getCode()).isEqualTo("SENT");
        assertThat(MessageStatus.RECALLED.getCode()).isEqualTo("RECALLED");
    }
}
