// ── Pagination ────────────────────────────────────────────────────────────────
// Forme réellement renvoyée par les endpoints paginés du backend (PatientResource,
// EtablissementResource) : { content, totalElements, page, size }.
export interface PageResult<T> {
  content:       T[];
  totalElements: number;
  page:          number;
  size:          number;
}

// ── Acteurs ───────────────────────────────────────────────────────────────────
export type Severity = 'NORMAL' | 'INFORMATIVE' | 'URGENTE' | 'CRITIQUE';
export type UserRole  = 'medecin' | 'infirmier' | 'pharmacien' | 'admin_etablissement' | 'super_admin' | 'patient';

export interface CareUser {
  id:          string;
  username:    string;
  firstName:   string;
  lastName:    string;
  email:       string;
  roles:       UserRole[];
  tenantId?:   string;
  initials:    string;
  displayName: string;
}

// ── Patients ──────────────────────────────────────────────────────────────────
export type RiskLevel = 'normal' | 'informative' | 'urgente' | 'critique';
export type Pathology = 'DIABETE_T2' | 'ICFE' | 'BPCO' | 'HYPERTENSION' | 'OTHER';

export interface PatientDto {
  id:               string;
  firstName:        string;
  lastName:         string;
  age?:             number;
  gender?:          'M' | 'F';
  dateOfBirth?:     string;
  insNir?:          string;
  pathology?:       Pathology;
  service?:         string;
  riskLevel?:       RiskLevel;
  activeAlerts?:    number;
  connectedDevices?:number;
  assignedMedicId?: string;
}

export interface PatientConditionDto {
  id:        string;
  patientId: string;
  pathology: Pathology;
  severity:  string;
  dateDebut: string;
}

// ── Observations IoT ──────────────────────────────────────────────────────────
export interface ObservationDto {
  id?:          number;
  patientId:    string;
  deviceSerial: string;
  deviceType:   string;
  loincCode:    string;
  value:        number;
  unit:         string;
  severity:     Severity;
  source:       string;
  observedAt:   string;
  latencyMs?:   number;
}

export interface IoTDeviceDto {
  serial:           string;
  patientId:        string;
  deviceType:       string;
  manufacturer:     string;
  model:            string;
  loincCode:        string;
  unit:             string;
  active:           boolean;
  currentScenario?: string;
  lastObservation?: ObservationDto;
}

// ── Alertes ───────────────────────────────────────────────────────────────────
export interface AlertDto {
  alertId:          string;
  patientId:        string;
  patientFullName?: string;
  deviceSerial:     string;
  deviceType:       string;
  severity:         Exclude<Severity, 'NORMAL' | 'INFORMATIVE'>;
  message:          string;
  triggeredValue:   string;
  thresholdType?:   string;
  createdAt:        string;
  resolvedAt?:      string;
  statut:           'OUVERTE' | 'ACQUITTEE' | 'ESCALADEE' | 'RESOLUE';
}

// ── Plans de soins ────────────────────────────────────────────────────────────
export interface CarePlanDto {
  id:             string;
  patientId:      string;
  typePathologie: Pathology;
  objectif?:      string;
  statut:         'ACTIF' | 'SUSPENDU' | 'TERMINE';
  medicId?:       string;
  createdAt:      string;
}

export interface CarePlanTaskDto {
  id:           string;
  carePlanId:   string;
  typeTask:     string;
  statut:       'EN_ATTENTE' | 'EN_COURS' | 'REALISEE' | 'ANNULEE';
  echeanceAt?:  string;
  realiseeAt?:  string;
  valeur?:      string;
  commentaire?: string;
}

// ── Stats dashboard ────────────────────────────────────────────────────────────
export interface StatsDto {
  totalObservations: number;
  totalPatients:     number;
  totalDevices:      number;
  alertsLast24h:     number;
  criticalAlerts24h: number;
}

// ── Messages ──────────────────────────────────────────────────────────────────
export interface MessageDto {
  id:            string;
  expediteurId:  string;
  objet:         string;
  priorite:      'NORMALE' | 'URGENTE' | 'CRITIQUE';
  patientRef?:   string;
  accuseReception: boolean;
  creeAt:        string;
  destinataires: MessageDestDto[];
}

