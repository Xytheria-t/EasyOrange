package com.cartethyia.easyorange.framework.audit.service.impl;

import com.cartethyia.easyorange.framework.audit.entity.AuditLog;
import com.cartethyia.easyorange.common.idgen.IdGenerator;
import com.cartethyia.easyorange.framework.audit.mapper.AuditLogMapper;
import com.cartethyia.easyorange.framework.audit.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogMapper auditLogMapper;
    private final IdGenerator idGenerator;

    @Override
    public void insertAuditLog(AuditLog auditLog) {
        if (auditLog.getId() == null) {
            auditLog.setId(idGenerator.generateId());
        }
        auditLogMapper.insert(auditLog);
    }
}
