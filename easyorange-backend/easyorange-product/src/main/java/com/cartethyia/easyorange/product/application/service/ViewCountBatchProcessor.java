package com.cartethyia.easyorange.product.application.service;

import com.cartethyia.easyorange.product.application.port.cache.ViewCountPort;
import com.cartethyia.easyorange.product.domain.repository.ProductRepository;
import com.cartethyia.easyorange.product.domain.valueobject.ViewCountEntry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ViewCountBatchProcessor {

    private final ViewCountPort viewCountPort;
    private final ProductRepository productRepository;

    public void flush() {
        // Step 1: Read all pending views from Redis (non-transactional)
        var entries = viewCountPort.findAllPending();
        if (entries.isEmpty()) return;

        // Step 2: Batch update DB — batchAddViewCounts 为单条批处理 SQL（语句级原子），
        // 失败时 Redis 计数保留待下轮重放；不加 @Transactional（同类自调用不生效，且单语句无增益）
        productRepository.batchAddViewCounts(entries);
        log.debug("batch update view count done: processed={}", entries.size());

        // Step 3: Clean up Redis (non-transactional, best-effort)
        try {
            viewCountPort.removePending(
                    entries.stream().map(ViewCountEntry::productId).toList());
        } catch (Exception e) {
            log.error("action=cleanupViewCountCacheFailed entries={}", entries.size(), e);
        }
    }
}
