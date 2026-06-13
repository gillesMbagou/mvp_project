package be.caresync.demo.service;

import be.caresync.demo.model.db.rgpd.AuditLog;
import be.caresync.demo.repository.jpa.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepo;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String action, String resourceType, String resourceId,
                    String patientId, String userEmail, String userRole) {
        AuditLog entry = AuditLog.builder()
                .action(action)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .patientId(patientId)
                .userEmail(userEmail)
                .userRole(userRole)
                .eventTimestamp(Instant.now())
                .outcome("SUCCESS")
                .build();
        auditLogRepo.save(entry);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> getByPatient(String patientId, Pageable pageable) {
        return auditLogRepo.findByPatientIdOrderByEventTimestampDesc(patientId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> getAll(Pageable pageable) {
        return auditLogRepo.findAllByOrderByEventTimestampDesc(pageable);
    }
}
