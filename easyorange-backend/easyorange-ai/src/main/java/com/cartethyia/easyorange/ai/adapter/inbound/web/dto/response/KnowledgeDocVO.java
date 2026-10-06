package com.cartethyia.easyorange.ai.adapter.inbound.web.dto.response;

import com.cartethyia.easyorange.ai.domain.enums.KnowledgeDocStatus;
import java.time.LocalDateTime;

/**
 * 知识库文档列表视图 — 不携带正文（列表页不展示，避免大字段透传）。
 */
public record KnowledgeDocVO(
        String id, String title, String source, KnowledgeDocStatus status, int chunkCount, LocalDateTime createTime) {}
