package com.cartethyia.easyorange.message.adapter.outbound.persistence;

import com.cartethyia.easyorange.framework.repository.BaseRepository;
import com.cartethyia.easyorange.message.constant.MessageConstant;
import com.cartethyia.easyorange.message.entity.OfflineMessage;
import com.cartethyia.easyorange.message.domain.repository.OfflineMessageRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class MybatisOfflineMessageRepository extends BaseRepository<OfflineMessageMapper, OfflineMessage> implements OfflineMessageRepository {

    public MybatisOfflineMessageRepository(OfflineMessageMapper mapper) {
        super(mapper);
    }

    @Override
    public OfflineMessage save(OfflineMessage message) {
        mapper.insert(message);
        return message;
    }

    @Override
    public List<OfflineMessage> findPendingByUserId(Long userId) {
        return lambdaQuery()
                        .eq(OfflineMessage::getUserId, userId)
                        .eq(OfflineMessage::getPushStatus, MessageConstant.PUSH_STATUS_PENDING)
                        .orderByAsc(OfflineMessage::getCreateTime)
                .list()
                .stream()
                .filter(msg -> msg.getRetryCount() < msg.getMaxRetryCount())
                .collect(Collectors.toList());
    }

    @Override
    public void markAsPushed(Long offlineMessageId) {
        lambdaUpdate()
                .eq(OfflineMessage::getId, offlineMessageId)
                .set(OfflineMessage::getPushStatus, MessageConstant.PUSH_STATUS_PUSHED)
                .set(OfflineMessage::getPushTime, LocalDateTime.now())
                .update();
    }

    @Override
    public void markAsFailed(Long offlineMessageId) {
        lambdaUpdate()
                .eq(OfflineMessage::getId, offlineMessageId)
                .set(OfflineMessage::getPushStatus, MessageConstant.PUSH_STATUS_FAILED)
                .update();
    }

    @Override
    public void incrementRetryCount(Long offlineMessageId) {
        lambdaUpdate()
                .eq(OfflineMessage::getId, offlineMessageId)
                .setSql("retry_count = retry_count + 1")
                .set(OfflineMessage::getLastRetryTime, LocalDateTime.now())
                .update();
    }
}