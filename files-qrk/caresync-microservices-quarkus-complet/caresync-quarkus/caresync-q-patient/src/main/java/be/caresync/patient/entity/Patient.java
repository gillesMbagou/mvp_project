package be.caresync.patient.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.List;

/**
 * Entité Panache — remplace Spring Data JPA + Repository.
 *
 * Différence clé avec Spring Boot :
 *   Spring Boot : entity + interface repository + @Autowired
 *   Quarkus     : entity qui IS son propre repository
 *
 * Avantage : 50% moins de code, même puissance.
 *
 * Utilisation :
 *   Patient.findById("id")
 *   Patient.findAll().page(0, 20).list()
 *   Patient.find("pathology", "DIABETE_T2").list()
 *   patient.persist()
 */
@Entity
@Table(name = "patients")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Patient extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false) private String firstName;
    @Column(nullable = false) private String lastName;

    private int     age;
    private String  gender;
    private String  dateOfBirth;
    private String  insNir;
    @Column(nullable = false) private String pathology;
    private String  service;
    private String  assignedMedicId;

    @Enumerated(EnumType.STRING)
    private RiskLevel riskLevel = RiskLevel.NORMAL;

    private double  baseGlucose;
    private double  baseSpO2;
    private double  baseSystolic;
    private double  weightKg;
    private boolean active = true;

    @CreationTimestamp private Instant createdAt;
    @UpdateTimestamp   private Instant updatedAt;

    public enum RiskLevel { NORMAL, INFORMATIVE, URGENTE, CRITIQUE }

    // ── Méthodes de requête Panache (pas besoin d'interface Repository) ──

    public static List<Patient> findActive() {
        return find("active", true).list();
    }

    public static List<Patient> search(String q) {
        return find("active = true AND (LOWER(firstName) LIKE LOWER(?1) " +
                    "OR LOWER(lastName) LIKE LOWER(?2) " +
                    "OR LOWER(pathology) LIKE LOWER(?3))",
                    "%" + q + "%", "%" + q + "%", "%" + q + "%")
                .list();
    }

    public static List<Patient> findByMedic(String medicId) {
        return find("assignedMedicId = ?1 AND active = true", medicId).list();
    }

    public static long countCritical() {
        return count("riskLevel = ?1 AND active = true", RiskLevel.CRITIQUE);
    }
}
