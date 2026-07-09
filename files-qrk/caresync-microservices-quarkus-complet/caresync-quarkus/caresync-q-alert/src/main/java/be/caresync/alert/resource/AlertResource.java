package be.caresync.alert.resource;

import be.caresync.alert.entity.Alert;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.annotation.security.RolesAllowed;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.time.Instant;
import java.util.List;

/**
 * AlertResource — UC-07 Alertes cliniques.
 *
 * Les alertes elles-mêmes sont produites par AlertProcessor (Kafka
 * iot-observations → clinical-alerts), qui les persiste maintenant aussi en
 * base (voir Alert entity) pour que cette Resource ait quelque chose à lire —
 * jusqu'ici purement Kafka-to-Kafka, sans trace consultable côté REST.
 */
@Path("/api/v1/alertes")
@Produces(MediaType.APPLICATION_JSON)
public class AlertResource {

    @GET
    @RolesAllowed({"medecin", "infirmier", "admin_etablissement", "super_admin"})
    @RunOnVirtualThread
    public List<Alert> list(@QueryParam("patientId") String patientId,
                             @QueryParam("hours") @DefaultValue("24") int hours) {
        return Alert.findRecent(patientId, hours);
    }

    @PATCH
    @Path("/{id}/acquitter")
    @Transactional
    @RolesAllowed({"medecin", "infirmier"})
    @RunOnVirtualThread
    public Alert acquitter(@PathParam("id") String id) {
        Alert alert = Alert.findById(id);
        if (alert == null) throw new NotFoundException("Alerte introuvable: " + id);
        alert.setStatut(Alert.Statut.ACQUITTEE);
        return alert;
    }

    @PATCH
    @Path("/{id}/resoudre")
    @Transactional
    @RolesAllowed({"medecin", "infirmier"})
    @RunOnVirtualThread
    public Alert resoudre(@PathParam("id") String id) {
        Alert alert = Alert.findById(id);
        if (alert == null) throw new NotFoundException("Alerte introuvable: " + id);
        alert.setStatut(Alert.Statut.RESOLUE);
        alert.setResolvedAt(Instant.now());
        return alert;
    }
}
