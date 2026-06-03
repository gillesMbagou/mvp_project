package be.caresync.controller.iot;

import be.caresync.dto.request.RegisterDeviceRequest;
import be.caresync.dto.response.IoTDeviceResponse;
import be.caresync.service.iot.IoTDeviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/iot/devices")
@RequiredArgsConstructor
@Tag(name = "Dispositifs IoT", description = "Gestion des dispositifs médicaux connectés")
public class IoTDeviceController {

    private final IoTDeviceService deviceService;

    @PostMapping
    @Operation(summary = "Enregistrer un nouveau dispositif IoT")
    @PreAuthorize("hasAnyRole('ADMIN','MEDECIN')")
    public ResponseEntity<IoTDeviceResponse> register(
            @RequestBody @Valid RegisterDeviceRequest req) {

        IoTDeviceResponse response = deviceService.register(req);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(response.id()).toUri();

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/patient/{patientId}")
    @Operation(summary = "Dispositifs actifs d'un patient")
    public ResponseEntity<List<IoTDeviceResponse>> getForPatient(
            @PathVariable UUID patientId) {
        return ResponseEntity.ok(deviceService.getDevicesForPatient(patientId));
    }

    @GetMapping("/{deviceId}")
    @Operation(summary = "Détail d'un dispositif")
    public ResponseEntity<IoTDeviceResponse> getDevice(@PathVariable UUID deviceId) {
        return ResponseEntity.ok(deviceService.getDevice(deviceId));
    }

    @DeleteMapping("/{deviceId}")
    @Operation(summary = "Désactiver un dispositif")
    @PreAuthorize("hasAnyRole('ADMIN','MEDECIN')")
    public ResponseEntity<Void> deactivate(@PathVariable UUID deviceId) {
        deviceService.deactivate(deviceId);
        return ResponseEntity.noContent().build();
    }
}
