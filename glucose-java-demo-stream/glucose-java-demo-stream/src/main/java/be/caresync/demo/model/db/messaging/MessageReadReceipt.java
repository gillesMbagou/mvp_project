package be.caresync.demo.model.db.messaging;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "message_read_receipts", indexes = {
        @Index(name = "idx_msg_receipts_message", columnList = "message_id"),
        @Index(name = "idx_msg_receipts_reader", columnList = "reader_email")
})
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class MessageReadReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "message_id", nullable = false)
    private Long messageId;

    @NotBlank
    @Column(name = "reader_email", nullable = false)
    private String readerEmail;

    @Column(name = "read_at")
    private Instant readAt;
}
