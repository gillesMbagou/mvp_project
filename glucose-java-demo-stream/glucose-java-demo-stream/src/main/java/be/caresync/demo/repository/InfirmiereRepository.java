package be.caresync.demo.repository;

import be.caresync.demo.model.db.staff.Infirmiere;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InfirmiereRepository extends JpaRepository<Infirmiere, String> {

    List<Infirmiere> findByNursingUnit(String nursingUnit);

    List<Infirmiere> findByShift(String shift);

    @Query("SELECT i FROM Infirmiere i JOIN i.assignedPatientIds p WHERE p = :patientId")
    List<Infirmiere> findByAssignedPatient(String patientId);
}
