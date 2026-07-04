package be.caresync.prescription.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** MEDICAMENT — Référentiel médicaments (base Theriaque / Vidal) */
@Entity
@Table(name = "medicaments")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
class Medicament extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String codeATC;          // Code ATC OMS (ex: C03CA01 = Furosémide)

    @Column(nullable = false, unique = true)
    private String codeCIS;          // Code CIS ANSM (identifiant France)

    @Column(nullable = false)
    private String denominationCommune; // DCI

    @Column(nullable = false)
    private String nomCommercial;

    private String dosage;            // ex: "40 mg"
    private String formeGalenique;   // COMPRIMES / INJECTABLE / PATCH / GOUTTES
    private String voieAdministration; // ORALE / IV / SC / NASALE

    private boolean interactions;     // flag : présence interactions connues
    private String classesInteragissantes; // CSV des codes ATC avec interactions

    private boolean actif = true;

    public static Medicament findByATC(String codeATC) {
        return find("codeATC", codeATC).firstResult();
    }

    public static List<Medicament> search(String q) {
        return find("LOWER(denominationCommune) LIKE LOWER(?1) OR LOWER(nomCommercial) LIKE LOWER(?2)",
                "%" + q + "%", "%" + q + "%").list();
    }
}


/** PRESCRIPTION — Ordonnance électronique signée */
@Entity
@Table(name = "prescriptions")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
class Prescription extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false) private String patientId;
    @Column(nullable = false) private String prescripteurId; // ID professionnel

    @Enumerated(EnumType.STRING)
    private StatutPrescription statut = StatutPrescription.BROUILLON;

    private LocalDate dateEmission;
    private LocalDate dateExpiration;   // +30 jours légal pour les stupéfiants

    private String signatureElectronique; // Base64 certificat RPPS
    private Instant signedAt;
    private String  certificatRPPS;       // Numéro RPPS prescripteur

    private boolean renouvellementPossible;
    private int     nombreRenouvellements;
    private String  prescriptionParentId; // Si renouvellement

    @CreationTimestamp private Instant createdAt;

    public enum StatutPrescription { BROUILLON, SIGNEE, DELIVREE, EXPIREE, ANNULEE }

    static List<Prescription> findByPatient(String patientId) {
        return find("patientId = ?1 AND statut NOT IN ?2",
                patientId, List.of(StatutPrescription.EXPIREE, StatutPrescription.ANNULEE)).list();
    }

    static List<Prescription> findExpiringInDays(int days) {
        LocalDate threshold = LocalDate.now().plusDays(days);
        return find("statut = ?1 AND dateExpiration <= ?2", StatutPrescription.SIGNEE, threshold).list();
    }
}


/** PRESCRIPTION_LINE — Une ligne de médicament dans l'ordonnance */
@Entity
@Table(name = "prescription_lines")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
class PrescriptionLine extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false) private String prescriptionId;
    @Column(nullable = false) private String medicamentId;

    @NotNull private String posologie;    // ex: "1 comprimé le matin à jeun"
    @Positive private int   dureeJours;  // ex: 30

    private String voieSpecifique;        // Si différente du médicament
    private String instructionsSpeciales; // ex: "avec beaucoup d'eau"
    private String alerteInteraction;     // Message si interaction détectée
    private boolean interactionConfirmee; // Médecin a confirmé malgré l'interaction

    static List<PrescriptionLine> findByPrescription(String prescriptionId) {
        return find("prescriptionId", prescriptionId).list();
    }
}
