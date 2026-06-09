export type ObservationSeverity = 'CRITIQUE' | 'URGENTE' | 'INFORMATIVE' | 'NORMAL';

export interface ObservationRecord {
  id: number;
  patientId: string;
  deviceSerial: string;
  deviceType: string;
  loincCode: string;
  value: number;
  unit: string;
  observedAt: string;
  severity: ObservationSeverity;
  message?: string;
}

export interface ObservationPage {
  content: ObservationRecord[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
