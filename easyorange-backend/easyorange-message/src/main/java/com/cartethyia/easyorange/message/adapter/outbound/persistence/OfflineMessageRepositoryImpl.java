package com.cartethyia.easyorange.message.adapter.outbound.persistence;

import com.cartethyia.easyorange.common.repository.BaseRepository;
import com.cartethyia.easyorange.message.domain.constant.MessageConstant;
import com.cartethyia.easyorange.message.domain.aggregate.OfflineMessage;
import com.cartethyia.easyorange.message.adapter.outbound.persistence.OfflineMessageDO;
import com.cartethyia.easyorange.message.domain.repository.OfflineMessageRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Primary
@Repository
public class OfflineMessageRepositoryImpl extends BaseRepository<OfflineMessageMapper, OfflineMessageDO> implements OfflineMessageRepository {

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
        return messageDataMapper.toOfflineAggregateList(
                lambdaQuery()
                        .eq(OfflineMessageDO::getUserId, userId)
                        .eq(OfflineMessageDO::getPushStatus, MessageConstant.PUSH_STATUS_PENDING)
                        .orderByAsc(OfflineMessageDO::getCreateTime)
                        .list()
                        .stream()
                        .filter(msg -> msg.getRetryCount() < msg.getMaxRetryCount())
                        .toList()
        );
    }

    @Override
    public void markAsPushed(String offlineMessageId) {
        lambdaUpdate()
                .eq(OfflineMessageDO::getId, offlineMessageId)
                .set(OfflineMessageDO::getPushStatus, MessageConstant.PUSH_STATUS_PUSHED)
                .set(OfflineMessageDO::getPushTime, LocalDateTime.now())
                .update();
    }

    @Override
    public void markAsFailed(String offlineMessageId) {
        lambdaUpdate()
                .eq(OfflineMessageDO::getId, offlineMessageId)
                .set(OfflineMessageDO::getPushStatus, MessageConstant.PUSH_STATUS_FAILED)
                .update();
    }

    @Override
    public void incrementRetryCount(String offlineMessageId) {
        lambdaUpdate()
                .eq(OfflineMessageDO::getId, offlineMessageId)
                .setSql("retry_count = retry_count + 1")
                .set(OfflineMessageDO::getLastRetryTime, LocalDateTime.now())
                .update();
    }
}
