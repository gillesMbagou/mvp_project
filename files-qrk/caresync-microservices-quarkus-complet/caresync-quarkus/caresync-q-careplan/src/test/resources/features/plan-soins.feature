# language: fr
Feature: Création et suivi du plan de soins
  En tant que médecin traitant
  Je veux créer et piloter un plan de soins structuré
  Afin de coordonner les actions thérapeutiques de l'équipe

  Background:
    Étant donné que "Dr. Cappuyns" est authentifié (rôle "MEDECIN")
    Et "Jean Martin" (ICFe) est son patient actif

  Scenario: Création plan de soins et activation
    Quand le médecin crée un CARE_PLAN "Suivi ICFe Intensif" pour "Jean Martin"
    Et ajoute la tâche "Pesée quotidienne" (fréquence: "DAILY", dispositif: "SCALE")
    Et ajoute la tâche "SpO2 matin et soir" (fréquence: "TWICE_DAILY")
    Et active le plan
    Alors le CARE_PLAN a le statut "ACTIF"
    Et un événement Kafka "care-plan-activated" est publié
    Et l'infirmière référente reçoit une notification

  Scenario: Exécution tâche par infirmière
    Étant donné qu'une CARE_PLAN_TASK "Pesée quotidienne" est planifiée pour aujourd'hui
    Quand l'infirmière "Sophie Renard" la marque comme exécutée avec valeur "78.5 kg"
    Alors la CARE_PLAN_TASK passe au statut "DONE"
    Et une OBSERVATION est créée automatiquement avec device_type "SCALE"
    Et si prise de poids > 2kg/48h, une ALERTE est déclenchée

  Scenario: Expiration automatique du plan
    Étant donné que la date de fin du CARE_PLAN est dépassée
    Alors le @Scheduler Quarkus fait passer le statut à "TERMINE"
    Et le médecin reçoit un rappel de renouvellement dans sa boîte
