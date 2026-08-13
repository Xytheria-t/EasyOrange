package com.cartethyia.easyorange.framework.audit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cartethyia.easyorange.framework.audit.entity.AuditLog;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLog> {

    /**
     * 分批删除保留期限前的审计记录（LIMIT 1000 防长事务持锁），
     * 由 {@link com.cartethyia.easyorange.framework.audit.job.AuditLogCleanupTask} 每日循环调用直至返回不足一批。
     *
     * @param expireDate 保留期限截止时间，早于该时间的记录被删除
     * @return 本次删除条数
     */
    @Delete("DELETE FROM eo_audit_log WHERE created_at < #{expireDate} LIMIT 1000")
    int deleteExpiredLogs(@Param("expireDate") LocalDateTime expireDate);
}
