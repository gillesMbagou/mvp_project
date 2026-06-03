package be.caresync.domain.alert;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AlertType {
    BIOLOGIQUE      ("Valeur biologique hors seuil"),
    MEDICAMENTEUSE  ("Interaction ou contre-indication médicamenteuse"),
    IOT             ("Mesure dispositif IoT hors seuil"),
    OBSERVANCE      ("Non-respect du plan de soins"),
    PREDICTIVE      ("Score de risque de décompensation élevé"),
    DISPOSITIF      ("Perte de signal ou défaillance dispositif"),
    MANUELLE        ("Alerte créée manuellement par un professionnel");

    private final String description;
}
