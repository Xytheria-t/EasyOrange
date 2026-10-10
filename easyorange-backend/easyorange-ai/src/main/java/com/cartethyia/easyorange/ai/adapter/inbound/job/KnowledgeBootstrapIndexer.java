package com.cartethyia.easyorange.ai.adapter.inbound.job;

import com.cartethyia.easyorange.ai.application.retrieval.KnowledgeIngestionAppService;
import com.cartethyia.easyorange.ai.domain.port.KnowledgeIndexPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 知识库启动补索引 — 上次摄入时 ES 不可用（PENDING）或写入/embed 失败（FAILED）的文档，启动时重试
 * （文档 ID 保持稳定）。
 * <p>
 * best-effort：种子文档（R__seed_knowledge_docs.sql）首次启动即被摄入，无需人工点管理端「重新索引」；
 * 索引持续不可用则保持 PENDING 下次再试。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeBootstrapIndexer implements ApplicationRunner {

    private final KnowledgeIngestionAppService ingestionService;
    private final ObjectProvider<KnowledgeIndexPort> indexPortProvider;

    @Override
    public void run(@NonNull ApplicationArguments args) {
        var port = indexPortProvider.getIfAvailable();
        if (port == null || !port.isAvailable()) {
            log.info("Knowledge index unavailable, skip bootstrap indexing");
            return;
        }
        int retried = ingestionService.reindexIncomplete();
        if (retried > 0) {
            log.info("Knowledge bootstrap indexer: retried {} incomplete (PENDING/FAILED) docs", retried);
        }
    }
}
