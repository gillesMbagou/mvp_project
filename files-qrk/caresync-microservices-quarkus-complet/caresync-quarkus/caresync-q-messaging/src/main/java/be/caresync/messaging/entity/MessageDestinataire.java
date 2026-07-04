package be.caresync.messaging.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/** MessageDestinataire — un destinataire d'un MessageSecurise et son accusé de lecture. */
@Entity
@Table(name = "message_destinataires")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class MessageDestinataire extends PanacheEntityBase {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne
    @JoinColumn(name = "message_id", nullable = false)
    private MessageSecurise message;

    @Column(nullable = false)
    private String destinataireId;

    private boolean lu = false;
    private Instant luAt;
}
