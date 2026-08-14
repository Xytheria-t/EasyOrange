package com.cartethyia.easyorange.ai.adapter.inbound.job;

import com.cartethyia.easyorange.ai.knowledge.KnowledgeIndexPort;
import com.cartethyia.easyorange.ai.service.KnowledgeIngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 知识库启动补索引 — 上次摄入时 ES 不可用/失败的 PENDING 文档，启动时重试（保持文档 ID 稳定）。
 * <p>
 * best-effort 语义：种子文档（R__seed_knowledge_docs.sql）首次启动即被摄入进 ES，
 * 不需要人工点管理端「重新索引」；索引持续不可用则保持 PENDING 下次再试。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeBootstrapIndexer implements ApplicationRunner {

    private final KnowledgeIngestionService ingestionService;
    private final ObjectProvider<KnowledgeIndexPort> indexPortProvider;

    @Override
    public void run(ApplicationArguments args) {
        var port = indexPortProvider.getIfAvailable();
        if (port == null || !port.isAvailable()) {
            log.info("Knowledge index unavailable, skip bootstrap indexing");
            return;
        }
        int retried = ingestionService.reindexAllPending();
        if (retried > 0) {
            log.info("Knowledge bootstrap indexer: retried {} PENDING docs", retried);
        }
    }
}
