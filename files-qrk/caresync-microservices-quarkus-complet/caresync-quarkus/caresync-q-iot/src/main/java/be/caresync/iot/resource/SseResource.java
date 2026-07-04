package be.caresync.iot.resource;

import be.caresync.common.events.IoTObservationEvent;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.reactive.messaging.Channel;
import io.smallrye.mutiny.Multi;
import org.jboss.resteasy.reactive.RestStreamElementType;

/**
 * SseResource — RESTEasy Reactive + SmallRye Channels.
 *
 * Comparaison avec Spring Boot :
 * ─────────────────────────────────────────────────────────────────
 * Spring Boot WebFlux                   | Quarkus RESTEasy Reactive
 * produces = TEXT_EVENT_STREAM_VALUE    | @Produces(SERVER_SENT_EVENTS)
 * Flux<ServerSentEvent<T>>              | Multi<T> (Mutiny)
 * sink.asFlux()                         | @Channel("...") Multi<T>
 * .map(e -> ServerSentEvent.builder()   | (automatique via @RestStreamElementType)
 * .retry(Duration.ofSeconds(3))         | config: reconnect-interval-ms
 * ─────────────────────────────────────────────────────────────────
 *
 * Le @Channel injecte directement un Multi<T> depuis le canal Kafka.
 * Quarkus crée le fan-out automatiquement pour plusieurs abonnés.
 * Beaucoup moins de code que Sinks.Many !
 *
 * Configuration application.properties :
 *   mp.messaging.incoming.observations-sse.connector=smallrye-kafka
 *   mp.messaging.incoming.observations-sse.topic=caresync.iot.observations
 *   mp.messaging.incoming.observations-sse.broadcast=true  ← fan-out auto!
 */
@Path("/api/stream")
@ApplicationScoped
public class SseResource {

    /**
     * Canal Kafka → Multi<T>.
     * broadcast=true dans config → tous les abonnés SSE reçoivent chaque message.
     * C'est l'équivalent du Sinks.Many de Spring Boot en UNE annotation.
     */
    @Channel("observations-sse")
    Multi<IoTObservationEvent> observations;

    @GET
    @Path("/observations")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.APPLICATION_JSON)
    @RolesAllowed({"infirmier", "medecin", "admin"})
    public Multi<IoTObservationEvent> streamAll(@QueryParam("patientId") String patientId) {
        if (patientId != null) {
            return observations
                    .filter(obs -> patientId.equals(obs.getPatientId()));
        }
        return observations;
    }

    @GET
    @Path("/patients/{patientId}/observations")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.APPLICATION_JSON)
    @RolesAllowed({"infirmier", "medecin", "admin"})
    public Multi<IoTObservationEvent> streamByPatient(@PathParam("patientId") String patientId) {
        return observations.filter(obs -> patientId.equals(obs.getPatientId()));
    }

    @GET
    @Path("/alerts")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.APPLICATION_JSON)
    @RolesAllowed({"infirmier", "medecin", "admin"})
    public Multi<IoTObservationEvent> streamAlerts() {
        return observations.filter(obs ->
                "CRITIQUE".equals(obs.getSeverity()) ||
                "URGENTE".equals(obs.getSeverity()));
    }
}
