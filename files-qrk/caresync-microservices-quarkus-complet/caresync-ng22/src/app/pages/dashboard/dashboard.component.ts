import {
  Component, inject, computed, OnInit, ChangeDetectionStrategy
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { SkeletonModule } from 'primeng/skeleton';
import { ButtonModule } from 'primeng/button';
import { PatientService } from '../../core/services/patient.service';
import { AlertService } from '../../core/services/alert.service';
import { KeycloakAuthService } from '../../core/auth/keycloak.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, SkeletonModule, ButtonModule],
  template: `
<div style="display:flex;flex-direction:column;gap:20px">

  <!-- Greeting -->
  <div style="display:flex;align-items:center;justify-content:space-between">
    <div>
      <h2 style="font-size:20px;font-weight:700;color:var(--cs-text);margin:0">
        Bonjour, {{ auth.currentUser()?.firstName }} 👋
      </h2>
      <p style="font-size:13px;color:var(--cs-text-3);margin-top:4px;text-transform:capitalize">
        {{ today }}
      </p>
    </div>
    <a routerLink="/alerts">
      <button pButton icon="pi pi-bell" label="Alertes" class="p-button-outlined p-button-sm"></button>
    </a>
  </div>

  <!-- KPI Grid -->
  <div style="display:grid;grid-template-columns:repeat(auto-fit,minmax(200px,1fr));gap:14px">
    @if (patientSvc.isLoading()) {
      @for (_ of [1,2,3,4]; track $index) {
        <p-skeleton height="110px" borderRadius="12px"/>
      }
    } @else {
      <div class="cs-kpi cs-kpi--info">
        <i class="pi pi-users kpi-icon"></i>
        <div class="kpi-label">Patients actifs</div>
        <div class="kpi-value">{{ patientSvc.totalElements() }}</div>
        <div class="kpi-delta">{{ patientSvc.patients().length }} chargés</div>
      </div>
      <div class="cs-kpi cs-kpi--critique">
        <i class="pi pi-exclamation-triangle kpi-icon"></i>
        <div class="kpi-label">Alertes critiques</div>
        <div class="kpi-value">{{ alertSvc.critiquesCount() }}</div>
        <div class="kpi-delta">{{ alertSvc.openAlertsCount() }} ouvertes</div>
      </div>
      <div class="cs-kpi cs-kpi--urgente">
        <i class="pi pi-wifi kpi-icon"></i>
        <div class="kpi-label">Mesures IoT live</div>
        <div class="kpi-value">{{ alertSvc.liveObs().length }}</div>
        <div class="kpi-delta">
          @if (alertSvc.sseConnected()) { Live · SSE } @else { Hors-ligne }
        </div>
      </div>
      <div class="cs-kpi cs-kpi--normal">
        <i class="pi pi-heart kpi-icon"></i>
        <div class="kpi-label">Patients stables</div>
        <div class="kpi-value">{{ stableCount() }}</div>
        <div class="kpi-delta">{{ patientSvc.critiquesCount() }} critiques</div>
      </div>
    }
  </div>

  <!-- Content grid -->
  <div style="display:grid;grid-template-columns:1fr 1fr;gap:16px">

    <!-- Alertes récentes -->
    <div class="cs-card">
      <div class="cs-card__header">
        <div>
          <div class="cs-card__title">Alertes récentes</div>
          <div class="cs-card__subtitle">24 dernières heures</div>
        </div>
        <a routerLink="/alerts"
           style="font-size:12px;color:var(--cs-teal);text-decoration:none">Voir tout →</a>
      </div>

      @for (alert of recentAlerts().slice(0,5); track alert.alertId) {
        <div style="display:flex;align-items:center;gap:10px;padding:9px;border-radius:8px;
                    margin-bottom:4px;background:var(--cs-surface-2)">
          <span class="cs-badge cs-badge--{{ alert.severity.toLowerCase() }}">
            {{ alert.severity }}
          </span>
          <div style="flex:1;min-width:0">
            <div style="font-size:12px;font-weight:600;color:var(--cs-text)">
              {{ alert.patientName }}
            </div>
            <div style="font-size:11px;color:var(--cs-text-3);white-space:nowrap;
                        overflow:hidden;text-overflow:ellipsis">
              {{ alert.message }}
            </div>
          </div>
          <div style="font-size:11px;color:var(--cs-text-3);white-space:nowrap">
            {{ formatTime(alert.createdAt) }}
          </div>
        </div>
      }

      @if (recentAlerts().length === 0) {
        <div style="text-align:center;padding:24px;color:var(--cs-text-3)">
          <i class="pi pi-check-circle" style="font-size:28px;color:var(--cs-normal)"></i>
          <p style="margin-top:8px;font-size:13px">Aucune alerte active</p>
        </div>
      }
    </div>

    <!-- IoT Live Feed -->
    <div class="cs-card">
      <div class="cs-card__header">
        <div>
          <div class="cs-card__title">Mesures IoT temps réel</div>
          <div class="cs-card__subtitle">Flux SSE en direct</div>
        </div>
        @if (alertSvc.sseConnected()) {
          <div class="cs-live"><div class="dot"></div><span>Live</span></div>
        }
      </div>

      @for (obs of alertSvc.liveObs().slice(0,7); track obs.observationId) {
        <div style="display:flex;align-items:center;gap:10px;padding:7px 10px;
                    border-radius:8px;margin-bottom:4px;transition:background .1s">
          <div style="width:30px;height:30px;border-radius:8px;display:flex;align-items:center;
                      justify-content:center;flex-shrink:0;color:#fff;font-size:13px"
               [style.background]="deviceColor(obs.deviceType)">
            <i class="pi" [class]="deviceIcon(obs.deviceType)"></i>
          </div>
          <div style="flex:1;min-width:0">
            <div style="font-size:12px;font-weight:600;color:var(--cs-text)">
              {{ obs.patientName }}
            </div>
            <div style="font-size:10px;color:var(--cs-text-3)">{{ obs.deviceType }}</div>
          </div>
          <div style="display:flex;align-items:baseline;gap:3px">
            <span style="font-size:16px;font-weight:700;color:var(--cs-text)">{{ obs.value }}</span>
            <span style="font-size:10px;color:var(--cs-text-3)">{{ obs.unit }}</span>
          </div>
          <span class="cs-badge cs-badge--{{ obs.severity.toLowerCase() }}">
            {{ obs.severity }}
          </span>
        </div>
      }

      @if (alertSvc.liveObs().length === 0) {
        <div style="text-align:center;padding:24px;color:var(--cs-text-3)">
          <i class="pi pi-wifi" style="font-size:28px"></i>
          <p style="margin-top:8px;font-size:13px">En attente de mesures...</p>
        </div>
      }
    </div>

    <!-- Patients à risque — full width -->
    <div class="cs-card" style="grid-column:1/-1">
      <div class="cs-card__header">
        <div>
          <div class="cs-card__title">Patients à surveiller</div>
          <div class="cs-card__subtitle">Risque urgente ou critique</div>
        </div>
        <a routerLink="/patients"
           style="font-size:12px;color:var(--cs-teal);text-decoration:none">
          Voir tous les patients →
        </a>
      </div>

      <div style="display:grid;grid-template-columns:repeat(auto-fill,minmax(260px,1fr));gap:10px">
        @for (p of riskPatients(); track p.id) {
          <a [routerLink]="['/patients', p.id]"
             style="display:flex;align-items:center;gap:12px;padding:12px;border-radius:10px;
                    background:var(--cs-surface-2);text-decoration:none;
                    border:1px solid transparent;transition:all .15s">
            <div style="width:36px;height:36px;border-radius:50%;display:flex;align-items:center;
                        justify-content:center;font-size:12px;font-weight:700;color:#fff;flex-shrink:0"
                 [style.background]="riskColor(p.riskLevel)">
              {{ p.firstName[0] }}{{ p.lastName[0] }}
            </div>
            <div style="flex:1;min-width:0">
              <div style="font-size:13px;font-weight:600;color:var(--cs-text)">
                {{ p.firstName }} {{ p.lastName }}
              </div>
              <div style="font-size:11px;color:var(--cs-text-3)">
                {{ p.pathology }} · {{ p.service }}
              </div>
            </div>
            <span class="cs-badge cs-badge--{{ p.riskLevel }}">{{ p.riskLevel }}</span>
          </a>
        }

        @if (riskPatients().length === 0) {
          <div style="text-align:center;padding:20px;color:var(--cs-text-3);grid-column:1/-1">
            <i class="pi pi-shield" style="font-size:28px;color:var(--cs-normal)"></i>
            <p style="margin-top:8px;font-size:13px">Tous les patients sont stables</p>
          </div>
        }
      </div>
    </div>
  </div>
</div>
  `,
})
export class DashboardComponent implements OnInit {
  readonly patientSvc = inject(PatientService);
  readonly alertSvc   = inject(AlertService);
  readonly auth       = inject(KeycloakAuthService);

