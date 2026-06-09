import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ObservationPage, ObservationRecord } from '../models/observation.model';

@Injectable({ providedIn: 'root' })
export class ObservationService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/api`;

  getObservations(patientId: string, page = 0, size = 50): Observable<ObservationPage> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<ObservationPage>(`${this.base}/patients/${patientId}/observations`, { params });
  }

  getTimeSeries(patientId: string, deviceType: string, hours = 24): Observable<ObservationRecord[]> {
    const params = new HttpParams().set('deviceType', deviceType).set('hours', hours);
    return this.http.get<ObservationRecord[]>(
      `${this.base}/patients/${patientId}/observations/timeseries`,
      { params },
    );
  }

  getAlerts(hours = 24): Observable<ObservationRecord[]> {
    const params = new HttpParams().set('hours', hours);
    return this.http.get<ObservationRecord[]>(`${this.base}/observations/alerts`, { params });
  }
}
