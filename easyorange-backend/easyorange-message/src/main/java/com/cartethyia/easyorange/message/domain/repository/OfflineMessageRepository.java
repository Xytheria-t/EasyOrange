package com.cartethyia.easyorange.message.domain.repository;

import com.cartethyia.easyorange.message.domain.aggregate.OfflineMessage;
import java.util.List;

/**
 * 离线消息仓储。状态转换（markAsPushed/markAsFailed/incrementRetry）唯一归属
 * {@link OfflineMessage} 聚合根，重推流程按 加载 → 迁移 → save 接线即可；
 * 此处不再提供绕过聚合根的平行 SQL 状态方法。
 */
public interface OfflineMessageRepository {

    OfflineMessage save(OfflineMessage message);

    /** 查询用户待推送（PENDING）的离线消息，按创建时间升序（先离线先补推）。 */
    List<OfflineMessage> findPendingByUserId(String userId);
}
