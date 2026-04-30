package com.cartethyia.easyorange.message.domain.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cartethyia.easyorange.message.constant.MessageConstant;
import com.cartethyia.easyorange.message.entity.OfflineMessage;
import com.cartethyia.easyorange.message.mapper.OfflineMessageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class MybatisOfflineMessageRepository implements OfflineMessageRepository {

    private final OfflineMessageMapper mapper;

    @Override
    public OfflineMessage save(OfflineMessage message) {
        mapper.insert(message);
        return message;
    }

    @Override
    public List<OfflineMessage> findPendingByUserId(Long userId) {
        return mapper.selectList(new LambdaQueryWrapper<OfflineMessage>()
                        .eq(OfflineMessage::getUserId, userId)
                        .eq(OfflineMessage::getPushStatus, MessageConstant.PUSH_STATUS_PENDING)
                        .orderByAsc(OfflineMessage::getCreateTime))
                .stream()
                .filter(msg -> msg.getRetryCount() < msg.getMaxRetryCount())
                .collect(Collectors.toList());
    }

    @Override
    public void markAsPushed(Long offlineMessageId) {
        mapper.update(null, new LambdaUpdateWrapper<OfflineMessage>()
                .eq(OfflineMessage::getId, offlineMessageId)
                .set(OfflineMessage::getPushStatus, MessageConstant.PUSH_STATUS_PUSHED)
                .set(OfflineMessage::getPushTime, LocalDateTime.now()));
    }

    @Override
    public void markAsFailed(Long offlineMessageId) {
        mapper.update(null, new LambdaUpdateWrapper<OfflineMessage>()
                .eq(OfflineMessage::getId, offlineMessageId)
                .set(OfflineMessage::getPushStatus, MessageConstant.PUSH_STATUS_FAILED));
    }

    @Override
    public void incrementRetryCount(Long offlineMessageId) {
        mapper.update(null, new LambdaUpdateWrapper<OfflineMessage>()
                .eq(OfflineMessage::getId, offlineMessageId)
                .setSql("retry_count = retry_count + 1")
                .set(OfflineMessage::getLastRetryTime, LocalDateTime.now()));
    }
}
