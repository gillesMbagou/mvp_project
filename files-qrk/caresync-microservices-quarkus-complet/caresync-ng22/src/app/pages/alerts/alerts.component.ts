import {
  Component, inject, signal, computed, ChangeDetectionStrategy
} from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { TimelineModule } from 'primeng/timeline';
import { AlertService, AlertEvent } from '../../core/services/alert.service';

type SeverityFilter = 'ALL' | 'CRITIQUE' | 'URGENTE';

@Component({
  selector: 'app-alerts',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ButtonModule, TagModule, TimelineModule],
  template: `
<div style="display:flex;flex-direction:column;gap:20px">

  <!-- Header -->
  <div style="display:flex;align-items:center;gap:12px;flex-wrap:wrap">
    <div style="flex:1">
      <h2 style="font-size:18px;font-weight:700;color:var(--cs-text);margin:0">
        Alertes cliniques
      </h2>
      <p style="font-size:13px;color:var(--cs-text-3);margin:2px 0 0">
        {{ alertSvc.openAlertsCount() }} alerte(s) ouverte(s) ·
        @if (alertSvc.sseConnected()) {
          <span style="color:var(--cs-normal)">● Flux SSE actif</span>
        } @else {
          <span style="color:var(--cs-critique)">● Flux SSE déconnecté</span>
        }
      </p>
    </div>

    <!-- Filtres sévérité -->
    <div style="display:flex;gap:6px">
      @for (f of severityFilters; track f.value) {
        <button
          style="padding:5px 14px;border-radius:20px;border:1px solid;font-size:12px;
                 font-weight:500;cursor:pointer;transition:all .15s"
          [style.background]="filter() === f.value ? f.color : 'transparent'"
          [style.color]="filter() === f.value ? '#fff' : f.color"
          [style.border-color]="f.color"
          (click)="filter.set(f.value)">
          {{ f.label }}
        </button>
      }
    </div>
  </div>

  <!-- Two-column: Live + History -->
  <div style="display:grid;grid-template-columns:1fr 1fr;gap:16px">

    <!-- Live stream -->
    <div class="cs-card">
      <div class="cs-card__header">
        <div>
          <div class="cs-card__title">Alertes temps réel</div>
          <div class="cs-card__subtitle">Flux SSE depuis alert-svc</div>
        </div>
        <div class="cs-live">
          <div class="dot" [style.background]="alertSvc.sseConnected() ? 'var(--cs-normal)' : 'var(--cs-text-3)'"></div>
          <span>{{ alertSvc.sseConnected() ? 'Live' : 'Offline' }}</span>
        </div>
      </div>

      <div style="display:flex;flex-direction:column;gap:8px;max-height:520px;overflow-y:auto">
        @for (alert of filteredLive(); track alert.alertId) {
          <div class="alert-card" [class]="'alert-card--' + alert.severity.toLowerCase()">
            <div style="display:flex;align-items:center;gap:10px;margin-bottom:8px">
              <span class="cs-badge cs-badge--{{ alert.severity.toLowerCase() }}">
                {{ alert.severity }}
              </span>
              <span style="font-size:12px;font-weight:600;color:var(--cs-text)">
                {{ alert.patientName }}
              </span>
              <span style="font-size:10px;color:var(--cs-text-3);margin-left:auto">
                {{ formatTime(alert.createdAt) }}
              </span>
            </div>

            <div style="font-size:13px;color:var(--cs-text);margin-bottom:6px">
              {{ alert.message }}
            </div>

            <div style="display:flex;align-items:center;gap:8px">
              <span style="font-size:11px;color:var(--cs-text-3)">{{ alert.deviceType }}</span>
              <span style="font-size:11px;font-weight:600;color:var(--cs-text)">
                → {{ alert.triggeredValue }}
              </span>
              <button pButton icon="pi pi-check" label="Acquitter"
                      class="p-button-text p-button-sm"
                      style="margin-left:auto;font-size:11px"
                      (click)="acknowledgeAlert(alert.alertId)"></button>
            </div>
          </div>
        }

        @if (filteredLive().length === 0) {
          <div style="text-align:center;padding:32px;color:var(--cs-text-3)">
            <i class="pi pi-check-circle" style="font-size:32px;color:var(--cs-normal)"></i>
            <p style="margin-top:8px;font-size:13px">Aucune alerte live active</p>
          </div>
        }
      </div>
    </div>

    <!-- Historique -->
    <div class="cs-card">
      <div class="cs-card__header">
        <div>
          <div class="cs-card__title">Historique 24h</div>
          <div class="cs-card__subtitle">{{ alertSvc.alertsHistory().length }} alertes</div>
        </div>
      </div>

      <div style="display:flex;flex-direction:column;gap:6px;max-height:520px;overflow-y:auto">
        @for (alert of filteredHistory(); track alert.alertId) {
          <div style="display:flex;gap:10px;padding:10px;border-radius:8px;
                      background:var(--cs-surface-2);transition:background .1s"
               onmouseover="this.style.background='var(--cs-border)'"
               onmouseout="this.style.background='var(--cs-surface-2)'">

            <!-- Sévérité indicator -->
            <div style="width:3px;border-radius:2px;flex-shrink:0;min-height:48px"
                 [style.background]="severityColor(alert.severity)"></div>

            <div style="flex:1;min-width:0">
              <div style="display:flex;align-items:center;gap:8px;margin-bottom:4px">
                <span class="cs-badge cs-badge--{{ alert.severity.toLowerCase() }}">
                  {{ alert.severity }}
                </span>
                <span style="font-size:11px;color:var(--cs-text-3)">
                  {{ formatTime(alert.createdAt) }}
                </span>
                @if (alert.statut === 'ACQUITTEE') {
                  <span style="font-size:10px;color:var(--cs-normal);margin-left:auto">
                    ✓ Acquittée
                  </span>
                }
                @if (alert.statut === 'ESCALADEE') {
                  <span style="font-size:10px;color:var(--cs-urgente);margin-left:auto">
                    ⬆ Escaladée
                  </span>
                }
              </div>
              <div style="font-size:12px;font-weight:600;color:var(--cs-text)">
                {{ alert.patientName }}
              </div>
              <div style="font-size:11px;color:var(--cs-text-2);margin-top:2px">
                {{ alert.message }} — {{ alert.triggeredValue }}
              </div>
            </div>
          </div>
        }

        @if (filteredHistory().length === 0) {
          <div style="text-align:center;padding:32px;color:var(--cs-text-3)">
            <i class="pi pi-calendar" style="font-size:28px"></i>
            <p style="margin-top:8px;font-size:13px">Aucune alerte dans l'historique</p>
          </div>
        }
      </div>
    </div>

  </div>
</div>
  `,
  styles: [`
    .alert-card {
      padding: 12px; border-radius: 10px; border: 1px solid;
      background: var(--cs-surface-2); transition: all .15s;
    }
    .alert-card:hover { transform: translateY(-1px); box-shadow: 0 4px 12px rgba(0,0,0,.08); }
    .alert-card--critique { border-color: var(--cs-critique); background: var(--cs-critique-bg); }
    .alert-card--urgente  { border-color: var(--cs-urgente);  background: var(--cs-urgente-bg);  }
    .alert-card--informative { border-color: var(--cs-informative); background: var(--cs-info-bg); }
  `],
})
export class AlertsComponent {
  readonly alertSvc = inject(AlertService);

