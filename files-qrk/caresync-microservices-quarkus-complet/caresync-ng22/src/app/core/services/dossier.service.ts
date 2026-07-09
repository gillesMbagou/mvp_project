import { Injectable, signal, computed } from '@angular/core';
import { httpResource } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { DossierCompletDto, DossierSyntheseDto } from '../models';

/**
 * DossierService — /api/v1/dossiers (caresync-q-dossier, port 8096).
 *
 * ⚠️ caresync-q-dossier n'a aucune entité ni Resource REST codée à ce jour
 * (uniquement application.yml) — endpoints spécifiés dans UC03_dossier.feature :
 *
 *   GET /{patientId}          → dossier complet agrégé (patient, conditions,
 *                                observations IoT, prescriptions, care plans, alertes)
 *   GET /{patientId}/synthese → vue synthétique (infirmière de garde)
 */
@Injectable({ providedIn: 'root' })
export class DossierService {
  private readonly baseUrl = `${environment.apiUrl}/dossiers`;

  readonly selectedPatientId = signal<string | null>(null);

  readonly dossierResource = httpResource<DossierCompletDto>(
    () => this.selectedPatientId()
      ? { url: `${this.baseUrl}/${this.selectedPatientId()}` }
      : undefined
  );

  // .value() lève si la resource est en état 'error' — hasValue() garde-fou.
  readonly dossier   = computed(() => this.dossierResource.hasValue() ? this.dossierResource.value() : undefined);
  readonly isLoading = this.dossierResource.isLoading;
  readonly error     = this.dossierResource.error;

  readonly syntheseResource = httpResource<DossierSyntheseDto>(
    () => this.selectedPatientId()
      ? { url: `${this.baseUrl}/${this.selectedPatientId()}/synthese` }
      : undefined
  );

  readonly synthese = computed(() => this.syntheseResource.hasValue() ? this.syntheseResource.value() : undefined);

  open(patientId: string): void {
    this.selectedPatientId.set(patientId);
  }

  reload(): void {
    this.dossierResource.reload();
  }
}
