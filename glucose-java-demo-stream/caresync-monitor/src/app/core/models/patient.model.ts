export interface PatientProfile {
  patientId: string;
  firstName: string;
  lastName: string;
  birthDate: string;
  gender: 'M' | 'F' | 'AUTRE';
  insNir?: string;
  primaryDiagnosis?: string;
  treatingPhysician?: string;
}
