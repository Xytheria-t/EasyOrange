package com.cartethyia.easyorange.framework.audit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cartethyia.easyorange.framework.audit.entity.AuditLog;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLog> {

    @Delete("DELETE FROM eo_audit_log WHERE created_at < #{expireDate} LIMIT 1000")
    int deleteExpiredLogs(@Param("expireDate") LocalDateTime expireDate);
}
