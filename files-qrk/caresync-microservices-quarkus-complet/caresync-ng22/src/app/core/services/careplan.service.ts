import { Injectable, signal, computed } from '@angular/core';
import { httpResource, HttpClient } from '@angular/common/http';
import { inject } from '@angular/core';
import { environment } from '../../../environments/environment';
import { CarePlanDto, CarePlanTaskDto, Pathology } from '../models';

/**
 * CarePlanService — /api/v1/care-plans (caresync-q-careplan, port 8094).
 *
 * Endpoints réels côté backend (CarePlanResource) :
 *   GET   /{patientId}              → CarePlan[] actifs — rôles medecin, infirmier
 *   POST  /                         → crée un plan + tâches standard — rôle medecin
 *   PATCH /tasks/{taskId}/valider   → valide une tâche — rôles medecin, infirmier
 */
@Injectable({ providedIn: 'root' })
export class CarePlanService {
  private readonly http    = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/care-plans`;

  readonly selectedPatientId = signal<string | null>(null);

  readonly carePlansResource = httpResource<CarePlanDto[]>(
    () => this.selectedPatientId()
      ? { url: `${this.baseUrl}/${this.selectedPatientId()}` }
      : undefined
  );

  readonly carePlans   = computed(() => this.carePlansResource.hasValue() ? this.carePlansResource.value() : []);
  readonly isLoading   = this.carePlansResource.isLoading;
  readonly activePlans = computed(() => this.carePlans().filter(p => p.statut === 'ACTIF'));

  forPatient(patientId: string): void {
    this.selectedPatientId.set(patientId);
  }

  create(patientId: string, typePathologie: Pathology, objectif?: string) {
    return this.http.post<CarePlanDto>(this.baseUrl, { patientId, typePathologie, objectif });
  }

  validerTask(taskId: string, valeur?: string, commentaire?: string) {
    return this.http.patch<CarePlanTaskDto>(`${this.baseUrl}/tasks/${taskId}/valider`, { valeur, commentaire });
  }

  reload(): void {
    this.carePlansResource.reload();
  }
}
