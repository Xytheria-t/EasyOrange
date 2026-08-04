package com.cartethyia.easyorange.payment.domain.valueobject;

import com.cartethyia.easyorange.payment.application.query.PaymentListQuery;
import com.cartethyia.easyorange.payment.domain.constant.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PaymentListQuery 测试")
class PaymentListQueryTest {

    @Nested
    @DisplayName("分页参数默认值")
    class PagingDefaultsTests {

        @Test
        @DisplayName("null 页码/页大小回退为默认值")
        void nullPaging_defaults() {
            PaymentListQuery query = new PaymentListQuery("3001", PaymentStatus.SUCCESS, null, null);

            assertThat(query.pageNum()).isEqualTo(1);
            assertThat(query.pageSize()).isEqualTo(20);
        }

        @Test
        @DisplayName("小于 1 的页码/页大小回退为默认值")
        void invalidPaging_defaults() {
            PaymentListQuery query = new PaymentListQuery("3001", null, 0, 0);

            assertThat(query.pageNum()).isEqualTo(1);
            assertThat(query.pageSize()).isEqualTo(20);
        }

        @Test
        @DisplayName("合法分页参数透传")
        void validPaging_passthrough() {
            PaymentListQuery query = new PaymentListQuery("3001", null, 3, 50);

            assertThat(query.pageNum()).isEqualTo(3);
            assertThat(query.pageSize()).isEqualTo(50);
        }
    }
}