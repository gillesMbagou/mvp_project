package be.caresync.etablissement.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.List;

/**
 * ETABLISSEMENT — Entité racine du multi-tenant CareSync.
 *
 * Chaque établissement est isolé :
 *   - Ses propres professionnels
 *   - Ses propres patients
 *   - Sa propre configuration (seuils, protocoles)
 *
 * Row Level Security PostgreSQL : les requêtes sont automatiquement
 * filtrées par etablissementId dans chaque service via le JWT Keycloak.
 */
@Entity
@Table(name = "etablissements")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Etablissement extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String finess;          // Numéro FINESS établissement

    @Column(nullable = false)
    private String nom;

    private String adresse;
    private String codePostal;
    private String ville;
    private String pays;
    private String telephone;
    private String emailContact;
    private String siteWeb;

    @Enumerated(EnumType.STRING)
    private TypeEtablissement type;  // CHU / CLINIQUE / CABINET / EHPAD / SSR

    @Enumerated(EnumType.STRING)
    private Statut statut = Statut.ACTIF;

    private String keycloakGroupId;  // ID groupe Keycloak correspondant

    @CreationTimestamp private Instant createdAt;
    @UpdateTimestamp   private Instant updatedAt;

    public enum TypeEtablissement { CHU, CHR, CLINIQUE_PRIVEE, CABINET, EHPAD, SSR, MAISON_SANTE }
    public enum Statut { ACTIF, INACTIF, SUSPENDU }

    // ── Méthodes Panache ───────────────────────────────────────────────
    public static List<Etablissement> findActifs() {
        return find("statut", Statut.ACTIF).list();
    }

    public static Etablissement findByFiness(String finess) {
        return find("finess", finess).firstResult();
    }
}


// ───────────────────────────────────────────────────────────────────────────
// PROFESSIONNEL — Acteur humain dans CareSync
// ───────────────────────────────────────────────────────────────────────────

@Entity
@Table(name = "professionnels")
class Professionnel extends PanacheEntityBase {

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

    @CreationTimestamp private Instant createdAt;
    @UpdateTimestamp   private Instant updatedAt;

    public enum RolePro { MEDECIN, INFIRMIER, AIDE_SOIGNANT, KINESITHERAPEUTE, DIETETICIEN, CHEF_SERVICE, ADMIN, SECRETAIRE }
    public enum StatutPro { ACTIF, INACTIF, EN_CONGE, SUSPENDU }

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
