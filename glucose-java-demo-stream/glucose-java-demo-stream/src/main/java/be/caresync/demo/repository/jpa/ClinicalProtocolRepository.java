package be.caresync.demo.repository.jpa;

import be.caresync.demo.model.db.careplan.ClinicalProtocol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClinicalProtocolRepository extends JpaRepository<ClinicalProtocol, Long> {
    List<ClinicalProtocol> findByActiveTrue();
    Optional<ClinicalProtocol> findByCode(String code);
    List<ClinicalProtocol> findByPathologyAndActiveTrue(String pathology);
}
