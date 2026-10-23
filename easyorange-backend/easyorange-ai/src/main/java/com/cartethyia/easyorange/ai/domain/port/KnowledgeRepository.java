package com.cartethyia.easyorange.ai.domain.port;

import com.cartethyia.easyorange.ai.domain.enums.KnowledgeDocStatus;
import com.cartethyia.easyorange.ai.domain.model.KnowledgeDoc;
import com.cartethyia.easyorange.common.result.PageResult;
import java.util.List;
import java.util.Optional;

/** 知识库文档仓储端口 — MyBatis-Plus 实现在 {@code adapter/outbound/persistence}，domain/application 不碰 mapper。 */
public interface KnowledgeRepository {

    /** id 为空时生成 UUID v7。 */
    String save(KnowledgeDoc doc);

    /** 摄入完成后回填。 */
    void updateStatus(String id, KnowledgeDocStatus status, int chunkCount);

    Optional<KnowledgeDoc> findById(String id);

    PageResult<KnowledgeDoc> page(int pageNum, int pageSize);

    /** 逻辑删除（del_flag=1），索引侧同步移除由调用方负责。 */
    void deleteById(String id);

    /** ES 不可用时的降级检索：按标题/正文 LIKE 匹配（仅保证可用，不保证召回质量）。 */
    List<KnowledgeDoc> searchByContent(String keyword, int limit);
}
