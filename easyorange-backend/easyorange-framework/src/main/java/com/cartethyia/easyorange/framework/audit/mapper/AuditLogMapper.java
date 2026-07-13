package com.cartethyia.easyorange.framework.audit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cartethyia.easyorange.framework.audit.entity.AuditLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLog> {

    int deleteExpiredLogs(@Param("expireDate") LocalDateTime expireDate);
}
