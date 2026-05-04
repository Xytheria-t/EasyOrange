package com.cartethyia.easyorange.payment.domain.gateway;

import com.cartethyia.easyorange.payment.domain.exception.CallbackSignInvalidException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Slf4j
@Service
public class CallbackSignatureVerifier {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    @Value("${payment.callback.secret:default-callback-secret-key}")
    private String callbackSecret;

    @Value("${payment.callback.verify-enabled:true}")
    private boolean verifyEnabled;

    public void verify(String paymentNo, String transactionId, String sign) {
        if (!verifyEnabled) {
            return;
        }

        if (sign == null || sign.isBlank()) {
            log.warn("回调签名缺失 paymentNo={}", paymentNo);
            throw CallbackSignInvalidException.of();
        }

        String data = paymentNo + "|" + transactionId;
        String expectedSign = hmacSha256(data, callbackSecret);

        if (!expectedSign.equals(sign)) {
            log.warn("回调签名验证失败 paymentNo={}", paymentNo);
            throw CallbackSignInvalidException.of();
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
