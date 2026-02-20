package com.crisisrouter.crisisRouter.repository;

import com.crisisrouter.crisisRouter.model.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByEntityIdOrderByChangedAtDesc(UUID entityId);
}
