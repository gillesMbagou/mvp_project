import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PatientProfile } from '../models/patient.model';

@Injectable({ providedIn: 'root' })
export class PatientService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/api`;

  getPatients(): Observable<PatientProfile[]> {
    return this.http.get<PatientProfile[]>(`${this.base}/patients`);
  }
}
