package com.cartethyia.easyorange.message.domain.repository;

import com.cartethyia.easyorange.message.domain.aggregate.OfflineMessage;

/**
 * 离线消息仓储。状态转换（markAsPushed/markAsFailed/incrementRetry）唯一归属
 * {@link OfflineMessage} 聚合根，重推流程按 加载 → 迁移 → save 接线即可；
 * 此处不再提供绕过聚合根的平行 SQL 状态方法。
 */
public interface OfflineMessageRepository {

    OfflineMessage save(OfflineMessage message);
}
