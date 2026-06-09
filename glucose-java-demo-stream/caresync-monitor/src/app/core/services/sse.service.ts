import { Injectable, inject, NgZone } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { KeycloakService } from '../auth/keycloak.service';
import { GlucoseObservation } from '../models/alert.model';
import { DashboardStats } from '../models/stats.model';

@Injectable({ providedIn: 'root' })
export class SseService {
  private keycloak = inject(KeycloakService);
  private zone = inject(NgZone);
  private base = `${environment.apiUrl}/api/health-professional/stream`;

  private connect<T>(path: string): Observable<T> {
    return new Observable<T>(observer => {
      const token = this.keycloak.getToken();
      const url = token ? `${this.base}${path}?token=${token}` : `${this.base}${path}`;
      const source = new EventSource(url);

      source.addEventListener('message', (e: MessageEvent) => {
        this.zone.run(() => {
          try {
            observer.next(JSON.parse(e.data));
          } catch {
            observer.next(e.data as T);
          }
        });
      });

      source.addEventListener('observation', (e: MessageEvent) => {
        this.zone.run(() => observer.next(JSON.parse(e.data)));
      });

      source.addEventListener('alert', (e: MessageEvent) => {
        this.zone.run(() => observer.next(JSON.parse(e.data)));
      });

      source.addEventListener('stats', (e: MessageEvent) => {
        this.zone.run(() => observer.next(JSON.parse(e.data)));
      });

      source.onerror = () => {
        this.zone.run(() => observer.error(new Error('SSE connection error')));
        source.close();
      };

      return () => source.close();
    });
  }

  observations(serial?: string, patientId?: string): Observable<GlucoseObservation> {
    const params = new URLSearchParams();
    if (serial) params.set('serial', serial);
    if (patientId) params.set('patientId', patientId);
    const query = params.toString() ? `?${params}` : '';
    return this.connect<GlucoseObservation>(`/observations${query}`);
  }

  alerts(): Observable<GlucoseObservation> {
    return this.connect<GlucoseObservation>('/alerts');
  }

  stats(): Observable<DashboardStats> {
    return this.connect<DashboardStats>('/stats');
  }
}
