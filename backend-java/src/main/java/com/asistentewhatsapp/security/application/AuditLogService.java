package com.asistentewhatsapp.security.application;

import com.asistentewhatsapp.security.api.AuditLogResponse;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import com.asistentewhatsapp.security.infrastructure.AuditLogJdbcRepository;
import com.asistentewhatsapp.shared.api.PagedResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {

    private final AuditLogJdbcRepository auditLogJdbcRepository;

    public AuditLogService(AuditLogJdbcRepository auditLogJdbcRepository) {
        this.auditLogJdbcRepository = auditLogJdbcRepository;
    }

    @Transactional(readOnly = true)
    public PagedResponse<AuditLogResponse> list(AuthenticatedUser authenticatedUser, int page, int size) {
        return auditLogJdbcRepository.findByBusinessId(authenticatedUser.businessId(), page, size);
    }
}

