package com.cartethyia.easyorange.message.adapter.outbound.persistence;

import com.cartethyia.easyorange.common.repository.BaseRepository;
import com.cartethyia.easyorange.message.constant.MessageConstant;
import com.cartethyia.easyorange.message.domain.aggregate.OfflineMessageAggregate;
import com.cartethyia.easyorange.message.entity.OfflineMessage;
import com.cartethyia.easyorange.message.domain.repository.OfflineMessageRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class MybatisOfflineMessageRepository extends BaseRepository<OfflineMessageMapper, OfflineMessage> implements OfflineMessageRepository {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    private final MessageDataMapper messageDataMapper;

    public MybatisOfflineMessageRepository(OfflineMessageMapper mapper, MessageDataMapper messageDataMapper) {
        super(mapper);
        this.messageDataMapper = messageDataMapper;
    }

    @Override
    public OfflineMessageAggregate save(OfflineMessageAggregate message) {
        OfflineMessage entity = messageDataMapper.toEntity(message);
        mapper.insert(entity);
        return messageDataMapper.toAggregate(entity);
    }

    @Override
    public List<OfflineMessageAggregate> findPendingByUserId(String userId) {
        return messageDataMapper.toOfflineAggregateList(
                lambdaQuery()
                        .eq(OfflineMessage::getUserId, userId)
                        .eq(OfflineMessage::getPushStatus, MessageConstant.PUSH_STATUS_PENDING)
                        .orderByAsc(OfflineMessage::getCreateTime)
                        .list()
                        .stream()
                        .filter(msg -> msg.getRetryCount() < msg.getMaxRetryCount())
                        .toList()
        );
    }

    @Override
    public void markAsPushed(String offlineMessageId) {
        lambdaUpdate()
                .eq(OfflineMessage::getId, offlineMessageId)
                .set(OfflineMessage::getPushStatus, MessageConstant.PUSH_STATUS_PUSHED)
                .set(OfflineMessage::getPushTime, LocalDateTime.now())
                .update();
    }

    @Override
    public void markAsFailed(String offlineMessageId) {
        lambdaUpdate()
                .eq(OfflineMessage::getId, offlineMessageId)
                .set(OfflineMessage::getPushStatus, MessageConstant.PUSH_STATUS_FAILED)
                .update();
    }

    @Override
    public void incrementRetryCount(String offlineMessageId) {
        lambdaUpdate()
                .eq(OfflineMessage::getId, offlineMessageId)
                .setSql("retry_count = retry_count + 1")
                .set(OfflineMessage::getLastRetryTime, LocalDateTime.now())
                .update();
    }
}
