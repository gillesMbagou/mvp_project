package be.caresync.domain.iot;

import be.caresync.domain.common.BaseEntity;
import be.caresync.domain.patient.Patient;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "iot_devices")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IoTDevice extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeviceType deviceType;

    @NotBlank
    @Column(nullable = false)
    private String manufacturer;

    @NotBlank
    @Column(nullable = false)
    private String model;

    @NotBlank
    @Column(nullable = false, unique = true)
    private String serialNumber;

    // Topic MQTT sur lequel ce dispositif publie
    @Column(nullable = false)
    private String mqttTopic;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Protocol protocol = Protocol.MQTT;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    private Instant lastSeenAt;

    @Column(name = "firmware_version")
    private String firmwareVersion;

    public enum Protocol { MQTT, BLE, WIFI, LPWAN }
}
