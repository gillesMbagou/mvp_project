package be.caresync.demo.model.db.messaging;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "message_threads", indexes = {
        @Index(name = "idx_msg_threads_patient", columnList = "related_patient_id")
})
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class MessageThread {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String subject;

    @Column(name = "related_patient_id")
    private String relatedPatientId;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Builder.Default
    private boolean active = true;
}
