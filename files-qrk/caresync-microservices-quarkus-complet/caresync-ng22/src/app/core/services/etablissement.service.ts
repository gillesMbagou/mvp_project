import { Injectable, signal, computed, inject } from '@angular/core';
import { httpResource, HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { EtablissementDto, ProfessionnelDto, PageResult } from '../models';

/**
 * EtablissementService — /api/v1/etablissements (caresync-q-etablissement, port 8091).
 *
 * Endpoints réels (EtablissementResource) :
 *   GET    /                          → PageResult<Etablissement>  — super_admin, admin_etablissement
 *   GET    /{id}                      → Etablissement              — super_admin, admin_etablissement
 *   POST   /                          → crée                       — super_admin
 *   DELETE /{id}                      → désactive (statut INACTIF) — super_admin
 *   GET    /{id}/professionnels       → PageResult<Professionnel>  — super_admin, admin_etablissement
 *   POST   /{id}/professionnels       → crée (statut INACTIF)      — super_admin, admin_etablissement
 */
@Injectable({ providedIn: 'root' })
export class EtablissementService {
  private readonly http    = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/etablissements`;

  readonly currentPage = signal(0);
  readonly pageSize    = signal(20);
  readonly selectedId  = signal<string | null>(null);

  readonly etablissementsResource = httpResource<PageResult<EtablissementDto>>(() => ({
    url: this.baseUrl,
    params: { page: this.currentPage().toString(), size: this.pageSize().toString() },
  }));

  // .value() lève si la resource est en état 'error' — hasValue() garde-fou.
  readonly etablissements  = computed(() =>
    this.etablissementsResource.hasValue() ? this.etablissementsResource.value().content : []
  );
  readonly totalElements   = computed(() =>
    this.etablissementsResource.hasValue() ? this.etablissementsResource.value().totalElements : 0
  );
  readonly isLoading       = this.etablissementsResource.isLoading;

  readonly professionnelsResource = httpResource<PageResult<ProfessionnelDto>>(
    () => this.selectedId()
      ? { url: `${this.baseUrl}/${this.selectedId()}/professionnels`, params: { page: '0', size: '50' } }
      : undefined
  );

  readonly professionnels = computed(() =>
    this.professionnelsResource.hasValue() ? this.professionnelsResource.value().content : []
  );
  readonly professionnelsLoading = this.professionnelsResource.isLoading;

  select(id: string): void {
    this.selectedId.set(id);
  }

  goToPage(page: number): void {
    this.currentPage.set(page);
  }

  create(etablissement: Partial<EtablissementDto>) {
    return this.http.post<EtablissementDto>(this.baseUrl, etablissement);
  }

  deactivate(id: string) {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  addProfessionnel(etablissementId: string, professionnel: Partial<ProfessionnelDto>) {
    return this.http.post<ProfessionnelDto>(`${this.baseUrl}/${etablissementId}/professionnels`, professionnel);
  }

  reload(): void {
    this.etablissementsResource.reload();
  }
}
