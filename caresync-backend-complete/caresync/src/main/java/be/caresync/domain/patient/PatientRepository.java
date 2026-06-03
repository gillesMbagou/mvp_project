// ── PatientRepository ─────────────────────────────────────────────────────────
package be.caresync.domain.patient;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PatientRepository extends JpaRepository<Patient, UUID> {

    Optional<Patient> findByEmail(String email);
    Optional<Patient> findByInsNir(String insNir);
    Optional<Patient> findByMedicalRecordNumber(String mrn);
    boolean existsByEmail(String email);
}
