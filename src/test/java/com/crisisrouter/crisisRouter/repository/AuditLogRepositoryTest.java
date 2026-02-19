package com.crisisrouter.crisisRouter.repository;

import com.crisisrouter.crisisRouter.model.entity.AuditLog;
import com.crisisrouter.crisisRouter.testconfig.TestcontainersConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TestcontainersConfig.class)
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AuditLogRepositoryTest {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Test
    void findByEntityIdOrderByChangedAtDesc_returnsOrderedLogs() {
        UUID entityId = UUID.randomUUID();

        AuditLog log1 = AuditLog.builder()
                .entityType("ResourceRequest").entityId(entityId)
                .actionType("CREATE").oldValue("{}").newValue("{\"status\":\"OPEN\"}")
                .changedAt(LocalDateTime.now().minusHours(2)).build();

        AuditLog log2 = AuditLog.builder()
                .entityType("ResourceRequest").entityId(entityId)
                .actionType("CLAIM").oldValue("{\"status\":\"OPEN\"}").newValue("{\"status\":\"CLAIMED\"}")
                .changedAt(LocalDateTime.now().minusHours(1)).build();

        auditLogRepository.saveAndFlush(log1);
        auditLogRepository.saveAndFlush(log2);

        List<AuditLog> result = auditLogRepository.findByEntityIdOrderByChangedAtDesc(entityId);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getChangedAt()).isAfter(result.get(1).getChangedAt());
    }

    @Test
    void findByEntityIdOrderByChangedAtDesc_noLogs_returnsEmpty() {
        List<AuditLog> result = auditLogRepository.findByEntityIdOrderByChangedAtDesc(UUID.randomUUID());

        assertThat(result).isEmpty();
    }
}
