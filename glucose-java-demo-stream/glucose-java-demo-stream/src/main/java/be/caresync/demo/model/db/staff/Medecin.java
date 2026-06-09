package be.caresync.demo.model.db.staff;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Médecin — professionnel de santé prescripteur.
 *
 * Hérite de {@link UserProfile} (identité + contact + rôle).
 * Ajoute les attributs métier : spécialité, numéro RPPS,
 * service hospitalier et liste des patients suivis.
 */
@Entity
@Table(name = "medecins")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class Medecin extends UserProfile {

    private String specialty;       // CARDIOLOGIE / DIABETOLOGIE / PNEUMOLOGIE / MEDECINE_GENERALE
    private String rppsNumber;      // Répertoire Partagé des Professionnels de Santé
    private String serviceUnit;     // Ex : "Cardiologie — CHU Saint-Luc"

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "medecin_patients",
        joinColumns = @JoinColumn(name = "medecin_id")
    )
    @Column(name = "patient_id")
    @Builder.Default
    private List<String> assignedPatientIds = new ArrayList<>();
}
