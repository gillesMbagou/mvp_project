package be.caresync.demo.model.db.messaging;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "thread_participants", indexes = {
        @Index(name = "idx_thread_participants_thread", columnList = "thread_id"),
        @Index(name = "idx_thread_participants_email", columnList = "professional_email")
})
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ThreadParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "thread_id", nullable = false)
    private Long threadId;

    @NotBlank
    @Column(name = "professional_email", nullable = false)
    private String professionalEmail;

    @Column(name = "professional_name")
    private String professionalName;

    @Column(name = "professional_role")
    private String professionalRole;

    @Column(name = "joined_at")
    private Instant joinedAt;

    @Column(name = "left_at")
    private Instant leftAt;

    @Builder.Default
    private boolean active = true;
}
