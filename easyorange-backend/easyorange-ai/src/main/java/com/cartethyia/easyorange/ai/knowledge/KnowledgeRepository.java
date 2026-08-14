package com.cartethyia.easyorange.ai.knowledge;

import com.cartethyia.easyorange.ai.enums.KnowledgeDocStatus;
import com.cartethyia.easyorange.common.result.PageResult;
import java.util.List;
import java.util.Optional;

/**
 * 知识库文档仓储端口 — 业务侧只依赖本接口，持久化实现（MyBatis-Plus）在
 * {@code adapter/outbound/persistence}，符合 DDD 依赖方向（domain/application 不碰 mapper）。
 */
public interface KnowledgeRepository {

    /** 插入文档（id 为空时生成 UUID v7），返回文档 ID。 */
    String save(KnowledgeDocEntity doc);

    /** 摄入完成后回填索引状态与分块数。 */
    void updateStatus(String id, KnowledgeDocStatus status, int chunkCount);

    Optional<KnowledgeDocEntity> findById(String id);

    PageResult<KnowledgeDocEntity> page(int pageNum, int pageSize);

    /** 逻辑删除（del_flag=1），索引侧同步移除由调用方负责。 */
    void deleteById(String id);

    /** ES 不可用时的降级检索：按标题/正文 LIKE 匹配（仅保证可用，不保证召回质量）。 */
    List<KnowledgeDocEntity> searchByContent(String keyword, int limit);
}
