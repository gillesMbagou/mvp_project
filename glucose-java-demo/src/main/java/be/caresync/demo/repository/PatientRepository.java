package be.caresync.demo.repository;

import be.caresync.demo.model.db.PatientProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PatientRepository extends JpaRepository<PatientProfile, String> {}
