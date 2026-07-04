# language: fr
@analytics @UC-09
Feature: Tableaux de bord analytics
  En tant que MEDECIN ou ADMIN_ETABLISSEMENT
  Je veux visualiser les données agrégées de santé
  Afin de prendre des décisions basées sur des données probantes

  Background:
    Given le Dr. Cappuyns suit Marie Dupont (PAT-001) depuis 90 jours
    And TimescaleDB contient les mesures IoT historiques

  @smoke
  Scenario: Tableau de bord glycémique 30 jours
    When le médecin accède à
      GET /api/v1/analytics/patients/PAT-001/glucose?days=30
    Then la réponse contient une série temporelle agrégée:
      | agrégation  | par heure (time_bucket)        |
      | moyenne     | 1.42 g/L                       |
      | minimum     | 0.55 g/L (épisode nocturne J3) |
      | maximum     | 2.85 g/L (post-prandiaux)      |
      | episodes    | 3 hypoglycémies CRITIQUE       |
    And la tendance d'amélioration HbA1c estimée est calculée

  Scenario: KPI opérationnel de l'établissement
    Given l'admin de ETB-001 est authentifié
    When il accède à
      GET /api/v1/analytics/etablissements/ETB-001/kpi?month=2026-06
    Then la réponse contient:
      | patients_actifs       | 847    |
      | alertes_critiques     | 23     |
      | taux_acquittement     | 98.3%  |
      | latence_iot_p95_ms    | 320    |
      | dispositifs_actifs    | 1253   |
      | prescriptions_emises  | 412    |

  Scenario: Rapport d'évolution patient (export PDF)
    Given le médecin veut un rapport mensuel de PAT-001
    When il accède à
      GET /api/v1/analytics/patients/PAT-001/rapport?format=pdf&month=2026-06
    Then la réponse est un PDF généré avec:
      | courbe_glycemie     | 30 jours           |
      | courbe_spo2         | si applicable      |
      | liste_alertes       | avec acquittements |
      | observance_soins    | tâches réalisées % |
    And le PDF est signé numériquement

  Scenario: Dashboard multi-patients (vue infirmière de garde)
    Given l'infirmière suit 15 patients actifs
    When elle accède à GET /api/v1/analytics/dashboard/operationnel
    Then elle voit pour chaque patient:
      | derniere_mesure | heure et valeur          |
      | statut_risque   | NORMAL/URGENTE/CRITIQUE  |
      | alertes_ouvertes| count                    |
    And les patients CRITIQUE sont affichés en premier
