package be.caresync.demo.repository.jpa;

import be.caresync.demo.model.db.patient.Vaccination;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VaccinationRepository extends JpaRepository<Vaccination, Long> {
    List<Vaccination> findByPatientIdOrderByAdministrationDateDesc(String patientId);
}
