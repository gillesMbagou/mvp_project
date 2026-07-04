# language: fr
Feature: Réception et traitement des observations IoT
  En tant que système de télésurveillance CareSync
  Je dois recevoir les mesures des dispositifs médicaux en temps réel
  Afin de détecter les anomalies cliniques et alerter l'équipe soignante

  Background:
    Étant donné que Mosquitto est opérationnel sur port 1883
    Et Kafka "caresync.iot.observations" est disponible
    Et le patient "patient-001" (Marie Dupont, Diabète T2) est suivi
    Et le dispositif "LIB3-P001-001" (FreeStyle Libre 3) est enregistré
    Et une infirmière est connectée au flux SSE

  Scenario: Mesure glycémie normale reçue et broadcastée SSE
    Quand le capteur publie sur "caresync/devices/LIB3-P001-001/glucose"
    Et la valeur est "1.2 g/L" avec LOINC "14743-9"
    Alors l'OBSERVATION est persistée avec severity "NORMAL"
    Et l'événement arrive sur le flux SSE en moins de 200ms
    Et aucune ALERTE n'est publiée sur "caresync.alerts"

  Scenario: Hypoglycémie critique — pipeline complet
    Quand le capteur publie une glycémie de "0.55 g/L"
    Alors la severity calculée est "CRITIQUE"
    Et une ALERTE est publiée sur "caresync.alerts" avec message "Hypoglycémie sévère : 0.55 g/L"
    Et le flux SSE affiche l'alerte rouge en moins de 500ms

  Scenario: Dispositif hors ligne détecté
    Étant donné que "LIB3-P001-001" n'a pas émis depuis 31 minutes
    Alors une ALERTE "DEVICE_OFFLINE" de sévérité "INFORMATIVE" est créée
    Et l'infirmière référente reçoit une notification de déconnexion

  Scenario Outline: Seuils cliniques par type de dispositif
    Quand le dispositif "<type>" mesure "<valeur>"
    Alors la sévérité est "<sévérité>"
    Exemples:
      | type            | valeur   | sévérité  |
      | GLUCOMETER      | 0.55 g/L | CRITIQUE  |
      | GLUCOMETER      | 0.75 g/L | URGENTE   |
      | GLUCOMETER      | 1.20 g/L | NORMAL    |
      | PULSE_OXIMETER  | 83%      | CRITIQUE  |
      | PULSE_OXIMETER  | 88%      | URGENTE   |
      | PULSE_OXIMETER  | 95%      | NORMAL    |
      | SCALE           | 85.0 kg  | URGENTE   |
