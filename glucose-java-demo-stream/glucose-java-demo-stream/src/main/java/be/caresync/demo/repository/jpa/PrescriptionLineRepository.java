package be.caresync.demo.repository.jpa;

import be.caresync.demo.model.db.prescription.PrescriptionLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PrescriptionLineRepository extends JpaRepository<PrescriptionLine, Long> {
    List<PrescriptionLine> findByPrescriptionId(Long prescriptionId);
}
