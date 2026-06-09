export type DeviceScenario = 'NORMAL' | 'HYPOGLYCEMIA' | 'HYPERGLYCEMIA' | 'CRITICAL';

export interface SimulatedDevice {
  serial: string;
  patientId: string;
  deviceType: string;
  brand: string;
  model: string;
  protocol: string;
  active: boolean;
  currentScenario: DeviceScenario;
  lastObservationAt?: string;
}
