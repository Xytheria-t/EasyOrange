package com.cartethyia.easyorange.message.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cartethyia.easyorange.common.entity.BaseDO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * 离线消息实体
 *
 * @author cartethyia
 * @date 2026/03/06
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("eo_offline_message")
public class OfflineMessage extends BaseDO {

    /** 用户 ID */
    private Long userId;

    /** 消息 ID */
    private Long messageId;

    /** 推送渠道 */
    private String pushChannel;

    /** 推送状态：0-待推送 1-已推送 2-推送失败 */
    private Integer pushStatus;

    /** 推送时间 */
    private LocalDateTime pushTime;

    /** 重试次数 */
    private Integer retryCount;

    /** 最大重试次数 */
    private Integer maxRetryCount;

    /** 最后重试时间 */
    private LocalDateTime lastRetryTime;
}
