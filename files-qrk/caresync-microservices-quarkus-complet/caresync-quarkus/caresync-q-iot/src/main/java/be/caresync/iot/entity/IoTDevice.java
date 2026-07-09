package be.caresync.iot.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.List;

/**
 * IOT_DEVICE — registre des dispositifs. Alimenté de deux façons :
 *   1. Auto-provisioning : IoTProcessor fait un upsert à chaque observation
 *      reçue (device jamais explicitement enregistré mais bien réel).
 *   2. Enregistrement explicite via IoTDeviceResource.register() (device
 *      connu à l'avance, ex: matériel fourni par l'établissement).
 * Auparavant aucune Resource REST n'existait : /api/v1/iot-devices n'était
 * que planifiée (voir cucumber-features), la page "Dispositifs" du frontend
 * ne pouvait montrer que les devices ayant émis une mesure DANS LA MINUTE
 * précédente via SSE, jamais un registre stable.
 */
@Entity
@Table(name = "iot_devices")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class IoTDevice extends PanacheEntityBase {

    @Id
    private String serial;

    private String patientId;

    @Column(nullable = false) private String deviceType;
    private String manufacturer;
    private String model;
    private String loincCode;
    private String unit;

    private boolean active;
    private String currentScenario;

    private Instant registeredAt;
    private Instant lastObservedAt;
    private Double lastValue;

    public static List<IoTDevice> findByPatient(String patientId) {
        return find("patientId", patientId).list();
    }
}
