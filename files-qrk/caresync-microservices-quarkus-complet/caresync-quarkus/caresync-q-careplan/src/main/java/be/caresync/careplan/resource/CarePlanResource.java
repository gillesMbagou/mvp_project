package be.caresync.careplan.resource;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.scheduler.Scheduled;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.persistence.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import lombok.*;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;
import java.util.List;

/**
 * CarePlanResource — UC-04 Plans de soins.
 *
 * Entités : CARE_PLAN, CARE_PLAN_TASK
 *
 * Quarkus @Scheduled remplace Spring @Scheduled pour le check d'escalade.
 * Quarkus SmallRye Emitter remplace KafkaTemplate pour notifier alert-svc.
 */
@Path("/api/v1/care-plans")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CarePlanResource {

    @Channel("care-plan-events")
    Emitter<String> events;

    @GET
    @Path("/{patientId}")
    @RolesAllowed({"medecin", "infirmier"})
    @RunOnVirtualThread
    public List<CarePlan> getByPatient(@PathParam("patientId") String patientId) {
        return CarePlan.find("patientId = ?1 AND statut = ?2",
                patientId, CarePlan.Statut.ACTIF).list();
    }

    @POST
    @Transactional
    @RolesAllowed("medecin")
    @RunOnVirtualThread
    public Response create(@Valid CarePlan body) {
        body.setStatut(CarePlan.Statut.ACTIF);
        body.persist();

        // Générer les tâches standard selon la pathologie
        generateStandardTasks(body);

        events.send("{\"action\":\"CAREPLAN_CREATED\",\"planId\":\"" + body.getId() + "\"}");
        return Response.status(201).entity(body).build();
    }

    @PATCH
    @Path("/tasks/{taskId}/valider")
    @Transactional
    @RolesAllowed({"medecin", "infirmier"})
    @RunOnVirtualThread
    public CarePlanTask validateTask(
            @PathParam("taskId") String taskId,
            TaskValidation validation) {
        CarePlanTask task = CarePlanTask.findById(taskId);
        if (task == null) throw new NotFoundException(taskId);

        task.setStatut(CarePlanTask.Statut.REALISEE);
        task.setRealiseeAt(Instant.now());
        task.setValeur(validation.valeur());
        task.setCommentaire(validation.commentaire());
        task.persist();

        // Notifier si valeur hors seuil → alert-svc via Kafka
        if (validation.valeur() != null) {
            events.send("{\"action\":\"TASK_COMPLETED\",\"taskId\":\"" + taskId + "\",\"valeur\":\"" + validation.valeur() + "\"}");
        }

        return task;
    }

    /** Vérification escalade tâches en retard — toutes les heures */
    @Scheduled(every = "1h")
    @RunOnVirtualThread
    void checkOverdueTasks() {
        List<CarePlanTask> overdue = CarePlanTask.find(
                "statut = ?1 AND echeanceAt < ?2",
                CarePlanTask.Statut.EN_ATTENTE, Instant.now()
        ).list();

        for (CarePlanTask task : overdue) {
            events.send("{\"action\":\"TASK_OVERDUE\",\"taskId\":\"" + task.getId() + "\"}");
        }
    }

    private void generateStandardTasks(CarePlan plan) {
        // Tâches standard par pathologie
        List<String> tasks = switch (plan.getTypePathologie()) {
            case "DIABETE_T2" -> List.of("mesure_glycemie_quotidienne", "consultation_mensuelle", "HbA1c_trimestriel");
            case "ICFE"       -> List.of("peser_quotidien", "spo2_quotidien", "consultation_bimensuelle");
            case "BPCO"       -> List.of("spirometrie_hebdomadaire", "spo2_quotidien", "consultation_mensuelle");
            default           -> List.of("consultation_mensuelle");
        };

        for (String taskType : tasks) {
            new CarePlanTask(plan.getId(), taskType).persist();
        }
    }

    // ── Entités ────────────────────────────────────────────────────────────────

    @Entity @Table(name = "care_plans")
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class CarePlan extends PanacheEntityBase {
        @Id @GeneratedValue(strategy = GenerationType.UUID) private String id;
        @Column(nullable = false) private String patientId;
        @Column(nullable = false) private String typePathologie;
        private String objectif;
        @Enumerated(EnumType.STRING) private Statut statut = Statut.ACTIF;
        private String medicId;
        @CreationTimestamp private Instant createdAt;
        public enum Statut { ACTIF, SUSPENDU, TERMINE }
    }

    @Entity @Table(name = "care_plan_tasks")
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class CarePlanTask extends PanacheEntityBase {
        @Id @GeneratedValue(strategy = GenerationType.UUID) private String id;
        private String carePlanId;
        private String typeTask;
        @Enumerated(EnumType.STRING) private Statut statut = Statut.EN_ATTENTE;
        private Instant echeanceAt;
        private Instant realiseeAt;
        private String valeur;
        private String commentaire;
        @CreationTimestamp private Instant createdAt;

        public CarePlanTask(String carePlanId, String typeTask) {
            this.carePlanId = carePlanId;
            this.typeTask   = typeTask;
        }

        public enum Statut { EN_ATTENTE, EN_COURS, REALISEE, ANNULEE }
    }

    public record TaskValidation(String valeur, String commentaire) {}
}
