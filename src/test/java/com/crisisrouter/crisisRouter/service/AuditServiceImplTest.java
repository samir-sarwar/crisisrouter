package com.crisisrouter.crisisRouter.service;

import com.crisisrouter.crisisRouter.model.entity.AuditLog;
import com.crisisrouter.crisisRouter.model.entity.User;
import com.crisisrouter.crisisRouter.repository.AuditLogRepository;
import com.crisisrouter.crisisRouter.service.impl.AuditServiceImpl;
import com.crisisrouter.crisisRouter.testutil.TestDataFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditServiceImplTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditServiceImpl auditService;

    @Test
    void logAction_savesAuditLogWithCorrectFields() {
        User actor = TestDataFactory.createUser();
        UUID entityId = UUID.randomUUID();

        auditService.logAction("CLAIM_REQUEST", "ResourceRequest", entityId,
                "{\"status\": \"OPEN\"}", "{\"status\": \"CLAIMED\"}", actor);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertThat(saved.getActionType()).isEqualTo("CLAIM_REQUEST");
        assertThat(saved.getEntityType()).isEqualTo("ResourceRequest");
        assertThat(saved.getEntityId()).isEqualTo(entityId);
        assertThat(saved.getOldValue()).isEqualTo("{\"status\": \"OPEN\"}");
        assertThat(saved.getNewValue()).isEqualTo("{\"status\": \"CLAIMED\"}");
        assertThat(saved.getChangedBy()).isEqualTo(actor);
        assertThat(saved.getChangedAt()).isNotNull();
    }
}
