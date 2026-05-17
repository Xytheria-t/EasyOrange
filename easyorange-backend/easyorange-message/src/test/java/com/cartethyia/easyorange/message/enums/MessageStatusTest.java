package com.cartethyia.easyorange.message.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MessageStatus 枚举测试")
class MessageStatusTest {

    @Test
    @DisplayName("fromCode 正确映射 Integer 类型 code")
    void fromCode_integerCode_returnsCorrectEnum() {
        assertThat(MessageStatus.fromCode(0)).isEqualTo(MessageStatus.UNREAD);
        assertThat(MessageStatus.fromCode(1)).isEqualTo(MessageStatus.READ);
    }

    @Test
    @DisplayName("fromCode 返回 null 当 code 不存在")
    void fromCode_unknownCode_returnsNull() {
        assertThat(MessageStatus.fromCode(999)).isNull();
    }

    @Test
    @DisplayName("fromStringCode 正确映射 String 类型 code")
    void fromStringCode_stringCode_returnsCorrectEnum() {
        assertThat(MessageStatus.fromStringCode("SENT")).isEqualTo(MessageStatus.SENT);
        assertThat(MessageStatus.fromStringCode("DELIVERED")).isEqualTo(MessageStatus.DELIVERED);
        assertThat(MessageStatus.fromStringCode("RECALLED")).isEqualTo(MessageStatus.RECALLED);
    }

    @Test
    @DisplayName("fromStringCode 不区分大小写")
    void fromStringCode_caseInsensitive() {
        assertThat(MessageStatus.fromStringCode("sent")).isEqualTo(MessageStatus.SENT);
        assertThat(MessageStatus.fromStringCode("Recalled")).isEqualTo(MessageStatus.RECALLED);
    }

    @Test
    @DisplayName("fromStringCode 返回 null 当 code 不存在")
    void fromStringCode_unknownCode_returnsNull() {
        assertThat(MessageStatus.fromStringCode("UNKNOWN")).isNull();
    }

    @Test
    @DisplayName("getDescByCode 返回正确描述")
    void getDescByCode_validCode_returnsDesc() {
        assertThat(MessageStatus.getDescByCode(0)).isEqualTo("未读");
        assertThat(MessageStatus.getDescByCode(1)).isEqualTo("已读");
    }

    @Test
    @DisplayName("getDescByCode 返回未知状态描述")
    void getDescByCode_unknownCode_returnsUnknown() {
        assertThat(MessageStatus.getDescByCode(999)).isEqualTo("未知状态");
    }

    @Test
    @DisplayName("getCode 返回正确类型")
    void getCode_returnsCorrectType() {
        assertThat(MessageStatus.UNREAD.<Integer>getCode()).isEqualTo(0);
        assertThat(MessageStatus.SENT.<String>getCode()).isEqualTo("SENT");
    }
}
