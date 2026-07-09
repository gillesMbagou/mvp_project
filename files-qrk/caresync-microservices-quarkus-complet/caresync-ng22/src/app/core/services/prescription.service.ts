import { Injectable, signal, computed, inject } from '@angular/core';
import { httpResource, HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { PrescriptionDto, PrescriptionLineDto } from '../models';

/**
 * PrescriptionService — /api/v1/prescriptions (caresync-q-prescription, port 8095).
 *
 * ⚠️ Aucune Resource REST n'existe encore côté backend pour ce module (uniquement
 * des entités JPA) — les endpoints ci-dessous sont ceux spécifiés dans
 * prescription-electronique.feature / UC05_prescription.feature. Le frontend est
 * bâti contre ce contrat prévu ; il affichera un état vide/erreur tant que
 * caresync-q-prescription n'expose pas de PrescriptionResource.
 *
 *   GET   /?patientId=...            → PrescriptionDto[]
 *   POST  /                          → crée (statut SIGNEE) — rôle medecin uniquement
 *   PATCH /{id}/valider              → dispensation (statut DELIVREE) — rôle pharmacien
 */
@Injectable({ providedIn: 'root' })
export class PrescriptionService {
  private readonly http    = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/prescriptions`;

  readonly selectedPatientId = signal<string | null>(null);

  readonly prescriptionsResource = httpResource<PrescriptionDto[]>(
    () => this.selectedPatientId()
      ? { url: this.baseUrl, params: { patientId: this.selectedPatientId()! } }
      : undefined
  );

  readonly prescriptions = computed(() =>
    this.prescriptionsResource.hasValue() ? this.prescriptionsResource.value() : []
  );
  readonly isLoading     = this.prescriptionsResource.isLoading;
  readonly actives       = computed(() => this.prescriptions().filter(p => p.statut === 'SIGNEE'));

  forPatient(patientId: string): void {
    this.selectedPatientId.set(patientId);
  }

  create(patientId: string, validiteJours: number, lignes: PrescriptionLineDto[]) {
    return this.http.post<PrescriptionDto>(this.baseUrl, { patientId, validiteJours, lignes });
  }

  valider(id: string) {
    return this.http.patch<PrescriptionDto>(`${this.baseUrl}/${id}/valider`, {});
  }

  reload(): void {
    this.prescriptionsResource.reload();
  }
}
