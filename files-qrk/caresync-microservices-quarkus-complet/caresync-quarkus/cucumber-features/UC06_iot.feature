# language: fr
@iot @telésurveillance @UC-06
Feature: Télésurveillance IoT temps réel
  En tant que SYSTEME_IOT et INFIRMIER
  Je veux collecter et surveiller les mesures médicales en temps réel
  Afin de détecter immédiatement toute anomalie clinique

  Background:
    Given le patient Marie Dupont (PAT-001, DIABETE_T2) est suivi
    And le dispositif FreeStyle Libre 3 (LIB3-P001-001) est enregistré
    And Mosquitto MQTT est opérationnel sur port 1883
    And Kafka broker "caresync-kafka-broker-1:9092" est opérationnel

  @smoke
  Scenario: Enregistrement d'un dispositif IoT GLUCOMETER
    Given le médecin est authentifié
    When il soumet POST /api/v1/iot-devices avec:
      | serial       | LIB3-P001-001  |
      | type         | GLUCOMETER     |
      | loincCode    | 14743-9        |
      | patientId    | PAT-001        |
      | manufacturer | Abbott         |
      | model        | FreeStyle Libre 3 |
    Then un IOT_DEVICE est créé avec statut ACTIF
    And le topic MQTT "caresync/devices/LIB3-P001-001/+" est souscrit
    And un AUDIT_LOG "REGISTER_IOT_DEVICE" est enregistré

  @temps-reel
  Scenario: Réception d'une mesure glycémique normale
    Given le dispositif LIB3-P001-001 est actif
    When il publie sur MQTT "caresync/devices/LIB3-P001-001/glucose":
      | loincCode | 14743-9     |
      | value     | 1.2         |
      | unit      | g/L         |
      | ts        | [now_ms]    |
    Then une OBSERVATION est créée avec severity=NORMAL
    And l'OBSERVATION est publiée sur Kafka "caresync.iot.observations"
    Et les clients SSE connectés reçoivent l'événement en < 500ms

  @critique
  Scenario: Détection d'une hypoglycémie sévère (CRITIQUE)
    Given le dispositif LIB3-P001-001 est actif
    When il publie une mesure: { value: 0.55, unit: "g/L" }
    Then l'OBSERVATION severity = CRITIQUE
    And une ALERTE est créée sur Kafka "caresync.alerts"
    And l'infirmière de garde reçoit une notification push < 1s
    And le médecin reçoit un SMS d'alerte
    And le CARE_PLAN génère une tâche d'intervention urgente

  Scenario: Surveillance SpO2 pour patient ICFe
    Given le patient Jean Martin (PAT-002, ICFE) est suivi
    And son oxymètre Nonin GO2 (SPO2-P002-001) est actif
    When il publie: { loincCode: "59408-5", value: 85, unit: "%" }
    Then l'OBSERVATION severity = CRITIQUE (seuil < 85%)
    And une ALERTE CRITIQUE "Désaturation SpO2" est déclenchée

  @flux-sse
  Scenario: Dashboard temps réel via SSE
    Given l'infirmière est connectée sur le dashboard Angular
    And une connexion SSE est ouverte vers GET /api/stream/observations
    When le dispositif LIB3-P001-001 publie une mesure
    Then l'infirmière reçoit l'événement SSE "observation" en < 500ms
    And l'événement contient: serial, value, unit, severity, patientId

  Scenario: Gestion de la perte de connexion IoT
    Given le dispositif LIB3-P001-001 était actif
    When aucune mesure n'est reçue depuis 15 minutes
    Then une ALERTE "DEVICE_SILENT" est générée
    And le statut IoT passe à SUSPECT
    And l'infirmière est notifiée pour vérification physique

  @pipeline-kafka
  Scenario: Pipeline complet MQTT → Kafka → SSE (bout en bout)
    Given le simulateur FreeStyle Libre 3 est actif (scenario: hypoglycemie)
    When la glycémie descend à 0.58 g/L
    Then dans les 2 secondes:
      | Étape               | Résultat attendu                    |
      | 1. MQTT reçoit      | payload JSON sur le topic MQTT      |
      | 2. IoT Processor    | @Incoming enrichit + severity       |
      | 3. Kafka broker     | offset incrémenté dans iot.obs      |
      | 4. Alert Engine     | @Incoming déclenche alerte CRITIQUE |
      | 5. SSE Angular      | Multi<T> pousse l'événement         |
    And la latence totale (MQTT → SSE) est < 1000ms

  Scenario: Désactiver un dispositif
    Given le dispositif LIB3-P001-001 est ACTIF
    When le médecin soumet PATCH /api/v1/iot-devices/LIB3-P001-001
      | statut | INACTIF |
    Then le topic MQTT associé est désabonné
    Et les nouvelles mesures sont ignorées
    Et un AUDIT_LOG "DEACTIVATE_IOT_DEVICE" est enregistré

  Scenario: Reconnecter un dispositif après panne réseau
    Given la connexion MQTT était interrompue
    When Mosquitto se reconnecte (auto-reconnect)
    Then l'adaptateur MQTT reprend les souscriptions
    And les mesures en attente (QoS=1) sont reçues
    And aucune mesure n'est perdue
