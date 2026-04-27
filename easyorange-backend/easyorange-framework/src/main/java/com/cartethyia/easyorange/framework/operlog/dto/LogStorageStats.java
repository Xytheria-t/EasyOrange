package com.cartethyia.easyorange.framework.operlog.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 日志存储统计信息
 *
 * @author cartethyia
 * @date 2026/03/27
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogStorageStats {

    /**
     * 总记录数
     */
    private Long totalCount;

    /**
     * 今日新增记录数
     */
    private Long todayCount;

    /**
     * 最早记录时间
     */
    private LocalDateTime oldestRecord;

    /**
     * 最新记录时间
     */
    private LocalDateTime newestRecord;

    /**
     * 预估存储空间（KB）
     */
    private Long storageSizeKB;
}
