package be.caresync.demo.repository;

import be.caresync.demo.model.db.rgpd.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    Page<AuditLog> findByPatientIdOrderByEventTimestampDesc(String patientId, Pageable pageable);
    Page<AuditLog> findByUserEmailOrderByEventTimestampDesc(String userEmail, Pageable pageable);
    Page<AuditLog> findAllByOrderByEventTimestampDesc(Pageable pageable);
}
