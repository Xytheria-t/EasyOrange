package com.cartethyia.easyorange.payment.domain.valueobject;

import com.cartethyia.easyorange.payment.domain.exception.PaymentDomainException;
import com.cartethyia.easyorange.payment.domain.constant.PaymentMethod;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PaymentMethodInfo 值对象测试")
class PaymentMethodInfoTest {

    @Nested
    @DisplayName("of 静态工厂方法 - 正常场景")
    class OfValidTests {

        @Test
        @DisplayName("使用 of 创建微信支付")
        void of_withWechatCode_createsPaymentMethodInfo() {
            PaymentMethodInfo vo = PaymentMethodInfo.of(PaymentMethod.WECHAT.getCode());
            assertThat(vo.code()).isEqualTo(PaymentMethod.WECHAT.getCode());
            assertThat(vo.isWechat()).isTrue();
            assertThat(vo.isAlipay()).isFalse();
            assertThat(vo.isBalance()).isFalse();
        }

        @Test
        @DisplayName("使用 of 创建支付宝")
        void of_withAlipayCode_createsPaymentMethodInfo() {
            PaymentMethodInfo vo = PaymentMethodInfo.of(PaymentMethod.ALIPAY.getCode());
            assertThat(vo.code()).isEqualTo(PaymentMethod.ALIPAY.getCode());
            assertThat(vo.isAlipay()).isTrue();
        }

        @Test
        @DisplayName("使用 of 创建余额支付")
        void of_withBalanceCode_createsPaymentMethodInfo() {
            PaymentMethodInfo vo = PaymentMethodInfo.of(PaymentMethod.BALANCE.getCode());
            assertThat(vo.code()).isEqualTo(PaymentMethod.BALANCE.getCode());
            assertThat(vo.isBalance()).isTrue();
        }

        @Test
        @DisplayName("静态工厂方法创建微信支付")
        void wechat_createsWechatPaymentMethod() {
            PaymentMethodInfo vo = PaymentMethodInfo.wechat();
            assertThat(vo.isWechat()).isTrue();
        }

        @Test
        @DisplayName("静态工厂方法创建支付宝")
        void alipay_createsAlipayPaymentMethod() {
            PaymentMethodInfo vo = PaymentMethodInfo.alipay();
            assertThat(vo.isAlipay()).isTrue();
        }

        @Test
        @DisplayName("静态工厂方法创建余额支付")
        void balance_createsBalancePaymentMethod() {
            PaymentMethodInfo vo = PaymentMethodInfo.balance();
            assertThat(vo.isBalance()).isTrue();
        }
    }

    @Nested
    @DisplayName("of 静态工厂方法 - 非法输入")
    class OfInvalidTests {

        @Test
        @DisplayName("null 抛出 PaymentDomainException")
        void of_withNull_throws() {
            assertThatThrownBy(() -> PaymentMethodInfo.of(null))
                .isInstanceOf(PaymentDomainException.class)
                .hasMessageContaining("支付方式不能为空");
        }

        @Test
        @DisplayName("不支持的支付方式抛出 PaymentDomainException")
        void of_withUnsupportedCode_throws() {
            assertThatThrownBy(() -> PaymentMethodInfo.of(999))
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
            PaymentMethodInfo vo = PaymentMethodInfo.wechat();
            assertThat(vo.getDesc()).isEqualTo(PaymentMethod.WECHAT.getDesc());
        }

        @Test
        @DisplayName("支付宝描述正确")
        void alipay_getDesc_returnsCorrectDesc() {
            PaymentMethodInfo vo = PaymentMethodInfo.alipay();
            assertThat(vo.getDesc()).isEqualTo(PaymentMethod.ALIPAY.getDesc());
        }

        @Test
        @DisplayName("余额支付描述正确")
        void balance_getDesc_returnsCorrectDesc() {
            PaymentMethodInfo vo = PaymentMethodInfo.balance();
            assertThat(vo.getDesc()).isEqualTo(PaymentMethod.BALANCE.getDesc());
        }
    }

    @Nested
    @DisplayName("equals 和 hashCode")
    class EqualsHashCodeTests {

        @Test
        @DisplayName("相同支付方式相等")
        void equals_sameCode_returnsTrue() {
            PaymentMethodInfo vo1 = PaymentMethodInfo.wechat();
            PaymentMethodInfo vo2 = PaymentMethodInfo.wechat();
            assertThat(vo1).isEqualTo(vo2);
        }

        @Test
        @DisplayName("不同支付方式不相等")
        void equals_differentCode_returnsFalse() {
            PaymentMethodInfo vo1 = PaymentMethodInfo.wechat();
            PaymentMethodInfo vo2 = PaymentMethodInfo.alipay();
            assertThat(vo1).isNotEqualTo(vo2);
        }

        @Test
        @DisplayName("相同支付方式 hashCode 相等")
        void hashCode_sameCode_returnsSameHash() {
            PaymentMethodInfo vo1 = PaymentMethodInfo.wechat();
            PaymentMethodInfo vo2 = PaymentMethodInfo.wechat();
            assertThat(vo1.hashCode()).isEqualTo(vo2.hashCode());
        }
    }

    @Nested
    @DisplayName("toString")
    class ToStringTests {

        @Test
        @DisplayName("toString 返回描述和代码")
        void toString_returnsDescAndCode() {
            PaymentMethodInfo vo = PaymentMethodInfo.wechat();
            String str = vo.toString();
            assertThat(str).contains("微信支付");
            assertThat(str).contains(String.valueOf(PaymentMethod.WECHAT.getCode()));
        }
    }
}