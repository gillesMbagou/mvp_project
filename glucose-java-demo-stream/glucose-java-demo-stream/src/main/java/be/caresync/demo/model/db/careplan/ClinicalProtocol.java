package be.caresync.demo.model.db.careplan;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "clinical_protocols")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ClinicalProtocol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, unique = true)
    private String code;

    @NotBlank
    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProtocolSource source;

    private String version;
    private String pathology;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "recommended_tasks_json", columnDefinition = "TEXT")
    private String recommendedTasksJson;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    public enum ProtocolSource { HAS, SFAR, SFC, ANSM, ESCARDIO, INTERNAL }
}
