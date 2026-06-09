package be.caresync.demo.model.db;

import jakarta.persistence.*;
import lombok.*;

/**
 * Profil patient simulé en base H2.
 * Un patient peut avoir plusieurs dispositifs médicaux associés.
 */
@Entity
@Table(name = "patients")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientProfile {

    @Id
    private String id;              // UUID fixe pour reproductibilité

    private String firstName;
    private String lastName;
    private int    age;
    private String pathology;       // DIABETE_T2 / ICFE / BPCO
    private String gender;          // M / F
    private double weightKg;
    private double heightCm;

    // Contexte clinique pour le simulateur
    private double baseGlucose;     // glycémie à jeun de référence (g/L)
    private double baseSpO2;        // SpO2 de référence (%)
    private double baseSystolic;    // PA systolique de référence (mmHg)
    private double baseDiastolic;   // PA diastolique de référence (mmHg)

    public String fullName() {
        return firstName + " " + lastName;
    }
}
