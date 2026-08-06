package com.cartethyia.easyorange.payment.adapter.outbound.persistence.typehandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cartethyia.easyorange.payment.domain.constant.PaymentMethod;
import com.cartethyia.easyorange.payment.domain.constant.PaymentStatus;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.apache.ibatis.type.JdbcType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("支付枚举 TypeHandler 测试")
class PaymentTypeHandlerTest {

    @Mock
    private PreparedStatement ps;

    @Mock
    private ResultSet rs;

    @Mock
    private CallableStatement cs;

    @Nested
    @DisplayName("PaymentStatusTypeHandler")
    class StatusHandlerTests {

        private final PaymentStatusTypeHandler handler = new PaymentStatusTypeHandler();

        @Test
        @DisplayName("写入参数转为状态码")
        void setNonNullParameter_writesCode() throws Exception {
            handler.setNonNullParameter(ps, 1, PaymentStatus.SUCCESS, JdbcType.VARCHAR);

            verify(ps).setString(1, "SUCCESS");
        }

        @Test
        @DisplayName("按列名读取 - 非空值转枚举")
        void getNullableResult_byColumnName() throws Exception {
            when(rs.getString("status")).thenReturn("SUCCESS");

            assertThat(handler.getNullableResult(rs, "status")).isEqualTo(PaymentStatus.SUCCESS);
        }

        @Test
        @DisplayName("按列名读取 - 空值返回 null")
        void getNullableResult_byColumnName_null() throws Exception {
            when(rs.getString("status")).thenReturn(null);

            assertThat(handler.getNullableResult(rs, "status")).isNull();
        }

        @Test
        @DisplayName("按列索引读取")
        void getNullableResult_byIndex() throws Exception {
            when(rs.getString(1)).thenReturn("REFUNDED");

            assertThat(handler.getNullableResult(rs, 1)).isEqualTo(PaymentStatus.REFUNDED);
        }

        @Test
        @DisplayName("存储过程读取")
        void getNullableResult_fromCallable() throws Exception {
            when(cs.getString(1)).thenReturn("PENDING");

            assertThat(handler.getNullableResult(cs, 1)).isEqualTo(PaymentStatus.PENDING);
        }
    }

    @Nested
    @DisplayName("PaymentMethodTypeHandler")
    class MethodHandlerTests {

        private final PaymentMethodTypeHandler handler = new PaymentMethodTypeHandler();

        @Test
        @DisplayName("写入参数转方法码")
        void setNonNullParameter_writesCode() throws Exception {
            handler.setNonNullParameter(ps, 1, PaymentMethod.ALIPAY, JdbcType.VARCHAR);

            verify(ps).setString(1, "ALIPAY");
        }

        @Test
        @DisplayName("按列名读取")
        void getNullableResult_byColumnName() throws Exception {
            when(rs.getString("method")).thenReturn("WECHAT");

            assertThat(handler.getNullableResult(rs, "method")).isEqualTo(PaymentMethod.WECHAT);
        }
    }
}
