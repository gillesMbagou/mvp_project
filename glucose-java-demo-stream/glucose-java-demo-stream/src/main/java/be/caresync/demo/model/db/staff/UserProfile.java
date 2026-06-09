package be.caresync.demo.model.db.staff;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Profil utilisateur commun à tous les professionnels de santé.
 *
 * Classe abstraite {@code @MappedSuperclass} : pas de table propre,
 * chaque sous-classe ({@link Medecin}, {@link Infirmiere}) possède sa table.
 * Les champs communs (identité, contact, rôle) sont hérités.
 */
@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public abstract class UserProfile {

    @Id
    private String id;

    private String firstName;
    private String lastName;
    private String email;
    private String phone;

    @Enumerated(EnumType.STRING)
    private UserRole role;

    public String fullName() {
        return firstName + " " + lastName;
    }
}
