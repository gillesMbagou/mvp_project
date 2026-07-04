package be.caresync.careplan.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** CARE_PLAN — Plan de soins structuré par pathologie active */
@Entity
@Table(name = "care_plans")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CarePlan extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false) private String patientId;
    @Column(nullable = false) private String patientConditionId;  // Lié à une PATIENT_CONDITION
    @Column(nullable = false) private String medecinId;           // Créateur / prescripteur
    @Column(nullable = false) private String titre;

    private String description;
    private LocalDate dateDebut;
    private LocalDate dateFin;

    @Enumerated(EnumType.STRING)
    private StatutPlan statut = StatutPlan.BROUILLON;

    private String etablissementId;

    @CreationTimestamp private Instant createdAt;
    @UpdateTimestamp   private Instant updatedAt;
    private Instant activatedAt;

    public enum StatutPlan { BROUILLON, ACTIF, SUSPENDU, TERMINE, ANNULE }

    // ── Méthodes Panache ───────────────────────────────────────────────
    public static List<CarePlan> findByPatient(String patientId) {
        return find("patientId = ?1 AND statut = ?2", patientId, StatutPlan.ACTIF).list();
    }

    public static List<CarePlan> findExpiringInDays(int days) {
        LocalDate target = LocalDate.now().plusDays(days);
        return find("statut = ?1 AND dateFin <= ?2", StatutPlan.ACTIF, target).list();
    }
}


/** CARE_PLAN_TASK — Tâche de soins dans un plan */
@Entity
@Table(name = "care_plan_tasks")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
class CarePlanTask extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false) private String carePlanId;
    @Column(nullable = false) private String titre;
    private String description;

    @Enumerated(EnumType.STRING)
    private FrequenceTache frequence;   // DAILY / TWICE_DAILY / WEEKLY / ON_DEMAND

    @Enumerated(EnumType.STRING)
    private StatutTache statut = StatutTache.PLANIFIEE;

    private String assigneeId;          // ID infirmier assigné
    private String deviceType;          // SCALE / GLUCOMETER / PULSE_OXIMETER

    private Instant planifiedAt;
    private Instant executedAt;
    private String  executedByProfessionalId;
    private String  valeurRenseignee;   // Valeur saisie lors de l'exécution
    private String  commentaire;

    @CreationTimestamp private Instant createdAt;

    public enum FrequenceTache { ONCE, DAILY, TWICE_DAILY, THREE_TIMES_DAILY, WEEKLY, ON_DEMAND }
    public enum StatutTache { PLANIFIEE, EN_COURS, DONE, IGNOREE, ANNULEE }

    static List<CarePlanTask> findByPlan(String carePlanId) {
        return find("carePlanId", carePlanId).list();
    }

    static List<CarePlanTask> findTodayByAssignee(String assigneeId) {
        Instant startOfDay = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.DAYS);
        return find("assigneeId = ?1 AND statut = ?2 AND planifiedAt >= ?3",
                assigneeId, StatutTache.PLANIFIEE, startOfDay).list();
    }
}
