# language: fr
@etablissement @UC-01
Feature: Gestion des établissements
  En tant que SUPER_ADMIN ou ADMIN_ETABLISSEMENT
  Je veux gérer les établissements de santé et leurs professionnels
  Afin d'assurer une organisation multi-tenant conforme HDS

  Background:
    Given le super_admin "admin@caresync.be" est authentifié avec le rôle "super_admin"
    And le système CareSync est opérationnel

  @smoke
  Scenario: Créer un nouvel établissement avec schema isolé
    Given les données de l'établissement suivantes:
      | nom        | CHU Bruxelles        |
      | type       | HOPITAL              |
      | siret      | 12345678900012       |
      | certifie_hds | true               |
    When je soumets POST /api/v1/etablissements
    Then la réponse HTTP est 201 CREATED
    And un ETABLISSEMENT est créé avec un ID unique
    And un schema PostgreSQL isolé est provisionné
    And un namespace Kubernetes "chu-bruxelles" est créé
    And un AUDIT_LOG "CREATE_ETABLISSEMENT" est enregistré

  Scenario: Enregistrer un médecin dans un établissement
    Given l'établissement "CHU Bruxelles" (ID: ETB-001) existe
    And l'admin_etablissement de ETB-001 est authentifié
    When il soumet POST /api/v1/professionnels avec:
      | inami        | 1-23456-07-008  |
      | role         | medecin         |
      | specialite   | Cardiologie     |
      | service      | Cardiologie     |
      | email        | dr.dupont@chu.be|
    Then un PROFESSIONNEL est créé avec le statut INACTIF
    And un compte Keycloak est provisionné avec le rôle "medecin"
    And un email d'activation est envoyé à dr.dupont@chu.be

  Scenario: Affecter un professionnel à un service
    Given le professionnel PRO-042 (statut ACTIF) existe
    And le service "Pneumologie" (SVC-010) existe dans ETB-001
    When l'admin soumet PUT /api/v1/professionnels/PRO-042/service
      | serviceId | SVC-010 |
    Then PRO-042 est affecté au service Pneumologie
    And ses droits d'accès Keycloak sont mis à jour

  @securite
  Scenario: Accès refusé si rôle insuffisant
    Given un utilisateur avec le rôle "infirmier" est connecté
    When il tente POST /api/v1/etablissements
    Then la réponse HTTP est 403 FORBIDDEN
    And un AUDIT_LOG "ACCESS_DENIED" est enregistré

  Scenario: Désactiver un établissement
    Given l'établissement ETB-002 est ACTIF
    When le super_admin soumet DELETE /api/v1/etablissements/ETB-002
    Then ETB-002 passe au statut INACTIF
    And tous les comptes Keycloak associés sont suspendus
    And les données restent conservées (RGPD, HDS)

  Scenario: Lister les professionnels d'un établissement
    Given ETB-001 a 12 professionnels actifs
    When l'admin soumet GET /api/v1/etablissements/ETB-001/professionnels?page=0&size=10
    Then la réponse contient 10 professionnels
    And le total_elements = 12
    And chaque professionnel contient: id, inami, role, service
