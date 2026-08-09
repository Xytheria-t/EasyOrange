package com.cartethyia.easyorange.message.adapter.outbound.persistence;

import com.cartethyia.easyorange.common.repository.BaseRepository;
import com.cartethyia.easyorange.message.domain.aggregate.OfflineMessage;
import com.cartethyia.easyorange.message.domain.repository.OfflineMessageRepository;
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
}