  readonly today = new Date().toLocaleDateString('fr-BE', {
    weekday: 'long', day: 'numeric', month: 'long'
  });

  readonly stableCount = computed(() =>
    this.patientSvc.totalElements()
    - this.patientSvc.critiquesCount()
    - this.patientSvc.urgentesCount()
  );

  readonly recentAlerts = computed(() => [
    ...this.alertSvc.liveAlerts(),
    ...this.alertSvc.alertsHistory(),
  ].slice(0, 6));

  readonly riskPatients = computed(() =>
    this.patientSvc.patients()
      .filter(p => p.riskLevel === 'critique' || p.riskLevel === 'urgente')
      .slice(0, 6)
  );

  ngOnInit(): void { this.patientSvc.search(''); }

  formatTime(iso: string): string {
    return new Date(iso).toLocaleTimeString('fr-BE', { hour: '2-digit', minute: '2-digit' });
  }

  deviceIcon(t: string): string {
    const m: Record<string,string> = {
      GLUCOMETER:'pi-heart', PULSE_OXIMETER:'pi-bolt',
      SCALE:'pi-chart-bar',  BP_MONITOR:'pi-ellipsis-h', SPIROMETER:'pi-send'
    };
    return m[t] ?? 'pi-wifi';
  }

  deviceColor(t: string): string {
    const m: Record<string,string> = {
      GLUCOMETER:'#0EA5B4', PULSE_OXIMETER:'#7C3AED',
      SCALE:'#059669', BP_MONITOR:'#DC2626', SPIROMETER:'#D97706'
    };
    return m[t] ?? '#64748B';
  }

  riskColor(r: string): string {
    const m: Record<string,string> = {
      critique:'#EF4444', urgente:'#F59E0B',
      informative:'#3B82F6', normal:'#10B981'
    };
    return m[r] ?? '#64748B';
  }
}
