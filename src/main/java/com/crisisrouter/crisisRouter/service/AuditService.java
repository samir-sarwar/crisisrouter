package com.crisisrouter.crisisRouter.service;

import com.crisisrouter.crisisRouter.model.entity.User;
import java.util.UUID;

public interface AuditService {
    void logAction(String action,
                   String entityName,
                   UUID entityId,
                   String oldValue,
                   String newValue,
                   User actor);
}