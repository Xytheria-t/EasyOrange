package com.cartethyia.easyorange.ai.knowledge;

import com.cartethyia.easyorange.ai.enums.KnowledgeDocStatus;
import java.time.LocalDateTime;

/**
 * 知识库文档实体（跨层传输形态，持久化侧在 adapter/outbound/persistence）。
 */
public record KnowledgeDocEntity(
        String id,
        String title,
        String content,
        String source,
        KnowledgeDocStatus status,
        int chunkCount,
        LocalDateTime createTime) {}
