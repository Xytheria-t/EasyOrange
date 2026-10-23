package com.cartethyia.easyorange.ai.domain.model;

import com.cartethyia.easyorange.ai.domain.enums.KnowledgeDocStatus;
import java.time.LocalDateTime;

/** 知识库文档 — 跨层传输的不可变模型（不是持久化实体；映射对象是 {@code KnowledgeDocDO}）。 */
public record KnowledgeDoc(
        String id,
        String title,
        String content,
        String source,
        KnowledgeDocStatus status,
        int chunkCount,
        LocalDateTime createTime) {}
