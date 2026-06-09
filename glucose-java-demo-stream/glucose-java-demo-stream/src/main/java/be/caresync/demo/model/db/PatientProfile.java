package be.caresync.demo.model.db;

import be.caresync.demo.model.db.patient.ClinicalContext;
import be.caresync.demo.model.db.patient.Patient;
import jakarta.persistence.*;
import lombok.*;

/**
 * Agrégat racine représentant un patient dans le système CareSync.
 *
 * Délègue à deux sous-entités conformément au principe de responsabilité unique :
 *   - {@link Patient}         : identité démographique (nom, âge, genre, pathologie…)
 *   - {@link ClinicalContext} : paramètres physiologiques de référence pour le simulateur
 *
 * L'identifiant de PatientProfile est partagé avec Patient via {@code @MapsId} :
 * les trois tables (patient_profiles, patients, clinical_contexts) utilisent
 * le même UUID comme clé.
 */
@Entity
@Table(name = "patient_profiles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientProfile {

    @Id
    private String id;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @MapsId
    @JoinColumn(name = "id")
    private Patient patient;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id", referencedColumnName = "patient_id",
                insertable = false, updatable = false)
    private ClinicalContext clinicalContext;

    // Raccourcis pratiques vers les sous-entités

    public String fullName() {
        return patient != null ? patient.fullName() : "";
    }

    public String getPathology() {
        return patient != null ? patient.getPathology() : null;
    }

    public double getWeightKg() {
        return patient != null ? patient.getWeightKg() : 0.0;
    }

    public double getBaseGlucose() {
        return clinicalContext != null ? clinicalContext.getBaseGlucose() : 0.0;
    }

    public double getBaseSpO2() {
        return clinicalContext != null ? clinicalContext.getBaseSpO2() : 0.0;
    }

    public double getBaseSystolic() {
        return clinicalContext != null ? clinicalContext.getBaseSystolic() : 0.0;
    }
}
