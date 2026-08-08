package com.cartethyia.easyorange.payment.adapter.outbound.security;

import com.cartethyia.easyorange.payment.domain.constant.PaymentResultCode;
import com.cartethyia.easyorange.payment.domain.exception.PaymentDomainException;
import com.cartethyia.easyorange.payment.adapter.outbound.config.PaymentCallbackProperties;
import com.cartethyia.easyorange.payment.domain.port.CallbackSignatureVerifierPort;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Slf4j
@Primary
@Component
@RequiredArgsConstructor
public class CallbackSignatureVerifier implements CallbackSignatureVerifierPort {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final PaymentCallbackProperties callbackProperties;

    @Override
    public void verify(String paymentNo, String transactionId, String sign) {
        if (!callbackProperties.isVerifyEnabled()) {
            return;
        }

        if (sign == null || sign.isBlank()) {
            log.warn("回调签名缺失 paymentNo={}", paymentNo);
            throw PaymentDomainException.of(PaymentResultCode.CALLBACK_SIGN_INVALID);
        }

        String data = paymentNo + "|" + transactionId;
        String expectedSign = hmacSha256(data, callbackProperties.getSecret());

        if (!expectedSign.equals(sign)) {
            log.warn("回调签名验证失败 paymentNo={}", paymentNo);
            throw PaymentDomainException.of(PaymentResultCode.CALLBACK_SIGN_INVALID);
        }
    }

    private String hmacSha256(String data, String key) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("HMAC-SHA256 算法不可用", e);
        }
    }
}
