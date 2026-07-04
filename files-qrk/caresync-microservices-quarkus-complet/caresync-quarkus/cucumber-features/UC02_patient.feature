# language: fr
@patient @UC-02
Feature: Gestion du dossier patient
  En tant que MEDECIN ou INFIRMIER
  Je veux créer et gérer le dossier médical complet d'un patient
  Afin de centraliser les informations cliniques

  Background:
    Given le Dr. Cappuyns (rôle: medecin, ETB-001) est authentifié
    And l'établissement "CHU Bruxelles" est opérationnel

  @smoke
  Scenario: Créer un dossier patient avec condition chronique
    When le médecin soumet POST /api/v1/patients avec:
      | nom          | Dupont          |
      | prenom       | Marie           |
      | date_naiss   | 1966-04-15      |
      | ins_nir      | 2-60-0258-B-002 |
      | genre        | F               |
      | pathologie   | DIABETE_T2      |
    Then un PATIENT est créé avec un ID unique (PAT-XXX)
    And une PATIENT_CONDITION "DIABETE_T2" est associée
    And le Dr. Cappuyns est automatiquement ajouté à l'EQUIPE_SOINS
    And un AUDIT_LOG "CREATE_PATIENT" est enregistré

  Scenario: Ajouter une comorbidité à un patient existant
    Given le patient Marie Dupont (PAT-001) existe
    When le médecin soumet POST /api/v1/patients/PAT-001/conditions avec:
      | pathologie   | HYPERTENSION    |
      | date_debut   | 2024-03-10      |
      | severite     | MODEREE         |
    Then une PATIENT_CONDITION "HYPERTENSION" est ajoutée
    And la liste des pathologies actives = [DIABETE_T2, HYPERTENSION]

  Scenario: Constituer l'équipe pluridisciplinaire
    Given le patient PAT-001 existe avec l'équipe [Dr. Cappuyns]
    When le médecin soumet PUT /api/v1/patients/PAT-001/equipe avec:
      | professionnelId | INF-001 |
      | role_equipe     | infirmier_referent |
    Then INF-001 est ajouté à l'EQUIPE_SOINS
    And INF-001 peut accéder au dossier PAT-001
    And un AUDIT_LOG "ADD_EQUIPE_MEMBER" est enregistré

  Scenario: Rechercher un patient par nom
    Given 50 patients existent dans ETB-001
    When le médecin soumet GET /api/v1/patients?search=Dupont
    Then la réponse contient tous les patients dont le nom contient "Dupont"
    And les patients hors ETB-001 ne sont pas retournés
    And un AUDIT_LOG "SEARCH_PATIENTS" est enregistré

  Scenario: Mettre à jour le niveau de risque
    Given le patient PAT-001 a un riskLevel "NORMAL"
    When l'alert-svc reçoit une alerte CRITIQUE pour PAT-001
    Then PAT-001.riskLevel passe à "CRITIQUE"
    And les infirmiers de l'EQUIPE_SOINS sont notifiés

  @securite
  Scenario: Un patient ne peut voir que son propre dossier
    Given le patient Marie Dupont est authentifié (rôle: patient)
    When il accède GET /api/v1/patients/PAT-002 (autre patient)
    Then la réponse HTTP est 403 FORBIDDEN
    And un AUDIT_LOG "ACCESS_DENIED" est enregistré
