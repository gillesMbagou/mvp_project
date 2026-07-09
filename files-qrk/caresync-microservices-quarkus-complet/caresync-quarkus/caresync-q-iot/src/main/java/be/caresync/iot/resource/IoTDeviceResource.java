package be.caresync.iot.resource;

import be.caresync.iot.entity.IoTDevice;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.annotation.security.RolesAllowed;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.Instant;
import java.util.List;

/**
 * IoTDeviceResource — enregistrement/consultation du registre de dispositifs
 * (endpoint prévu par les features mais jamais codé — voir IoTDevice.java).
 */
@Path("/api/v1/iot-devices")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class IoTDeviceResource {

    @GET
    @RolesAllowed({"medecin", "infirmier", "admin"})
    @RunOnVirtualThread
    public List<IoTDevice> list(@QueryParam("patientId") String patientId) {
        return patientId != null && !patientId.isBlank()
                ? IoTDevice.findByPatient(patientId)
                : IoTDevice.listAll();
    }

    @POST
    @Transactional
    @RolesAllowed({"medecin", "infirmier", "admin"})
    @RunOnVirtualThread
    public Response register(@Valid RegisterRequest req) {
        if (IoTDevice.findById(req.serial()) != null) {
            throw new WebApplicationException("Dispositif déjà enregistré: " + req.serial(), 409);
        }
        IoTDevice device = IoTDevice.builder()
                .serial(req.serial())
                .patientId(req.patientId())
                .deviceType(req.deviceType())
                .manufacturer(req.manufacturer())
                .model(req.model())
                .loincCode(req.loincCode())
                .unit(req.unit())
                .active(true)
                .registeredAt(Instant.now())
                .build();
        device.persist();
        return Response.status(201).entity(device).build();
    }

    @PATCH
    @Path("/{serial}")
    @Transactional
    @RolesAllowed({"medecin", "infirmier", "admin"})
    @RunOnVirtualThread
    public IoTDevice update(@PathParam("serial") String serial, UpdateRequest req) {
        IoTDevice device = IoTDevice.findById(serial);
        if (device == null) throw new NotFoundException("Dispositif introuvable: " + serial);

        if (req.patientId() != null) device.setPatientId(req.patientId());
        if (req.active() != null) device.setActive(req.active());
        if (req.currentScenario() != null) device.setCurrentScenario(req.currentScenario());
        return device;
    }

    public record RegisterRequest(
            @NotBlank String serial,
            String patientId,
            @NotBlank String deviceType,
            String manufacturer,
            String model,
            String loincCode,
            String unit) {}

    public record UpdateRequest(String patientId, Boolean active, String currentScenario) {}
}
