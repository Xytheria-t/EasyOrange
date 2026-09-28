package com.cartethyia.easyorange.message.adapter.outbound.persistence;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cartethyia.easyorange.common.entity.BaseDO;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 离线消息表 {@code eo_offline_message} 数据对象。
 * <p>
 * 只映射在用的列：{@code push_time} / {@code retry_count} / {@code max_retry_count} /
 * {@code last_retry_time} 四列在 V1 里随「重试机制」一起建表，但该机制从未实现（见
 * {@code OfflineMessage} 类注释），故不映射——四列在库里可空或带默认值，不写不影响读写。
 * 收口收表时由迁移删除，本模块不改已执行的 V1。
 */
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("eo_offline_message")
public class OfflineMessageDO extends BaseDO {

    private String userId;
    private String messageId;
    private String pushChannel;
    private Integer pushStatus;

    public String getUserId() {
        return userId;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getPushChannel() {
        return pushChannel;
    }

    public Integer getPushStatus() {
        return pushStatus;
    }
}
