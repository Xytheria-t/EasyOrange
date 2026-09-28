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
public class ViewCountFlushAppService {

    private final ViewCountPort viewCountPort;
    private final ProductRepository productRepository;

    public void flush() {
        // 1. 读 Redis 待落库计数（非事务）
        var entries = viewCountPort.findAllPending();
        if (entries.isEmpty()) return;

        // 2. 批量更新 DB —— batchAddViewCounts 是单条批处理 SQL（语句级原子），
        // 失败时 Redis 计数保留待下轮重放；不加 @Transactional（同类自调用不生效，且单语句无增益）
        productRepository.batchAddViewCounts(entries);
        log.debug("action=flushViewCountDone processed={}", entries.size());

        // 3. 清 Redis 缓冲（非事务，尽力而为）
        try {
            viewCountPort.removePending(
                    entries.stream().map(ViewCountEntry::productId).toList());
        } catch (Exception e) {
            log.error("action=cleanupViewCountCacheFailed entries={}", entries.size(), e);
        }
    }
}
