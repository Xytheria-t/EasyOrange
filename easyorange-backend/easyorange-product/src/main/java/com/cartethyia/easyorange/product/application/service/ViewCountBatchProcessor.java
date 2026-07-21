package com.cartethyia.easyorange.product.application.service;

import com.cartethyia.easyorange.product.adapter.outbound.persistence.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ViewCountBatchProcessor {

    private final RedisTemplate<Object, Object> redisTemplate;
    private final ProductMapper productMapper;

    private static final String VIEW_COUNT_KEY = "eo:product:views:pending";

    @Transactional(rollbackFor = Exception.class)
    public void flush() {
        var pendingViews = redisTemplate.opsForHash().entries(VIEW_COUNT_KEY);
        if (pendingViews.isEmpty()) return;

        var entries = new ArrayList<ProductMapper.ViewCountEntry>();
        for (Map.Entry<Object, Object> entry : pendingViews.entrySet()) {
            try {
                var count = Integer.parseInt(entry.getValue().toString());
                entries.add(new ProductMapper.ViewCountEntry(entry.getKey().toString(), count));
            } catch (NumberFormatException e) {
                log.warn("解析浏览量数据失败: key={}, value={}", entry.getKey(), entry.getValue());
            }
        }

        if (entries.isEmpty()) return;

        productMapper.batchAddViewCounts(entries);
        redisTemplate.opsForHash().delete(VIEW_COUNT_KEY, pendingViews.keySet().toArray());
        log.debug("批量更新浏览量完成: processed={}", entries.size());
    }
}
