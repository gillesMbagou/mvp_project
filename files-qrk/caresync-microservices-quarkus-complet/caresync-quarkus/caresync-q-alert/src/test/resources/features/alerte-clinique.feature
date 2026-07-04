# language: fr
Feature: Moteur d'alertes cliniques et escalade
  En tant que moteur d'alertes CareSync
  Je dois détecter les dépassements de seuils et escalader si non acquitté

  Background:
    Étant donné que Kafka est opérationnel
    Et l'infirmière "Sophie Renard" est de garde
    Et le médecin "Dr. Cappuyns" est le référent de "Jean Martin"

  Scenario: Déclenchement alerte critique (T+0)
    Quand un IoTObservationEvent "CRITIQUE" est reçu (value: 0.55, patientId: "patient-001")
    Alors une ALERTE est créée avec severity "CRITIQUE" et message "Hypoglycémie sévère : 0.55 g/L"
    Et l'ALERTE est publiée sur "caresync.alerts" avec acks=all
    Et l'infirmière reçoit une notification push en moins de 5 secondes
    Et AUDIT_LOG "ALERT_CREATED" est enregistré avec sourceOffset Kafka

  Scenario: Escalade non acquittée à T+15 minutes
    Étant donné qu'une ALERTE CRITIQUE n'est pas acquittée depuis 15 minutes
    Alors le médecin "Dr. Cappuyns" reçoit un SMS d'escalade
    Et un AUDIT_LOG "ALERT_ESCALATED" est créé
    Et le statut de l'ALERTE est "ESCALADEE"

  Scenario: Acquittement de l'alerte
    Quand l'infirmière acquitte l'alerte avec le commentaire "Patient pris en charge"
    Alors l'ALERTE passe au statut "RESOLUE"
    Et AUDIT_LOG "ALERT_ACKNOWLEDGED" est créé avec actorId et timestamp

  Scenario: Prise de poids anormale ICFe (+2kg/48h)
    Quand la balance publie "80.5 kg" pour "Jean Martin" (baseline: 78.0 kg)
    Alors une ALERTE "URGENTE" est créée: "Rétention hydrique suspectée : +2.5 kg/48h"
    Et le médecin est notifié pour révision du traitement diurétique
