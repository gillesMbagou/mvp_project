import { Injectable, inject, signal, computed } from '@angular/core';
import { httpResource } from '@angular/common/http';
import { environment } from '../../../environments/environment';

// ── DTOs (Zod schemas = source de vérité) ───────────────────────────────────
import { z } from 'zod';

export const PatientSchema = z.object({
  id:              z.string(),
  firstName:       z.string(),
  lastName:        z.string(),
  age:             z.number(),
  gender:          z.enum(['M', 'F']),
  dateOfBirth:     z.string(),
  insNir:          z.string().optional(),
  pathology:       z.string(),
  service:         z.string().optional(),
  riskLevel:       z.enum(['normal', 'informative', 'urgente', 'critique']),
  activeAlerts:    z.number(),
  connectedDevices:z.number(),
  lastMeasure:     z.string().optional(),
  lastMeasureAt:   z.string().optional(),
});

export type PatientDto = z.infer<typeof PatientSchema>;

export const PageSchema = <T extends z.ZodType>(itemSchema: T) => z.object({
  content:       z.array(itemSchema),
  totalElements: z.number(),
  page:          z.number(),
  size:          z.number(),
});

export type PageResult<T> = { content: T[]; totalElements: number; page: number; size: number };

function normalizeRiskLevel(p: PatientDto): PatientDto {
  return p.riskLevel
    ? { ...p, riskLevel: p.riskLevel.toLowerCase() as PatientDto['riskLevel'] }
    : p;
}

/**
 * PatientService — utilise httpResource() (Angular 22).
 *
 * httpResource crée un Resource Signal qui :
 *   - Re-fetch automatiquement quand ses dépendances changent
 *   - Expose .value(), .isLoading(), .error(), .reload()
 *   - Annule les requêtes précédentes (pas de race condition)
 *
 * C'est le remplacement réactif de HttpClient + subscribe.
 */
@Injectable({ providedIn: 'root' })
export class PatientService {
  private readonly baseUrl = environment.apiUrl;

  // ── Filtres Signal (partagés entre composants) ───────────────────────────
  readonly searchQuery = signal('');
  readonly currentPage = signal(0);
  readonly pageSize    = signal(20);
  readonly selectedId  = signal<string | null>(null);

  // ── Liste patients — re-fetch automatique sur searchQuery ou page ─────────
  readonly patientsResource = httpResource<PageResult<PatientDto>>(
    () => ({
      url: `${this.baseUrl}/patients`,
      params: {
        search: this.searchQuery(),
        page:   this.currentPage().toString(),
        size:   this.pageSize().toString(),
      },
    })
  );

  // Dérivés
  // Le backend (Patient.RiskLevel, enum Java) renvoie riskLevel en MAJUSCULES
  // ("CRITIQUE") alors que le frontend (classes CSS cs-badge--critique, filtres
  // KPI) est bâti sur des valeurs en minuscules — normalisé ici, à la frontière
  // API, pour que tous les composants en aval restent inchangés.
  // .value() lève si la resource est en état 'error' — hasValue() garde-fou.
  readonly patients = computed(() =>
    this.patientsResource.hasValue()
      ? this.patientsResource.value().content.map(normalizeRiskLevel)
      : []
  );
  readonly totalElements = computed(() =>
    this.patientsResource.hasValue() ? this.patientsResource.value().totalElements : 0
  );
  readonly isLoading     = this.patientsResource.isLoading;

  // Compteurs KPI dashboard
  readonly critiquesCount = computed(() =>
    this.patients().filter(p => p.riskLevel === 'critique').length
  );
  readonly urgentesCount = computed(() =>
    this.patients().filter(p => p.riskLevel === 'urgente').length
  );

  // ── Détail patient — httpResource avec route dynamique ───────────────────
  readonly patientDetailResource = httpResource<PatientDto>(
    () => this.selectedId()
      ? { url: `${this.baseUrl}/patients/${this.selectedId()}` }
      : undefined   // undefined = pas de requête
  );

  readonly selectedPatient = computed(() =>
    this.patientDetailResource.hasValue()
      ? normalizeRiskLevel(this.patientDetailResource.value())
      : undefined
  );
  readonly detailLoading   = this.patientDetailResource.isLoading;

  // ── Actions ───────────────────────────────────────────────────────────────
  selectPatient(id: string): void {
    this.selectedId.set(id);
  }

  search(query: string): void {
    this.searchQuery.set(query);
    this.currentPage.set(0);  // reset pagination
  }

  goToPage(page: number): void {
    this.currentPage.set(page);
  }

  reload(): void {
    this.patientsResource.reload();
  }
}
