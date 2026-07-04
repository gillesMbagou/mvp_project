# language: fr
@alerte @UC-07
Feature: Alertes cliniques intelligentes
  En tant que SYSTEME et INFIRMIER/MEDECIN
  Je veux qu'une alerte soit déclenchée automatiquement dès le dépassement
  d'un seuil clinique afin d'intervenir immédiatement

  Background:
    Given le patient Marie Dupont (PAT-001) est sous télésurveillance
    And l'alert-svc consomme Kafka "caresync.iot.observations"

  @smoke
  Scenario: Hypoglycémie critique déclenche une alerte
    Given la glycémie de PAT-001 passe à 0.55 g/L (seuil critique < 0.60)
    When l'AlertProcessor évalue la mesure
    Then une ALERTE est créée:
      | severity      | CRITIQUE                        |
      | message       | Hypoglycémie critique : 0.55 g/L|
      | statut        | OUVERTE                         |
    And l'ALERTE est publiée sur Kafka "caresync.alerts"
    And une notification push est envoyée à l'équipe de soins
    And l'ALERTE est visible sur le dashboard SSE en < 500ms

  Scenario: Alerte urgente glycémie élevée
    Given la glycémie de PAT-001 passe à 2.7 g/L (seuil urgente > 2.50)
    When l'AlertProcessor évalue la mesure
    Then une ALERTE severity=URGENTE est créée
    And un message sécurisé est envoyé au médecin référent

  Scenario: Acquittement d'une alerte par l'infirmière
    Given l'ALERTE CRIT-0042 (severity=CRITIQUE) est OUVERTE
    And l'infirmière Inf.Lambert est de garde
    When elle soumet PATCH /api/v1/alertes/CRIT-0042/acquitter
      | commentaire | Patient vérifié, glycémie remontée à 0.9 |
    Then l'ALERTE passe au statut ACQUITTEE
    And l'acquittante et le timestamp sont enregistrés
    And le médecin est notifié de l'acquittement

  @escalade
  Scenario: Escalade automatique après 15 minutes sans acquittement
    Given l'ALERTE CRIT-0042 est OUVERTE depuis 15 minutes
    And aucun professionnel ne l'a acquittée
    When le scheduler d'escalade se déclenche
    Then l'ALERTE passe au statut ESCALADEE
    And le médecin de garde reçoit un SMS + appel téléphonique
    And une CARE_PLAN_TASK urgente "intervention_immediate" est créée
    And un AUDIT_LOG "ALERT_ESCALATED" est enregistré

  Scenario: Résoudre une alerte après intervention
    Given l'ALERTE CRIT-0042 est ACQUITTEE
    And la glycémie remonte à 1.1 g/L (valeur normale)
    When le médecin soumet PATCH /api/v1/alertes/CRIT-0042/resoudre
    Then l'ALERTE passe au statut RESOLUE
    And le riskLevel du patient revient à NORMAL
    And la chaîne complète (OUVERTE→ACQUITTEE→RESOLUE) est dans l'AUDIT_LOG

  Scenario: Historique des alertes des 24 dernières heures
    Given PAT-001 a eu 3 alertes dans les 24 dernières heures
    When le médecin accède à GET /api/v1/alertes?patientId=PAT-001&hours=24
    Then la réponse contient les 3 alertes triées par date décroissante
    And pour chaque alerte: severity, message, statut, dates

  @dlq
  Scenario: Message Kafka en erreur va en Dead Letter Queue
    Given une OBSERVATION avec un format JSON invalide arrive
    When l'AlertProcessor échoue à la désérialiser (3 tentatives)
    Then le message est publié sur "caresync.iot.observations.dlq"
    And une alerte OPS est envoyée à l'équipe technique
    And le consommateur continue à traiter les autres messages
