package be.caresync.careplan.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.List;

/**
 * CARE_PLAN_TASK — Tâche de soins dans un plan
 */
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "care_plan_tasks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class CarePlanTask extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String carePlanId;
    @Column(nullable = false)
    private String titre;
    private String description;

    @Enumerated(EnumType.STRING)
    private FrequenceTache frequence;   // DAILY / TWICE_DAILY / WEEKLY / ON_DEMAND

    @Enumerated(EnumType.STRING)
    private StatutTache statut = StatutTache.PLANIFIEE;

    private String assigneeId;          // ID infirmier assigné
    private String deviceType;          // SCALE / GLUCOMETER / PULSE_OXIMETER

    private Instant planifiedAt;
    private Instant executedAt;
    private String executedByProfessionalId;
    private String valeurRenseignee;   // Valeur saisie lors de l'exécution
    private String commentaire;

    @CreationTimestamp
    private Instant createdAt;

    public enum FrequenceTache {ONCE, DAILY, TWICE_DAILY, THREE_TIMES_DAILY, WEEKLY, ON_DEMAND}

    public enum StatutTache {PLANIFIEE, EN_COURS, DONE, IGNOREE, ANNULEE}

    static List<CarePlanTask> findByPlan(String carePlanId) {
        return find("carePlanId", carePlanId).list();
    }

    static List<CarePlanTask> findTodayByAssignee(String assigneeId) {
        Instant startOfDay = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.DAYS);
        return find("assigneeId = ?1 AND statut = ?2 AND planifiedAt >= ?3",
                assigneeId, StatutTache.PLANIFIEE, startOfDay).list();
    }
}
