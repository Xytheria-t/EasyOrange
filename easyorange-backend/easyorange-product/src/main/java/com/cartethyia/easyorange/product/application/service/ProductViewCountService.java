package com.cartethyia.easyorange.product.application.service;

import com.cartethyia.easyorange.product.domain.repository.ProductRepository;
import com.cartethyia.easyorange.product.domain.valueobject.ProductId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductViewCountService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ProductRepository productRepository;

    private static final String VIEW_COUNT_KEY = "eo:product:views:pending";
    private static final String VIEW_COUNT_LOCK = "eo:product:views:lock";
    private static final int BATCH_SIZE = 100;

    public void incrementViewCount(Long productId) {
        if (productId == null) {
            return;
        }
        try {
            redisTemplate.opsForHash().increment(VIEW_COUNT_KEY, String.valueOf(productId), 1);
        } catch (Exception e) {
            log.warn("记录浏览量失败: productId={}, error={}", productId, e.getMessage());
        }
    }

    @Scheduled(fixedRate = 5000)
    public void flushViewCountBatch() {
        Boolean locked = false;
        try {
            locked = redisTemplate.opsForValue()
                    .setIfAbsent(VIEW_COUNT_LOCK, "1", 10, TimeUnit.SECONDS);
            if (!Boolean.TRUE.equals(locked)) {
                return;
            }

            Map<Object, Object> pendingViews = redisTemplate.opsForHash().entries(VIEW_COUNT_KEY);
            if (pendingViews.isEmpty()) {
                return;
            }

            Map<Long, Long> viewCountMap = new HashMap<>();
            for (Map.Entry<Object, Object> entry : pendingViews.entrySet()) {
                try {
                    Long productId = Long.parseLong(String.valueOf(entry.getKey()));
                    Long count = Long.parseLong(String.valueOf(entry.getValue()));
                    viewCountMap.put(productId, count);
                } catch (NumberFormatException e) {
                    log.warn("解析浏览量数据失败: key={}, value={}", entry.getKey(), entry.getValue());
                }
            }

            int processed = 0;
            for (Map.Entry<Long, Long> entry : viewCountMap.entrySet()) {
                try {
                    productRepository.findById(ProductId.of(entry.getKey()))
                            .ifPresent(product -> {
                                product.addViewCount(entry.getValue().intValue());
                                productRepository.update(product);
                            });
                    processed++;
                } catch (Exception e) {
                    log.warn("更新商品浏览量失败: productId={}, error={}", entry.getKey(), e.getMessage());
                }
            }

            if (processed > 0) {
                Set<Object> keys = pendingViews.keySet();
                redisTemplate.opsForHash().delete(VIEW_COUNT_KEY, keys.toArray(new Object[0]));
                log.debug("批量更新浏览量完成: processed={}", processed);
            }
        } catch (Exception e) {
            log.error("批量更新浏览量失败: error={}", e.getMessage(), e);
        } finally {
            if (Boolean.TRUE.equals(locked)) {
                redisTemplate.delete(VIEW_COUNT_LOCK);
            }
        }
    }
}
