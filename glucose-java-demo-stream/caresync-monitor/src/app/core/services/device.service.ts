import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { SimulatedDevice, DeviceScenario } from '../models/device.model';

@Injectable({ providedIn: 'root' })
export class DeviceService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/api`;

  getDevices(): Observable<SimulatedDevice[]> {
    return this.http.get<SimulatedDevice[]>(`${this.base}/devices`);
  }

  setScenario(serial: string, scenario: DeviceScenario): Observable<Record<string, string>> {
    return this.http.post<Record<string, string>>(
      `${this.base}/devices/${serial}/scenario/${scenario}`,
      null,
    );
  }
}
