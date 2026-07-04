# language: fr
@audit @hds @UC-10
Feature: Audit Trail HDS
  En tant que SUPER_ADMIN et responsable conformité
  Je veux que chaque action médicale soit tracée de manière immuable
  Afin de satisfaire aux obligations légales HDS (2 ans de rétention)

  Background:
    Given le système CareSync est opérationnel
    And le topic Kafka "caresync.audit" avec cleanup.policy=compact existe

  @smoke
  Scenario: Chaque action médicale génère un AUDIT_LOG automatique
    Given le Dr. Cappuyns consulte le dossier de PAT-001
    Then dans la seconde, un AUDIT_LOG est publié sur Kafka:
      | eventId       | [UUID unique]         |
      | actorId       | [Keycloak user ID]    |
      | actorRole     | medecin               |
      | action        | READ_PATIENT_RECORD   |
      | resourceType  | Patient               |
      | resourceId    | PAT-001               |
      | serviceName   | caresync-q-dossier    |
      | ipAddress     | [IP du client]        |
      | timestamp     | [maintenant ISO8601]  |
      | success       | true                  |
    And l'AUDIT_LOG est persisté en base (statut IMMUABLE)

  @immuabilite
  Scenario: Un AUDIT_LOG ne peut pas être modifié ou supprimé
    Given l'AUDIT_LOG AUD-001 existe
    When un admin tente de DELETE /api/v1/audit/AUD-001
    Then la réponse HTTP est 405 METHOD_NOT_ALLOWED
    And un AUDIT_LOG "TAMPER_ATTEMPT" est généré pour cette tentative

  Scenario: Export des logs pour audit de conformité HDS
    Given le super_admin déclenche un audit annuel
    When il soumet GET /api/v1/audit/export?from=2025-01-01&to=2026-01-01
    Then les AUDIT_LOG sont exportés en JSON signé (HMAC-SHA256)
    And l'export contient > 10 000 entrées
    And l'export lui-même est tracé dans l'AUDIT_LOG

  Scenario: Rétention 2 ans garantie par Kafka compact
    Given le topic "caresync.audit" a cleanup.policy=compact
    And retention.ms=-1 (infinie)
    When on vérifie la configuration du topic
    Then les messages ne sont JAMAIS supprimés automatiquement
    And la compaction préserve la dernière valeur par clé (eventId)
    And les anciens messages sont archivés vers S3 après 90 jours
