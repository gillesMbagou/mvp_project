package be.caresync.demo.model.db.messaging;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "secure_messages", indexes = {
        @Index(name = "idx_secure_msg_thread", columnList = "thread_id"),
        @Index(name = "idx_secure_msg_sender", columnList = "sender_email")
})
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SecureMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "thread_id", nullable = false)
    private Long threadId;

    @NotBlank
    @Column(name = "sender_email", nullable = false)
    private String senderEmail;

    @Column(name = "sender_name")
    private String senderName;

    @NotBlank
    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private MessageStatus status = MessageStatus.SENT;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "edited_at")
    private Instant editedAt;

    @Builder.Default
    private boolean deleted = false;

    public enum MessageStatus { DRAFT, SENT, DELIVERED, READ }
}
