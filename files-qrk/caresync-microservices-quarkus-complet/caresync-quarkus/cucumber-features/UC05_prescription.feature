# language: fr
@prescription @UC-05
Feature: Prescription électronique
  En tant que MEDECIN
  Je veux créer des prescriptions électroniques signées
  Afin de garantir la traçabilité et la sécurité des ordonnances

  Background:
    Given le Dr. Cappuyns est prescripteur autorisé (INAMI valide)
    And le patient Marie Dupont (PAT-001) est dans son équipe de soins
    And le médicament METFORMINE (MED-001, 500mg) existe dans le formulaire

  @smoke
  Scenario: Créer une prescription de Metformine
    When le médecin soumet POST /api/v1/prescriptions avec:
      | patientId    | PAT-001         |
      | validite_j   | 90              |
    And ajoute une PRESCRIPTION_LINE:
      | medicamentId | MED-001         |
      | posologie    | 500mg 2x par jour|
      | duree_j      | 90              |
      | renouvellements | 2            |
    Then une PRESCRIPTION est créée avec statut SIGNEE
    And une PRESCRIPTION_LINE est créée
    And la prescription est transmise à la pharmacie désignée de PAT-001
    And un AUDIT_LOG "CREATE_PRESCRIPTION" est enregistré

  Scenario: Pharmacien valide et dispense une prescription
    Given la prescription P-2026-0042 est SIGNEE
    And le pharmacien PHR-001 est authentifié
    When il soumet PATCH /api/v1/prescriptions/P-2026-0042/valider
    Then la PRESCRIPTION passe au statut DISPENSEE
    And la date_dispensation = maintenant
    And le patient est notifié via messagerie sécurisée

  Scenario: Renouvellement d'une ordonnance
    Given la PRESCRIPTION P-2026-0001 a renouvellements_restants = 2
    When le patient ou pharmacien demande un renouvellement
    Then une nouvelle PRESCRIPTION est créée comme copie
    And renouvellements_restants décrémenté = 1
    And la nouvelle prescription est envoyée à la pharmacie

  Scenario: Vérification des interactions médicamenteuses
    Given PAT-001 a déjà METFORMINE et RAMIPRIL actifs
    When le médecin tente d'ajouter IBUPROFENE (interaction connue)
    Then une ALERTE "INTERACTION_MEDICAMENTEUSE" est affichée
    And le médecin peut confirmer ou annuler
    And si confirmé, un AUDIT_LOG "OVERRIDE_DRUG_INTERACTION" est créé

  Scenario: Accès refusé — prescription par un non-médecin
    Given une infirmière (rôle: infirmier) est connectée
    When elle tente POST /api/v1/prescriptions
    Then la réponse HTTP est 403 FORBIDDEN
