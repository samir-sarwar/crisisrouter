package com.crisisrouter.crisisRouter.service.impl;

import com.crisisrouter.crisisRouter.model.entity.AuditLog;
import com.crisisrouter.crisisRouter.model.entity.User;
import com.crisisrouter.crisisRouter.repository.AuditLogRepository;
import com.crisisrouter.crisisRouter.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;

    /**
     * @Transactional(propagation = Propagation.REQUIRES_NEW)
     * This ensures the log is saved even if the main transaction fails later.
     * Usually, you want audit logs to persist no matter what.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(String action, String entityName, UUID entityId, String oldValue, String newValue, User actor) {
        AuditLog log = new AuditLog();
        log.setActionType(action);
        log.setEntityType(entityName);
        log.setEntityId(entityId);
        log.setOldValue(oldValue);
        log.setNewValue(newValue);
        log.setChangedBy(actor);
        log.setChangedAt(LocalDateTime.now());

        auditLogRepository.save(log);
    }
}