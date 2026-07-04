package be.caresync.etablissement.resource;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.List;

/**
 * EtablissementResource — UC-01 Gestion des établissements.
 *
 * Entités : ETABLISSEMENT, PROFESSIONNEL
 * Acteurs : SUPER_ADMIN, ADMIN_ETABLISSEMENT
 *
 * Multi-tenant : chaque établissement a son propre schema PostgreSQL.
 * Le provisionnement du schema est déclenché à la création de l'établissement.
 */
@Path("/api/v1/etablissements")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EtablissementResource {

    @GET
    @RolesAllowed({"super_admin", "admin_etablissement"})
    @RunOnVirtualThread
    public List<Etablissement> listAll(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        return Etablissement.findAll()
                .page(page, size)
                .list();
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({"super_admin", "admin_etablissement"})
    @RunOnVirtualThread
    public Etablissement getById(@PathParam("id") String id) {
        Etablissement e = Etablissement.findById(id);
        if (e == null) throw new NotFoundException("Établissement introuvable : " + id);
        return e;
    }

    @POST
    @Transactional
    @RolesAllowed("super_admin")
    @RunOnVirtualThread
    public Response create(@Valid Etablissement body) {
        body.setStatut(Etablissement.Statut.ACTIF);
        body.persist();
        // TODO: provisionner schema PostgreSQL + namespace K8s
        return Response.status(201).entity(body).build();
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    @RolesAllowed("super_admin")
    @RunOnVirtualThread
    public Response deactivate(@PathParam("id") String id) {
        Etablissement e = Etablissement.findById(id);
        if (e == null) throw new NotFoundException(id);
        e.setStatut(Etablissement.Statut.INACTIF);
        e.persist();
        return Response.noContent().build();
    }

    @GET
    @Path("/{id}/professionnels")
    @RolesAllowed({"super_admin", "admin_etablissement"})
    @RunOnVirtualThread
    public List<Professionnel> getProfessionnels(
            @PathParam("id") String etablissementId,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("10") int size) {
        return Professionnel.find("etablissementId = ?1 AND statut = ?2",
                        etablissementId, Professionnel.Statut.ACTIF)
                .page(page, size)
                .list();
    }

    @POST
    @Path("/{id}/professionnels")
    @Transactional
    @RolesAllowed({"super_admin", "admin_etablissement"})
    @RunOnVirtualThread
    public Response addProfessionnel(
            @PathParam("id") String etablissementId,
            @Valid Professionnel body) {
        body.setEtablissementId(etablissementId);
        body.setStatut(Professionnel.Statut.INACTIF); // En attente d'activation
        body.persist();
        // TODO: créer compte Keycloak + envoyer email activation
        return Response.status(201).entity(body).build();
    }

    // ── Entités inline (en prod : modules séparés) ───────────────────────────

    @Entity @Table(name = "etablissements")
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Etablissement extends PanacheEntityBase {
        @Id @GeneratedValue(strategy = GenerationType.UUID) private String id;
        @Column(nullable = false) private String nom;
        @Enumerated(EnumType.STRING) private Type type;
        private String siret;
        private boolean certifieHds;
        @Enumerated(EnumType.STRING) private Statut statut = Statut.ACTIF;
        @CreationTimestamp private Instant createdAt;
        public enum Type   { HOPITAL, CLINIQUE, CABINET, EHPAD }
        public enum Statut { ACTIF, INACTIF, SUSPENDU }
    }

    @Entity @Table(name = "professionnels")
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Professionnel extends PanacheEntityBase {
        @Id @GeneratedValue(strategy = GenerationType.UUID) private String id;
        private String etablissementId;
        @Column(nullable = false) private String inami;
        @Column(nullable = false) private String email;
        private String nom;
        private String prenom;
        private String specialite;
        private String service;
        @Enumerated(EnumType.STRING) private Role role;
        @Enumerated(EnumType.STRING) private Statut statut = Statut.INACTIF;
        @CreationTimestamp private Instant createdAt;
        public enum Role   { medecin, infirmier, pharmacien, admin_etablissement }
        public enum Statut { ACTIF, INACTIF, SUSPENDU }
    }
}
