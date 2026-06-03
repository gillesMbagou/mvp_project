package be.caresync.domain.patient;

import be.caresync.domain.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Entity
@Table(name = "patients")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Patient extends BaseEntity {

    @NotBlank
    @Column(nullable = false)
    private String firstName;

    @NotBlank
    @Column(nullable = false)
    private String lastName;

    @Email
    @Column(nullable = false, unique = true)
    private String email;

    // Identifiant national de santé (chiffré via Vault Transit)
    @Column(name = "ins_nir")
    private String insNir;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    @Column(name = "date_of_birth")
    private java.time.LocalDate dateOfBirth;

    @Column(name = "medical_record_number", unique = true)
    private String medicalRecordNumber;

    // Pathologie principale (simplifié pour ce contexte)
    @Enumerated(EnumType.STRING)
    private Pathology primaryPathology;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    public enum Gender      { M, F, OTHER }
    public enum Pathology   { DIABETE_T2, ICFe, BPCO, HTA, CANCER, AUTRE }
}
