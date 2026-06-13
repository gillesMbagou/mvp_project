package be.caresync.demo.repository.jpa;

import be.caresync.demo.model.db.PatientProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PatientRepository extends JpaRepository<PatientProfile, String> {

    List<PatientProfile> findByPatient_Pathology(String pathology);
}
