package be.caresync.demo.repository;

import be.caresync.demo.model.db.staff.Medecin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedecinRepository extends JpaRepository<Medecin, String> {

    List<Medecin> findBySpecialty(String specialty);

    @Query("SELECT m FROM Medecin m JOIN m.assignedPatientIds p WHERE p = :patientId")
    List<Medecin> findByAssignedPatient(String patientId);
}
