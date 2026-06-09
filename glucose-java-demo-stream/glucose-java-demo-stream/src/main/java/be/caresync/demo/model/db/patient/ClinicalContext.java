package be.caresync.demo.model.db.patient;

import jakarta.persistence.*;
import lombok.*;

/**
 * Contexte clinique d'un patient — valeurs de référence physiologiques
 * utilisées par le simulateur pour générer des mesures réalistes.
 *
 * Relation 1:1 avec {@link Patient} via {@code patientId}.
 * Séparé de Patient pour respecter le principe de responsabilité unique :
 * Patient = identité, ClinicalContext = paramètres de simulation clinique.
 */
@Entity
@Table(name = "clinical_contexts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClinicalContext {

    @Id
    private String patientId;           // FK logique vers Patient.id (1:1)

    private double baseGlucose;         // Glycémie à jeun de référence (g/L)
    private double baseSpO2;            // Saturation O₂ de référence (%)
    private double baseSystolic;        // Pression systolique de référence (mmHg)
    private double baseDiastolic;       // Pression diastolique de référence (mmHg)
}
