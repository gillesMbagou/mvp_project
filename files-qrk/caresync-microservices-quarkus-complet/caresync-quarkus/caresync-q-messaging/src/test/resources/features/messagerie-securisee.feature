# language: fr
Feature: Messagerie sécurisée intra-service
  En tant que professionnel de santé
  Je veux communiquer de façon sécurisée et auditée avec mon équipe
  Afin de coordonner la prise en charge des patients chroniques

  Background:
    Étant donné que "Dr. Cappuyns" est authentifié
    Et l'infirmière "Sophie Renard" fait partie de son équipe soins
    Et le système de chiffrement Vault est opérationnel

  Scenario: Envoi message lié à un patient
    Quand le médecin envoie un MESSAGE_SECURISE avec:
      | sujet     | Jean Martin — adaptation posologie Furosémide |
      | patientId | patient-002                                    |
      | priorite  | URGENTE                                        |
    Et désigne les destinataires ["sophie.renard@chu", "dr.bernard@chu"]
    Alors le message est chiffré via Vault (AES-256)
    Et 2 MESSAGE_DESTINATAIRE sont créés avec statut "ENVOYE"
    Et les destinataires reçoivent une notification push dans les 5 secondes
    Et le sujet (non chiffré) est "Jean Martin — adaptation posologie Furosémide"

  Scenario: Accusé de réception automatique
    Étant donné que "Sophie Renard" ouvre le message "msg-001"
    Alors son MESSAGE_DESTINATAIRE passe au statut "LU"
    Et l'horodatage de lecture est enregistré
    Et l'expéditeur "Dr. Cappuyns" voit "Lu par Sophie Renard le <heure>"

  Scenario: Message URGENTE avec délai d'acquittement
    Étant donné qu'un MESSAGE_SECURISE de priorité "URGENTE" est envoyé
    Et l'infirmière ne l'a pas acquitté après 10 minutes
    Alors elle reçoit un rappel par SMS
    Et le statut reste "LIVRE" (non "ACQUITTE") jusqu'à action explicite

  Scenario: Conformité RGPD — accès aux messages
    Étant donné qu'un médecin externe tente d'accéder aux messages de l'équipe
    Quand il appelle GET "/api/v1/messages"
    Alors il reçoit uniquement les messages dont il est expéditeur ou destinataire
    Et un AUDIT_LOG "MESSAGE_READ" est créé pour chaque lecture
