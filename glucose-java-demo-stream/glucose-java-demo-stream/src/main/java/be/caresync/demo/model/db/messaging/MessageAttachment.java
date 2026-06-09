package be.caresync.demo.model.db.messaging;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "message_attachments", indexes = {
        @Index(name = "idx_msg_attachments_message", columnList = "message_id")
})
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class MessageAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "message_id", nullable = false)
    private Long messageId;

    @NotBlank
    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "uploaded_at")
    private Instant uploadedAt;
}
