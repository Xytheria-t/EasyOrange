package com.cartethyia.easyorange.message.adapter.inbound.job;

import com.cartethyia.easyorange.message.adapter.outbound.persistence.MessageDO;
import com.cartethyia.easyorange.message.adapter.outbound.persistence.MessageMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 单批归档事务处理器 — 独立 Bean 避免定时任务同类自调用导致 {@code @Transactional} 失效
 * （与 payment 模块 {@code PaymentPhaseExecutor} 同惯例）。
 */
@Component
@RequiredArgsConstructor
public class MessageArchiveBatchHandler {

    private final MessageMapper messageMapper;

    /** 写入归档表 + 从主表物理删除同事务原子提交，中途失败整批回滚、下次重跑。 */
    @Transactional(rollbackFor = Exception.class)
    public void archiveBatch(List<MessageDO> messages) {
        messageMapper.batchInsertArchive(messages);
        messageMapper.deleteByIdsPhysical(
                messages.stream().map(MessageDO::getId).toList());
    }
}
