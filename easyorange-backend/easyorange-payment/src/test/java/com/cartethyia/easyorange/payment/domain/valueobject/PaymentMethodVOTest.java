package com.cartethyia.easyorange.payment.domain.valueobject;

import com.cartethyia.easyorange.payment.domain.exception.PaymentDomainException;
import com.cartethyia.easyorange.payment.domain.constant.PaymentMethod;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PaymentMethodVO 值对象测试")
class PaymentMethodVOTest {

    @Nested
    @DisplayName("of 静态工厂方法 - 正常场景")
    class OfValidTests {

        @Test
        @DisplayName("使用 of 创建微信支付")
        void of_withWechatCode_createsPaymentMethodVO() {
            PaymentMethodVO vo = PaymentMethodVO.of(PaymentMethod.WECHAT.getCode());
            assertThat(vo.code()).isEqualTo(PaymentMethod.WECHAT.getCode());
            assertThat(vo.isWechat()).isTrue();
            assertThat(vo.isAlipay()).isFalse();
            assertThat(vo.isBalance()).isFalse();
        }

        @Test
        @DisplayName("使用 of 创建支付宝")
        void of_withAlipayCode_createsPaymentMethodVO() {
            PaymentMethodVO vo = PaymentMethodVO.of(PaymentMethod.ALIPAY.getCode());
            assertThat(vo.code()).isEqualTo(PaymentMethod.ALIPAY.getCode());
            assertThat(vo.isAlipay()).isTrue();
        }

        @Test
        @DisplayName("使用 of 创建余额支付")
        void of_withBalanceCode_createsPaymentMethodVO() {
            PaymentMethodVO vo = PaymentMethodVO.of(PaymentMethod.BALANCE.getCode());
            assertThat(vo.code()).isEqualTo(PaymentMethod.BALANCE.getCode());
            assertThat(vo.isBalance()).isTrue();
        }

        @Test
        @DisplayName("静态工厂方法创建微信支付")
        void wechat_createsWechatPaymentMethod() {
            PaymentMethodVO vo = PaymentMethodVO.wechat();
            assertThat(vo.isWechat()).isTrue();
        }

        @Test
        @DisplayName("静态工厂方法创建支付宝")
        void alipay_createsAlipayPaymentMethod() {
            PaymentMethodVO vo = PaymentMethodVO.alipay();
            assertThat(vo.isAlipay()).isTrue();
        }

        @Test
        @DisplayName("静态工厂方法创建余额支付")
        void balance_createsBalancePaymentMethod() {
            PaymentMethodVO vo = PaymentMethodVO.balance();
            assertThat(vo.isBalance()).isTrue();
        }
    }

    @Nested
    @DisplayName("of 静态工厂方法 - 非法输入")
    class OfInvalidTests {

        @Test
        @DisplayName("null 抛出 PaymentDomainException")
        void of_withNull_throws() {
            assertThatThrownBy(() -> PaymentMethodVO.of(null))
                .isInstanceOf(PaymentDomainException.class)
                .hasMessageContaining("支付方式不能为空");
        }

        @Test
        @DisplayName("不支持的支付方式抛出 PaymentDomainException")
        void of_withUnsupportedCode_throws() {
            assertThatThrownBy(() -> PaymentMethodVO.of(999))
                .isInstanceOf(PaymentDomainException.class)
                .hasMessageContaining("不支持的支付方式");
        }
    }

    @Nested
    @DisplayName("getDesc 方法")
    class GetDescTests {

        @Test
        @DisplayName("微信支付描述正确")
        void wechat_getDesc_returnsCorrectDesc() {
            PaymentMethodVO vo = PaymentMethodVO.wechat();
            assertThat(vo.getDesc()).isEqualTo(PaymentMethod.WECHAT.getDesc());
        }

        @Test
        @DisplayName("支付宝描述正确")
        void alipay_getDesc_returnsCorrectDesc() {
            PaymentMethodVO vo = PaymentMethodVO.alipay();
            assertThat(vo.getDesc()).isEqualTo(PaymentMethod.ALIPAY.getDesc());
        }

        @Test
        @DisplayName("余额支付描述正确")
        void balance_getDesc_returnsCorrectDesc() {
            PaymentMethodVO vo = PaymentMethodVO.balance();
            assertThat(vo.getDesc()).isEqualTo(PaymentMethod.BALANCE.getDesc());
        }
    }

    @Nested
    @DisplayName("equals 和 hashCode")
    class EqualsHashCodeTests {

        @Test
        @DisplayName("相同支付方式相等")
        void equals_sameCode_returnsTrue() {
            PaymentMethodVO vo1 = PaymentMethodVO.wechat();
            PaymentMethodVO vo2 = PaymentMethodVO.wechat();
            assertThat(vo1).isEqualTo(vo2);
        }

        @Test
        @DisplayName("不同支付方式不相等")
        void equals_differentCode_returnsFalse() {
            PaymentMethodVO vo1 = PaymentMethodVO.wechat();
            PaymentMethodVO vo2 = PaymentMethodVO.alipay();
            assertThat(vo1).isNotEqualTo(vo2);
        }

        @Test
        @DisplayName("相同支付方式 hashCode 相等")
        void hashCode_sameCode_returnsSameHash() {
            PaymentMethodVO vo1 = PaymentMethodVO.wechat();
            PaymentMethodVO vo2 = PaymentMethodVO.wechat();
            assertThat(vo1.hashCode()).isEqualTo(vo2.hashCode());
        }
    }

    @Nested
    @DisplayName("toString")
    class ToStringTests {

        @Test
        @DisplayName("toString 返回描述和代码")
        void toString_returnsDescAndCode() {
            PaymentMethodVO vo = PaymentMethodVO.wechat();
            String str = vo.toString();
            assertThat(str).contains("微信支付");
            assertThat(str).contains(String.valueOf(PaymentMethod.WECHAT.getCode()));
        }
    }
}
