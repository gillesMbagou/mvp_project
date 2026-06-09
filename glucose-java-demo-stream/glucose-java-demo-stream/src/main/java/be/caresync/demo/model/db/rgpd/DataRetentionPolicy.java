package be.caresync.demo.model.db.rgpd;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "data_retention_policies")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class DataRetentionPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "data_category", nullable = false, unique = true)
    private String dataCategory;

    @NotNull
    @Column(name = "retention_years", nullable = false)
    private Integer retentionYears;

    @Column(name = "legal_basis")
    private String legalBasis;

    @Column(name = "regulatory_reference")
    private String regulatoryReference;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "auto_purge")
    @Builder.Default
    private boolean autoPurge = false;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