export interface MessageDestDto {
  destinataireId: string;
  statut:         'NON_LU' | 'LU';
  luLe?:          string;
}

// ── Établissements & professionnels ─────────────────────────────────────────────
export type EtablissementType =
  'CHU' | 'CHR' | 'CLINIQUE_PRIVEE' | 'CABINET' | 'EHPAD' | 'SSR' | 'MAISON_SANTE';
export type EtablissementStatut = 'ACTIF' | 'INACTIF' | 'SUSPENDU';

export interface EtablissementDto {
  id:             string;
  finess:         string;
  nom:            string;
  adresse?:       string;
  codePostal?:    string;
  ville?:         string;
  pays?:          string;
  telephone?:     string;
  emailContact?:  string;
  siteWeb?:       string;
  type:           EtablissementType;
  statut:         EtablissementStatut;
  createdAt:      string;
}

export type ProfessionnelRole =
  'MEDECIN' | 'INFIRMIER' | 'AIDE_SOIGNANT' | 'KINESITHERAPEUTE'
  | 'DIETETICIEN' | 'CHEF_SERVICE' | 'ADMIN' | 'SECRETAIRE';
export type ProfessionnelStatut = 'ACTIF' | 'INACTIF' | 'EN_CONGE' | 'SUSPENDU';

export interface ProfessionnelDto {
  id:              string;
  etablissementId: string;
  prenom:          string;
  nom:             string;
  email:           string;
  telephone?:      string;
  rpps?:           string;
  role:            ProfessionnelRole;
  service?:        string;
  specialite?:     string;
  statut:          ProfessionnelStatut;
  createdAt:       string;
}

// ── Prescriptions ─────────────────────────────────────────────────────────────
export type PrescriptionStatut = 'BROUILLON' | 'SIGNEE' | 'DELIVREE' | 'EXPIREE' | 'ANNULEE';

export interface MedicamentDto {
  id:                  string;
  codeCIS:             string;
  denominationCommune: string;
  nomCommercial?:      string;
  dosage?:             string;
  formeGalenique?:     string;
  voieAdministration?: string;
  interactions:        boolean;
}

export interface PrescriptionLineDto {
  id?:                   string;
  medicamentId:          string;
  medicamentNom?:        string;
  posologie:             string;
  dureeJours:            number;
  voieSpecifique?:       string;
  instructionsSpeciales?:string;
  alerteInteraction?:    boolean;
}

export interface PrescriptionDto {
  id:                    string;
  patientId:             string;
  prescripteurId:        string;
  statut:                PrescriptionStatut;
  dateEmission:          string;
  dateExpiration?:       string;
  renouvellementPossible:boolean;
  nombreRenouvellements?:number;
  lignes:                PrescriptionLineDto[];
}

// ── Dossier médical consolidé ────────────────────────────────────────────────
export interface DossierCompletDto {
  patient:       PatientDto;
  conditions:    PatientConditionDto[];
  observationsIot: ObservationDto[];
  prescriptions: PrescriptionDto[];
  carePlans:     CarePlanDto[];
  alertes:       AlertDto[];
}

export interface DossierSyntheseDto {
  patientId:       string;
  dernieresMesures:ObservationDto[];
  alertesActives:  AlertDto[];
  tachesDuJour:    CarePlanTaskDto[];
}

// ── Audit ─────────────────────────────────────────────────────────────────────
export interface AuditLogDto {
  eventId:      string;
  actorId:      string;
  actorRole:    string;
  action:       string;
  resourceType: string;
  resourceId?:  string;
  serviceName:  string;
  ipAddress?:   string;
  timestamp:    string;
  success:      boolean;
}

// ── Analytics complémentaires ────────────────────────────────────────────────
export interface KpiEtablissementDto {
  etablissementId:     string;
  periode:             string;
  patientsActifs:      number;
  alertesCritiques:    number;
  tauxAquittement:     number;
  latenceIotP95Ms:     number;
  dispositifsActifs:   number;
  prescriptionsEmises: number;
}

export interface DashboardOperationnelDto {
  patientId:       string;
  riskLevel:       RiskLevel;
  alertesOuvertes: number;
  derniereMesure?: string;
}
