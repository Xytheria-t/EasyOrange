package com.cartethyia.easyorange.message.domain.valueobject;

import static org.assertj.core.api.Assertions.assertThat;

import com.cartethyia.easyorange.message.domain.enums.ReadStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MessageQuery 分页归一化测试")
class MessageQueryTest {

    @Test
    @DisplayName("分页参数为 null：兜底 1 / 20，不抛 NPE")
    void nullPagingParameters_fallBackToDefaults() {
        var query = new MessageQuery(null, null, null, null);

        assertThat(query.pageNum()).isEqualTo(1);
        assertThat(query.pageSize()).isEqualTo(20);
    }

    @Test
    @DisplayName("分页参数非法（< 1）：兜底默认值")
    void invalidPagingParameters_fallBackToDefaults() {
        var query = new MessageQuery(0, -5, null, null);

        assertThat(query.pageNum()).isEqualTo(1);
        assertThat(query.pageSize()).isEqualTo(20);
    }

    @Test
    @DisplayName("pageSize 超过上限 100：截断为 100")
    void oversizedPageSize_cappedAt100() {
        var query = new MessageQuery(2, 500, null, null);

        assertThat(query.pageNum()).isEqualTo(2);
        assertThat(query.pageSize()).isEqualTo(100);
    }

    @Test
    @DisplayName("合法分页与过滤条件原样保留")
    void validParameters_preserved() {
        var query = new MessageQuery(3, 50, 1, ReadStatus.UNREAD);

        assertThat(query.pageNum()).isEqualTo(3);
        assertThat(query.pageSize()).isEqualTo(50);
        assertThat(query.type()).isEqualTo(1);
        assertThat(query.isRead()).isEqualTo(ReadStatus.UNREAD);
    }
}
