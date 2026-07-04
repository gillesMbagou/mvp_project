# language: fr
Feature: Prescription électronique sécurisée
  En tant que médecin prescripteur
  Je veux créer des ordonnances électroniques signées numériquement
  Afin de prescrire des traitements en toute sécurité et conformité légale

  Background:
    Étant donné que "Dr. Cappuyns" est authentifié avec rôle "MEDECIN" et numéro RPPS "10004356789"
    Et "Jean Martin" (ICFe, 72 ans) est son patient actif
    Et le référentiel médicaments (base CIS/Theriaque) est chargé

  Scenario: Création d'ordonnance nominale
    Quand le médecin crée une PRESCRIPTION pour "Jean Martin"
    Et ajoute la ligne:
      | medicament     | Furosémide 40mg Teva (C03CA01) |
      | posologie      | 1 comprimé le matin à jeun     |
      | dureeJours     | 30                             |
    Et signe électroniquement avec son certificat RPPS
    Alors la PRESCRIPTION a le statut "SIGNEE"
    Et la date d'expiration est à 30 jours
    Et une PRESCRIPTION_LINE est créée pour "Furosémide"
    Et la prescription est versée au dossier patient
    Et un AUDIT_LOG "PRESCRIPTION_CREATED" est enregistré

  Scenario: Détection d'interaction médicamenteuse critique
    Étant donné que "Jean Martin" prend déjà "Digoxine 0.25mg" (C01AA05)
    Quand le médecin tente d'ajouter "Amiodarone 200mg" (C01BD01) à l'ordonnance
    Alors un avertissement "INTERACTION_CRITIQUE" est affiché:
      "Amiodarone × Digoxine : risque de bradycardie et toxicité digitalique"
    Et la prescription ne peut pas être signée sans confirmation explicite
    Et si le médecin confirme, alerteInteraction est enregistrée dans PRESCRIPTION_LINE

  Scenario: Renouvellement d'ordonnance
    Étant donné qu'une PRESCRIPTION de "Metformine 850mg" pour "Jean Martin" expire dans 7 jours
    Alors le médecin reçoit une alerte de renouvellement dans sa liste de tâches
    Quand il clique "Renouveler"
    Alors une nouvelle PRESCRIPTION est créée en copiant les PRESCRIPTION_LINE
    Et le champ prescriptionParentId référence l'ordonnance d'origine

  Scenario Outline: Durée de validité légale par catégorie
    Étant donné qu'un médicament de classe "<classe>" est prescrit
    Alors la durée de validité légale est "<duree>"
    Exemples:
      | classe           | duree    |
      | STUPEFIANT       | 7 jours  |
      | PSYCHOTROPE      | 28 jours |
      | ANTIBIOTHERAPIE  | 7 jours  |
      | ORDONNANCE_STD   | 3 mois   |
