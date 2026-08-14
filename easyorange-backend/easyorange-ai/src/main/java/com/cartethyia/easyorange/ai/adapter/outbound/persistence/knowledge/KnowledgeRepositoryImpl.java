package com.cartethyia.easyorange.ai.adapter.outbound.persistence.knowledge;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cartethyia.easyorange.ai.enums.KnowledgeDocStatus;
import com.cartethyia.easyorange.ai.knowledge.KnowledgeDocEntity;
import com.cartethyia.easyorange.ai.knowledge.KnowledgeRepository;
import com.cartethyia.easyorange.common.idgen.IdGenerator;
import com.cartethyia.easyorange.common.repository.BaseRepository;
import com.cartethyia.easyorange.common.result.PageResult;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * 知识库文档仓储（MyBatis-Plus）— 实现 {@link KnowledgeRepository}，
 * 端口隔离保证领域侧不直接依赖 mapper。
 */
@Repository
public class KnowledgeRepositoryImpl extends BaseRepository<KnowledgeDocMapper, KnowledgeDocDO>
        implements KnowledgeRepository {

    private final IdGenerator idGenerator;

    public KnowledgeRepositoryImpl(KnowledgeDocMapper mapper, IdGenerator idGenerator) {
        super(mapper);
        this.idGenerator = idGenerator;
    }

    @Override
    public String save(KnowledgeDocEntity doc) {
        var entity = new KnowledgeDocDO();
        entity.setId(doc.id() != null ? doc.id() : idGenerator.generateId());
        entity.setTitle(doc.title());
        entity.setContent(doc.content());
        entity.setSource(doc.source());
        entity.setStatus(doc.status() != null ? doc.status() : KnowledgeDocStatus.PENDING);
        entity.setChunkCount(doc.chunkCount());
        mapper.insert(entity);
        return entity.getId();
    }

    @Override
    public void updateStatus(String id, KnowledgeDocStatus status, int chunkCount) {
        lambdaUpdate()
                .eq(KnowledgeDocDO::getId, id)
                .set(KnowledgeDocDO::getStatus, status)
                .set(KnowledgeDocDO::getChunkCount, chunkCount)
                .update();
    }

    @Override
    public Optional<KnowledgeDocEntity> findById(String id) {
        return findBy(KnowledgeDocDO::getId, id).map(KnowledgeRepositoryImpl::toEntity);
    }

    @Override
    public PageResult<KnowledgeDocEntity> page(int pageNum, int pageSize) {
        var result = lambdaQuery().orderByDesc(KnowledgeDocDO::getCreateTime).page(new Page<>(pageNum, pageSize));
        return PageResult.of(
                result.getRecords().stream()
                        .map(KnowledgeRepositoryImpl::toEntity)
                        .toList(),
                result.getTotal(),
                (int) result.getCurrent(),
                (int) result.getSize());
    }

    @Override
    public void deleteById(String id) {
        lambdaUpdate().eq(KnowledgeDocDO::getId, id).remove();
    }

    @Override
    public List<KnowledgeDocEntity> searchByContent(String keyword, int limit) {
        return lambdaQuery()
                .like(KnowledgeDocDO::getTitle, keyword)
                .or()
                .like(KnowledgeDocDO::getContent, keyword)
                .last("LIMIT " + limit)
                .list()
                .stream()
                .map(KnowledgeRepositoryImpl::toEntity)
                .toList();
    }

    private static KnowledgeDocEntity toEntity(KnowledgeDocDO doc) {
        return new KnowledgeDocEntity(
                doc.getId(),
                doc.getTitle(),
                doc.getContent(),
                doc.getSource(),
                doc.getStatus(),
                doc.getChunkCount(),
                doc.getCreateTime());
    }
}
