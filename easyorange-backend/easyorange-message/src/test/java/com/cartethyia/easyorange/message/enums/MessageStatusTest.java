package com.cartethyia.easyorange.message.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("MessageStatus 枚举测试")
class MessageStatusTest {

    @Test
    @DisplayName("fromCode 正确映射 String 类型 code")
    void fromCode_stringCode_returnsCorrectEnum() {
        assertThat(MessageStatus.fromCode("UNREAD")).isEqualTo(MessageStatus.UNREAD);
        assertThat(MessageStatus.fromCode("READ")).isEqualTo(MessageStatus.READ);
        assertThat(MessageStatus.fromCode("SENT")).isEqualTo(MessageStatus.SENT);
    }

    @Test
    @DisplayName("fromCode 抛出异常当 code 不存在或为 null")
    void fromCode_unknownCode_throwsException() {
        assertThatThrownBy(() -> MessageStatus.fromCode("UNKNOWN"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MessageStatus.fromCode(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("getDescByCode 返回正确描述")
    void getDescByCode_validCode_returnsDesc() {
        assertThat(MessageStatus.getDescByCode("UNREAD")).isEqualTo("未读");
        assertThat(MessageStatus.getDescByCode("READ")).isEqualTo("已读");
    }

    @Test
    @DisplayName("getDescByCode 返回未知状态描述")
    void getDescByCode_unknownCode_returnsUnknown() {
        assertThat(MessageStatus.getDescByCode("UNKNOWN")).isEqualTo("未知状态");
    }

    @Test
    @DisplayName("getCode 返回正确类型")
    void getCode_returnsCorrectType() {
        assertThat(MessageStatus.UNREAD.getCode()).isEqualTo("UNREAD");
        assertThat(MessageStatus.SENT.getCode()).isEqualTo("SENT");
        assertThat(MessageStatus.READ.getCode()).isEqualTo("READ");
    }
}
