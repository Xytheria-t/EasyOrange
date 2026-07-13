package com.cartethyia.easyorange.framework.audit.service;

import com.cartethyia.easyorange.framework.audit.entity.AuditLog;

public interface AuditLogService {

    void insertAuditLog(AuditLog auditLog);
}
