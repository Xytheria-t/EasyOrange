package com.cartethyia.easyorange.payment.adapter.outbound.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cartethyia.easyorange.payment.domain.exception.PaymentDomainException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("CallbackSignatureVerifier 测试")
class CallbackSignatureVerifierTest {

    private static final String SECRET = "test-secret";
    private final CallbackSignatureVerifier verifier = new CallbackSignatureVerifier();

    @BeforeEach
    void setUp() throws Exception {
        setField("callbackSecret", SECRET);
        setField("verifyEnabled", true);
    }

    private void setField(String name, Object value) throws Exception {
        Field field = CallbackSignatureVerifier.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(verifier, value);
    }

    private String hmac(String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    }

    @Nested
    @DisplayName("verify")
    class VerifyTests {

        @Test
        @DisplayName("签名匹配时通过")
        void verify_validSign_passes() throws Exception {
            String sign = hmac("PAY123|TXN_1");

            assertThatCode(() -> verifier.verify("PAY123", "TXN_1", sign)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("签名缺失时抛出业务异常")
        void verify_missingSign_throws() {
            assertThatThrownBy(() -> verifier.verify("PAY123", "TXN_1", null))
                    .isInstanceOf(PaymentDomainException.class);
        }

        @Test
        @DisplayName("签名不匹配时抛出业务异常")
        void verify_wrongSign_throws() {
            assertThatThrownBy(() -> verifier.verify("PAY123", "TXN_1", "wrong-sign"))
                    .isInstanceOf(PaymentDomainException.class);
        }

        @Test
        @DisplayName("校验开关关闭时直接放行")
        void verify_disabled_passesThrough() throws Exception {
            setField("verifyEnabled", false);

            assertThatCode(() -> verifier.verify("PAY123", "TXN_1", "anything")).doesNotThrowAnyException();
        }
    }
}
