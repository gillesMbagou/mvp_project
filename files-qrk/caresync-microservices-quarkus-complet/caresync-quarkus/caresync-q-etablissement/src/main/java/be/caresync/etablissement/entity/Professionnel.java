package be.caresync.etablissement.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "professionnels")
@Data
public class Professionnel extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String etablissementId;  // FK vers Etablissement

    @Column(nullable = false)
    private String keycloakUserId;   // UUID Keycloak

    @Column(nullable = false)
    private String prenom;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false, unique = true)
    private String email;

    private String telephone;

    @Column(unique = true)
    private String rpps;             // Répertoire Partagé des Professionnels de Santé

    @Enumerated(EnumType.STRING)
    private RolePro role;            // MEDECIN / INFIRMIER / AIDE_SOIGNANT / ADMIN / CHEF_SERVICE

    private String service;          // Cardiologie / Pneumologie / Endocrinologie...
    private String specialite;

    @Enumerated(EnumType.STRING)
    private StatutPro statut = StatutPro.ACTIF;

    @CreationTimestamp
    private Instant createdAt;
    @UpdateTimestamp
    private Instant updatedAt;

    public enum RolePro {MEDECIN, INFIRMIER, AIDE_SOIGNANT, KINESITHERAPEUTE, DIETETICIEN, CHEF_SERVICE, ADMIN, SECRETAIRE}

    public enum StatutPro {ACTIF, INACTIF, EN_CONGE, SUSPENDU}

    // ── Méthodes Panache ───────────────────────────────────────────────
    static List<Professionnel> findByEtablissement(String etabId) {
        return find("etablissementId = ?1 AND statut = ?2", etabId, StatutPro.ACTIF).list();
    }

    static Professionnel findByKeycloakId(String keycloakId) {
        return find("keycloakUserId", keycloakId).firstResult();
    }

    static List<Professionnel> findMedecins(String etabId) {
        return find("etablissementId = ?1 AND role = ?2 AND statut = ?3",
                etabId, RolePro.MEDECIN, StatutPro.ACTIF).list();
    }
}
