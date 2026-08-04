package com.cartethyia.easyorange.common.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("BusinessType / BaseCodeEnum 测试")
class BusinessTypeTest {

    @Nested
    @DisplayName("fromCode")
    class FromCodeTests {

        @Test
        @DisplayName("有效 code 查找")
        void fromCode_validCode() {
            assertThat(BusinessType.fromCode("1")).isEqualTo(BusinessType.ADD);
            assertThat(BusinessType.fromCode("4")).isEqualTo(BusinessType.LOGIN);
        }

        @Test
        @DisplayName("非法 code 抛出异常")
        void fromCode_invalidCode_throws() {
            assertThatThrownBy(() -> BusinessType.fromCode("99"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unknown");
        }

        @Test
        @DisplayName("null code 抛出异常")
        void fromCode_null_throws() {
            assertThatThrownBy(() -> BusinessType.fromCode(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must not be null");
        }
    }

    @Nested
    @DisplayName("getDescByCode")
    class GetDescByCodeTests {

        @Test
        @DisplayName("有效 code 返回描述")
        void getDescByCode_valid() {
            assertThat(BusinessType.getDescByCode("2")).isEqualTo("修改");
        }

        @Test
        @DisplayName("非法 code 返回未知")
        void getDescByCode_invalid_returnsUnknown() {
            assertThat(BusinessType.getDescByCode("999")).isEqualTo("未知");
        }
    }

    @Test
    @DisplayName("枚举 getCode/getDesc")
    void enum_getters() {
        assertThat(BusinessType.OTHER.getCode()).isEqualTo("0");
        assertThat(BusinessType.OTHER.getDesc()).isEqualTo("其它");
    }
}