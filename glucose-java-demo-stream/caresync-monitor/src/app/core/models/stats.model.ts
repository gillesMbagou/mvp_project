export interface DashboardStats {
  totalObservations: number;
  totalPatients: number;
  totalDevices: number;
  alertsLast24h: number;
  criticalAlerts24h: number;
  timestamp?: string;
}
