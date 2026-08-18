package com.cartethyia.easyorange.message.adapter.outbound.persistence;

import com.cartethyia.easyorange.common.repository.BaseRepository;
import com.cartethyia.easyorange.message.domain.aggregate.OfflineMessage;
import com.cartethyia.easyorange.message.domain.enums.PushStatus;
import com.cartethyia.easyorange.message.domain.repository.OfflineMessageRepository;
import java.util.List;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Primary
@Repository
public class OfflineMessageRepositoryImpl extends BaseRepository<OfflineMessageMapper, OfflineMessageDO>
        implements OfflineMessageRepository {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    private final MessageDataMapper messageDataMapper;

    public OfflineMessageRepositoryImpl(OfflineMessageMapper mapper, MessageDataMapper messageDataMapper) {
        super(mapper);
        this.messageDataMapper = messageDataMapper;
    }

    @Override
    public OfflineMessage save(OfflineMessage message) {
        OfflineMessageDO entity = messageDataMapper.toEntity(message);
        mapper.insert(entity);
        return messageDataMapper.toAggregate(entity);
    }

    @Override
    public List<OfflineMessage> findPendingByUserId(String userId) {
        // 命中表上 (user_id, push_status) 联合索引
        List<OfflineMessageDO> entities = lambdaQuery()
                .eq(OfflineMessageDO::getUserId, userId)
                .eq(OfflineMessageDO::getPushStatus, Integer.valueOf(PushStatus.PENDING.getCode()))
                .orderByAsc(OfflineMessageDO::getCreateTime)
                .list();
        return messageDataMapper.toOfflineAggregateList(entities);
    }
}
