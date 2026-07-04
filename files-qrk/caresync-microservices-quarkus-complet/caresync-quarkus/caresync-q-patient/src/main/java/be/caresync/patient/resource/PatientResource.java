package be.caresync.patient.resource;

import be.caresync.patient.entity.Patient;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

import java.util.List;

/**
 * PatientResource — RESTEasy Reactive + Quarkus OIDC.
 *
 * Comparaison avec Spring Boot :
 * ─────────────────────────────────────────────────────────────────
 * Spring Boot                    | Quarkus
 * @RestController                | @Path + @ApplicationScoped
 * @PreAuthorize("hasRole(...)")  | @RolesAllowed(...)
 * Mono<Page<T>>                  | List<T> (Panache gère la pagination)
 * .subscribeOn(boundedElastic()) | @RunOnVirtualThread (Java 21 VT!)
 * @Transactional                 | @Transactional (identique, jakarta)
 * ─────────────────────────────────────────────────────────────────
 *
 * @RunOnVirtualThread = Java 21 Virtual Threads.
 * Plus besoin de Reactor ou Mutiny pour les appels DB !
 * Quarkus Loom support = style impératif, performance réactive.
 */
@Path("/api/v1/patients")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PatientResource {

    // SmallRye Reactive Messaging pour publier sur Kafka
    @Channel("patient-events-out")
    Emitter<String> patientEvents;

    @GET
    @RolesAllowed({"infirmier", "medecin", "admin"})
    @RunOnVirtualThread  // Java 21 VT — appel JPA non-bloquant perçu
    public Response getAll(
            @QueryParam("page")   @DefaultValue("0")  int page,
            @QueryParam("size")   @DefaultValue("20") int size,
            @QueryParam("search") String search) {

        PanacheQuery<Patient> query = search != null && !search.isBlank()
            ? Patient.find("active = true AND (LOWER(firstName) LIKE LOWER(?1) " +
                           "OR LOWER(lastName) LIKE LOWER(?2))",
                           "%" + search + "%", "%" + search + "%")
            : Patient.find("active", true);

        List<Patient> patients = query.page(Page.of(page, size)).list();
        long total = query.count();

        return Response.ok(new PageResult<>(patients, total, page, size)).build();
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({"infirmier", "medecin", "admin"})
    @RunOnVirtualThread
    public Patient getById(@PathParam("id") String id) {
        Patient p = Patient.findById(id);
        if (p == null || !p.isActive()) throw new NotFoundException("Patient introuvable : " + id);
        return p;
    }

    @POST
    @Transactional
    @RolesAllowed({"medecin", "admin"})
    @RunOnVirtualThread
    public Response create(@Valid Patient patient) {
        patient.setActive(true);
        patient.persist();

        // Publier événement Kafka (async, non-bloquant)
        patientEvents.send("{\"action\":\"CREATE\",\"patientId\":\"" + patient.getId() + "\"}");

        return Response.status(201).entity(patient).build();
    }

    @PATCH
    @Path("/{id}/risk")
    @Transactional
    @RolesAllowed({"infirmier", "medecin", "admin"})
    @RunOnVirtualThread
    public Patient updateRisk(
            @PathParam("id") String id,
            @QueryParam("level") String level) {
        Patient p = Patient.findById(id);
        if (p == null) throw new NotFoundException(id);
        p.setRiskLevel(Patient.RiskLevel.valueOf(level.toUpperCase()));
        p.persist();
        return p;
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    @RolesAllowed({"admin"})
    @RunOnVirtualThread
    public Response deactivate(@PathParam("id") String id) {
        Patient p = Patient.findById(id);
        if (p == null) throw new NotFoundException(id);
        p.setActive(false);
        p.persist();
        return Response.noContent().build();
    }

    // ── DTO pagination ────────────────────────────────────────────────
    public record PageResult<T>(
        List<T> content,
        long    totalElements,
        int     page,
        int     size
    ) {}
}
