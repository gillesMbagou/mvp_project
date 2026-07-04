# language: fr
@messagerie @UC-08
Feature: Messagerie sécurisée intra-établissement
  En tant que professionnel de santé
  Je veux envoyer des messages chiffrés à mes collègues et patients
  Afin de coordonner les soins de manière sécurisée et traçable

  Background:
    Given le Dr. Cappuyns (medecin, ETB-001) est authentifié
    And l'infirmière Inf.Lambert (INF-001, ETB-001) est disponible

  @smoke
  Scenario: Envoyer un message sécurisé à l'équipe de soins
    When le médecin soumet POST /api/v1/messages avec:
      | objet       | Résultats HbA1c Marie Dupont   |
      | corps       | HbA1c = 8.2% — adapter doses  |
      | destinataires | [INF-001, INF-002]          |
      | priorite    | URGENTE                        |
      | patientRef  | PAT-001                        |
    Then un MESSAGE_SECURISE est créé (chiffré AES-256)
    And 2 MESSAGE_DESTINATAIRE sont créés avec statut NON_LU
    And une notification push est envoyée à INF-001 et INF-002

  @chiffrement
  Scenario: Chiffrement de bout en bout obligatoire
    Given le Dr. Cappuyns envoie un message à INF-001
    Then le contenu est chiffré AES-256-GCM en base de données
    And seuls les destinataires peuvent déchiffrer avec leur clé
    And le chiffrement est vérifié par l'AUDIT_LOG

  Scenario: Accusé de réception pour message URGENTE
    Given le message MSG-009 (priorite=URGENTE) est NON_LU pour INF-001
    When INF-001 ouvre le message via GET /api/v1/messages/MSG-009
    Then le MESSAGE_DESTINATAIRE passe à LU
    And timestamp "lu_le" = maintenant
    And le Dr. Cappuyns est notifié de la lecture
    And un AUDIT_LOG "READ_MESSAGE" est enregistré

  Scenario: Message avec pièce jointe (ordonnance PDF)
    Given une prescription P-2026-0042 existe
    When le médecin crée un message avec attachment: prescription.pdf
    Then la pièce jointe est chiffrée et stockée de manière sécurisée
    And le destinataire peut télécharger le PDF via GET /api/v1/messages/MSG-010/attachments/0

  Scenario: Fil de messages autour d'un patient
    Given 5 messages existent avec patientRef=PAT-001
    When INF-001 accède à GET /api/v1/messages?patientId=PAT-001
    Then les 5 messages sont retournés (uniquement ceux où INF-001 est destinataire)
    And ils sont triés par date décroissante
