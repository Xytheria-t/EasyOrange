package com.cartethyia.easyorange.product.application.service;

import com.cartethyia.easyorange.product.adapter.outbound.persistence.search.SearchHistoryDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.search.SearchHistoryMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 搜索历史写入缓冲 — 记录搜索是高频小写入，逐条落库会把 {@code eo_search_history} 打成写热点，
 * 故先进内存队列、由定时任务批量 upsert。
 * <p>
 * 已知技术债：本类直接 import adapter 的 {@code SearchHistoryDO} / {@code SearchHistoryMapper}，
 * 属「application 反向依赖 adapter」，已被 ArchUnit 规则 7 冻结在快照里，不做重构。
 * 热词计数那条链路刻意没跟进本类（见 {@code HotKeywordBufferAdapter}）—— 再往这里加一个
 * adapter 依赖只会让冻结快照多一条违规，且热词本身是可丢的统计量、不值得复制这套缓冲。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchHistoryBufferAppService {

    private final SearchHistoryMapper searchHistoryMapper;

    private final BlockingQueue<SearchHistoryDO> buffer = new LinkedBlockingQueue<>(5000);

    private static final int BATCH_SIZE = 100;

    /**
     * 队列水位警戒线：超过此水位时 flush 失败不再回插，直接丢弃以保护系统。
     */
    private static final int DRAIN_THRESHOLD = 4000;

    public void addToBuffer(String userId, String keyword) {
        if (userId == null || keyword == null || keyword.isBlank()) {
            return;
        }

        SearchHistoryDO historyDO = SearchHistoryDO.builder()
                .userId(userId)
                .keyword(keyword)
                .searchTime(LocalDateTime.now())
                .build();
        if (!buffer.offer(historyDO)) {
            log.warn("搜索历史缓冲区已满({}), 丢弃条目", buffer.size());
        }
    }

    @Scheduled(fixedRate = 5000)
    public void flushBuffer() {
        var batch = new ArrayList<SearchHistoryDO>(BATCH_SIZE);
        buffer.drainTo(batch, BATCH_SIZE);

        if (batch.isEmpty()) {
            return;
        }

        try {
            searchHistoryMapper.batchInsert(batch);
            log.debug("Flushed {} search history records to database", batch.size());
        } catch (Exception e) {
            log.error("Failed to flush search history buffer", e);
            // 队列不拥挤才回插，否则丢弃以保护系统
            if (buffer.size() < DRAIN_THRESHOLD) {
                buffer.addAll(batch);
            } else {
                log.warn("搜索历史缓冲区拥挤({}), 丢弃 {} 条记录", buffer.size(), batch.size());
            }
        }
    }

    public int getBufferSize() {
        return buffer.size();
    }
}