  readonly filter = signal<SeverityFilter>('ALL');

  readonly severityFilters = [
    { value: 'ALL'      as SeverityFilter, label: 'Toutes',   color: '#64748B' },
    { value: 'CRITIQUE' as SeverityFilter, label: 'Critiques', color: '#EF4444' },
    { value: 'URGENTE'  as SeverityFilter, label: 'Urgentes',  color: '#F59E0B' },
  ];

  readonly filteredLive = computed(() => {
    const f = this.filter();
    const alerts = this.alertSvc.liveAlerts();
    return f === 'ALL' ? alerts : alerts.filter(a => a.severity === f);
  });

  readonly filteredHistory = computed(() => {
    const f = this.filter();
    const alerts = this.alertSvc.alertsHistory();
    return f === 'ALL' ? alerts : alerts.filter(a => a.severity === f);
  });

  acknowledgeAlert(id: string): void {
    this.alertSvc.acquitter(id).subscribe({
      error: err => console.error('[Alertes] Échec acquittement', err),
    });
  }

  formatTime(iso: string): string {
    return new Date(iso).toLocaleTimeString('fr-BE', { hour: '2-digit', minute: '2-digit' });
  }

  severityColor(s: string): string {
    return { CRITIQUE:'#EF4444', URGENTE:'#F59E0B', INFORMATIVE:'#3B82F6' }[s] ?? '#64748B';
  }
}
