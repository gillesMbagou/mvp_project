package be.caresync.demo.model.db.patient;

import jakarta.persistence.*;
import lombok.*;

/**
 * Identité du patient — données démographiques et pathologie principale.
 * Ne contient aucun contexte clinique ni baseline physiologique
 * (voir {@link ClinicalContext}).
 */
@Entity
@Table(name = "patients")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Patient {

    @Id
    private String id;

    private String firstName;
    private String lastName;
    private int    age;
    private String gender;      // M / F
    private double weightKg;
    private double heightCm;
    private String pathology;   // DIABETE_T2 / ICFE / BPCO

    public String fullName() {
        return firstName + " " + lastName;
    }
}
