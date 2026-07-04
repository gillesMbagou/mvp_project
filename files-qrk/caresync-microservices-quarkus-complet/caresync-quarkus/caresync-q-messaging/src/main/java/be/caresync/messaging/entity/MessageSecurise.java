package be.caresync.messaging.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;
import java.util.List;

/**
 * MessageSecurise — entité Panache.
 * Le contenu est chiffré AES-256-GCM en base.
 * La clé de chiffrement est stockée dans Vault, associée à l'expediteur.
 */
@Entity
@Table(name = "messages_securises")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class MessageSecurise extends PanacheEntityBase {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String expediteurId;

    @Column(nullable = false)
    private String objet;

    /** Contenu chiffré AES-256-GCM, Base64 encodé */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String contenuChiffre;

    @Enumerated(EnumType.STRING)
    private Priorite priorite = Priorite.NORMALE;

    /** Référence optionnelle au patient concerné */
    private String patientRef;

    private boolean accuseReception = false;
    private boolean archivé = false;

    @CreationTimestamp
    private Instant creeAt;

    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL)
    private List<MessageDestinataire> destinataires;

    public enum Priorite { NORMALE, URGENTE, CRITIQUE }

    // ── Requêtes Panache ──────────────────────────────────────────────────────
    public static List<MessageSecurise> findByExpediteur(String expediteurId) {
        return find("expediteurId", expediteurId).list();
    }

    public static List<MessageSecurise> findByPatient(String patientId, String actorId) {
        return find("patientRef = ?1 AND expediteurId = ?2", patientId, actorId).list();
    }
}
