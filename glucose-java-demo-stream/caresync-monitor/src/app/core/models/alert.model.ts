export type AlertSeverity = 'CRITIQUE' | 'URGENTE' | 'INFORMATIVE';

export interface GlucoseObservation {
  id: number;
  patientId: string;
  deviceSerial: string;
  deviceType: string;
  loincCode: string;
  value: number;
  unit: string;
  observedAt: string;
  severity: AlertSeverity;
  message: string;
}
