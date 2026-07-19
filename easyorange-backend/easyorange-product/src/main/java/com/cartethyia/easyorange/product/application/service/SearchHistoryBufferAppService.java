package com.cartethyia.easyorange.product.application.service;

import com.cartethyia.easyorange.product.adapter.outbound.persistence.SearchHistoryDO;
import com.cartethyia.easyorange.product.adapter.outbound.persistence.mapper.SearchHistoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchHistoryBufferAppService {

    private final SearchHistoryMapper searchHistoryMapper;
    
    private final ConcurrentLinkedQueue<SearchHistoryDO> buffer = new ConcurrentLinkedQueue<>();
    
    private static final int BATCH_SIZE = 100;

    public void addToBuffer(String userId, String keyword) {
        if (userId == null || keyword == null || keyword.isBlank()) {
            return;
        }
        
        SearchHistoryDO historyDO = SearchHistoryDO.builder()
                .userId(userId)
                .keyword(keyword)
                .searchTime(LocalDateTime.now())
                .build();
        buffer.offer(historyDO);
    }

    @Scheduled(fixedRate = 5000)
    public void flushBuffer() {
        if (buffer.isEmpty()) {
            return;
        }
        
        List<SearchHistoryDO> batch = new ArrayList<>();
        SearchHistoryDO item;
        while (batch.size() < BATCH_SIZE && (item = buffer.poll()) != null) {
            batch.add(item);
        }
        
        if (!batch.isEmpty()) {
            try {
                searchHistoryMapper.batchInsert(batch);
                log.debug("Flushed {} search history records to database", batch.size());
            } catch (Exception e) {
                log.error("Failed to flush search history buffer", e);
                for (SearchHistoryDO history : batch) {
                    buffer.offer(history);
                }
            }
        }
    }

    public int getBufferSize() {
        return buffer.size();
    }
}
