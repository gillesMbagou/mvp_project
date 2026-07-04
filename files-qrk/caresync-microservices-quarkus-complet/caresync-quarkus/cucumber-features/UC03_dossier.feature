# language: fr
@dossier @UC-03
Feature: Consultation du dossier patient
  En tant que professionnel de santé de l'équipe de soins
  Je veux consulter le dossier complet d'un patient
  Afin de prendre des décisions cliniques éclairées

  Background:
    Given le patient Marie Dupont (PAT-001) existe avec:
      | pathologie   | DIABETE_T2       |
      | equipe_soins | [Dr.Cappuyns, Inf.Lambert] |

  @smoke
  Scenario: Médecin de l'équipe consulte le dossier complet
    Given le Dr. Cappuyns est dans l'EQUIPE_SOINS de PAT-001
    When il accède à GET /api/v1/dossiers/PAT-001
    Then la réponse HTTP est 200 OK
    And le dossier contient:
      | patient          | informations complètes        |
      | conditions       | [DIABETE_T2]                 |
      | observations_iot | 30 dernières mesures          |
      | prescriptions    | prescriptions actives         |
      | care_plans       | plans de soins actifs         |
      | alertes          | alertes des 24h               |
    And un AUDIT_LOG "READ_PATIENT_RECORD" est créé
      avec actorId, resourceId, ip, timestamp

  Scenario: Accès refusé — médecin hors équipe de soins
    Given le Dr. Martin (MED-099) n'est PAS dans l'EQUIPE_SOINS de PAT-001
    When il tente GET /api/v1/dossiers/PAT-001
    Then la réponse HTTP est 403 FORBIDDEN
    And le message est "Accès non autorisé à ce dossier"
    And un AUDIT_LOG "ACCESS_DENIED" est enregistré

  Scenario: Consulter l'historique des observations sur 7 jours
    Given PAT-001 a 2016 observations sur 7 jours
    When le médecin accède à
      GET /api/v1/dossiers/PAT-001/observations?deviceType=GLUCOMETER&days=7
    Then la réponse contient une série temporelle paginée
    And les valeurs sont agrégées par heure (time_bucket)
    And les min/max/avg sont calculés

  @audit
  Scenario: Chaque consultation est tracée en temps réel
    Given le Dr. Cappuyns consulte le dossier PAT-001
    When la consultation est effectuée
    Then dans la seconde, un AUDIT_LOG est publié sur Kafka "caresync.audit"
    And l'AUDIT_LOG contient:
      | action        | READ_PATIENT_RECORD |
      | actorId       | [id du médecin]     |
      | resourceType  | Patient             |
      | resourceId    | PAT-001             |
      | success       | true                |

  Scenario: Vue synthétique pour l'infirmière de garde
    Given l'infirmière Inf.Lambert est de garde
    When elle accède à GET /api/v1/dossiers/PAT-001/synthese
    Then elle voit: dernières mesures, alertes actives, tâches du jour
    And l'affichage est optimisé mobile (champs essentiels uniquement)
