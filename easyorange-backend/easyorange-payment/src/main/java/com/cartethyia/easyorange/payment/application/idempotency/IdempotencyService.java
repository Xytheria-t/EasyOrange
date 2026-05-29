package com.cartethyia.easyorange.payment.application.idempotency;

import com.cartethyia.easyorange.payment.domain.valueobject.IdempotencyKey;
import com.cartethyia.easyorange.payment.domain.repository.IdempotencyKeyRepositoryPort;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final IdempotencyKeyRepositoryPort repository;
    private final ObjectMapper objectMapper;

    private static final int EXPIRY_HOURS = 24;

    @Transactional
    public <T> Optional<T> process(String idempotencyKey, Long userId, Object request, 
                                     IdempotentOperation<T> operation) throws Exception {
        
        String requestHash = hashRequest(request);
        
        Optional<IdempotencyKey> existingKey = repository.findByKey(idempotencyKey);
        
        if (existingKey.isPresent()) {
            IdempotencyKey key = existingKey.get();
            
            if (!key.userId().equals(userId)) {
                log.warn("幂等性键用户不匹配 key={} expectedUserId={} actualUserId={}",
                    idempotencyKey, key.userId(), userId);
                throw new IllegalStateException("幂等性键不属于当前用户");
            }

            if (key.status().equals(IdempotencyKey.STATUS_COMPLETED)) {
                T cachedResponse = deserializeResponse(key.responseData(), operation.getResponseType());
                return Optional.of(cachedResponse);
            }
        } else {
            IdempotencyKey newKey = IdempotencyKey.of(
                    idempotencyKey,
                    userId,
                    requestHash,
                    IdempotencyKey.STATUS_PENDING,
                    LocalDateTime.now().plusHours(EXPIRY_HOURS)
            );
            
            repository.save(newKey);
        }
        
        T result = operation.execute();
        
        String responseData = serializeResponse(result);
        repository.updateResponse(idempotencyKey, responseData, IdempotencyKey.STATUS_COMPLETED);
        
        return Optional.of(result);
    }

    private String hashRequest(Object request) {
        try {
            String json = objectMapper.writeValueAsString(request);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(json.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (Exception e) {
            log.error("请求哈希计算失败", e);
            throw new RuntimeException("请求哈希计算失败", e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    private <T> String serializeResponse(T response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            log.error("响应序列化失败", e);
            throw new RuntimeException("响应序列化失败", e);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T deserializeResponse(String responseData, Class<T> responseType) {
        try {
            return objectMapper.readValue(responseData, responseType);
        } catch (Exception e) {
            log.error("响应反序列化失败", e);
            throw new RuntimeException("响应反序列化失败", e);
        }
    }

    @FunctionalInterface
    public interface IdempotentOperation<T> {
        T execute() throws Exception;
        
        default Class<T> getResponseType() {
            return null;
        }
    }
}
