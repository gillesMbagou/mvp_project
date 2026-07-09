import { Component, inject, input, effect, ChangeDetectionStrategy } from '@angular/core';
import { httpResource } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { SkeletonModule } from 'primeng/skeleton';
import { EtablissementService } from '../../core/services/etablissement.service';
import { KpiCardComponent } from '../../shared/components/kpi-card/kpi-card.component';
import { environment } from '../../../environments/environment';
import { KpiEtablissementDto } from '../../core/models';

@Component({
  selector: 'app-etablissement-detail',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, ButtonModule, TagModule, SkeletonModule, KpiCardComponent],
  template: `
<div style="display:flex;flex-direction:column;gap:20px">

  <a routerLink="/etablissements">
    <button pButton icon="pi pi-arrow-left" class="p-button-text p-button-sm" label="Retour"></button>
  </a>

  @if (etabSvc.professionnelsLoading()) {
    <p-skeleton height="100px" borderRadius="12px"/>
  } @else {

    <div style="display:grid;grid-template-columns:repeat(auto-fit,minmax(180px,1fr));gap:12px">
      @if (kpiResource.isLoading()) {
        @for (_ of [1,2,3,4]; track $index) { <p-skeleton height="90px" borderRadius="10px"/> }
      } @else if (kpiResource.error()) {
        <div class="cs-card" style="grid-column:1/-1;color:var(--cs-text-3);font-size:12px">
          KPI indisponibles (analytics-svc / établissement non trouvé).
        </div>
      } @else if (kpiResource.value(); as kpi) {
        <app-kpi-card label="Patients actifs" [value]="kpi.patientsActifs" tone="info"/>
        <app-kpi-card label="Alertes critiques" [value]="kpi.alertesCritiques" tone="critique"/>
        <app-kpi-card label="Taux acquittement" [value]="(kpi.tauxAquittement * 100).toFixed(0) + '%'" tone="normal"/>
        <app-kpi-card label="Dispositifs actifs" [value]="kpi.dispositifsActifs" tone="urgente"/>
      }
    </div>

    <div class="cs-card">
      <div class="cs-card__header">
        <div class="cs-card__title">Professionnels</div>
      </div>
      <div style="display:flex;flex-direction:column;gap:6px">
        @for (p of etabSvc.professionnels(); track p.id) {
          <div style="display:flex;align-items:center;gap:10px;padding:9px;border-radius:8px;background:var(--cs-surface-2)">
            <div style="flex:1;min-width:0">
              <div style="font-size:13px;font-weight:600;color:var(--cs-text)">{{ p.prenom }} {{ p.nom }}</div>
              <div style="font-size:11px;color:var(--cs-text-3)">{{ p.role }} @if (p.service) { · {{ p.service }} }</div>
            </div>
            <p-tag [severity]="p.statut === 'ACTIF' ? 'success' : 'warn'" [value]="p.statut" [rounded]="true"/>
          </div>
        }
        @if (etabSvc.professionnels().length === 0) {
          <div style="text-align:center;padding:24px;color:var(--cs-text-3);font-size:13px">
            Aucun professionnel enregistré
          </div>
        }
      </div>
    </div>
  }
</div>
  `,
})
export class EtablissementDetailComponent {
  readonly etabSvc = inject(EtablissementService);

  readonly id = input<string>('');

  readonly kpiResource = httpResource<KpiEtablissementDto>(() =>
    this.id() ? { url: `${environment.apiUrl}/analytics/etablissements/${this.id()}/kpi` } : undefined
  );

  constructor() {
    effect(() => {
      if (this.id()) this.etabSvc.select(this.id());
    });
  }
}
