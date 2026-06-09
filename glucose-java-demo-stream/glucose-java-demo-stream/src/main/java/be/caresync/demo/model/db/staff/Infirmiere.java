package be.caresync.demo.model.db.staff;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Infirmier(e) — professionnel(le) de santé en charge des soins quotidiens.
 *
 * Hérite de {@link UserProfile} (identité + contact + rôle).
 * Ajoute les attributs métier : diplôme, unité de soins,
 * roulement et liste des patients pris en charge.
 */
@Entity
@Table(name = "infirmieres")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class Infirmiere extends UserProfile {

    private String diplomeCode;     // IDE / IBODE / IADE
    private String nursingUnit;     // Unité de soins : "USIC", "Pneumologie", "Diabétologie"
    private String shift;           // MATIN / APRES_MIDI / NUIT

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "infirmiere_patients",
        joinColumns = @JoinColumn(name = "infirmiere_id")
    )
    @Column(name = "patient_id")
    @Builder.Default
    private List<String> assignedPatientIds = new ArrayList<>();
}
