import { Component, OnInit, OnDestroy, inject, signal, computed } from '@angular/core';
import { Subscription } from 'rxjs';
import { StatsService } from '../../core/services/stats.service';
import { SseService } from '../../core/services/sse.service';
import { DashboardStats } from '../../core/models/stats.model';
import { GlucoseObservation } from '../../core/models/alert.model';
import { SeverityBadgeComponent } from '../../shared/components/severity-badge/severity-badge.component';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { BadgeModule } from 'primeng/badge';
import { ScrollPanelModule } from 'primeng/scrollpanel';
import { SkeletonModule } from 'primeng/skeleton';
import { TooltipModule } from 'primeng/tooltip';
import { DatePipe, DecimalPipe } from '@angular/common';

@Component({
  selector: 'cs-dashboard',
  standalone: true,
  imports: [
    CardModule, ButtonModule, TagModule, BadgeModule,
    ScrollPanelModule, SkeletonModule, TooltipModule,
    SeverityBadgeComponent, DatePipe, DecimalPipe,
  ],
  template: `
    <div class="cs-dashboard">
      <div class="cs-dashboard__header">
        <h2 class="cs-dashboard__title">Tableau de bord temps réel</h2>
        <div class="cs-dashboard__live">
          <span class="cs-dashboard__pulse"></span>
          <span>LIVE</span>
        </div>
      </div>

      <!-- KPI row -->
      <div class="cs-kpi-row">
        @if (loading()) {
          @for (_ of [1,2,3,4,5]; track $index) {
            <p-skeleton height="90px" borderRadius="12px" />
          }
        } @else {
          <div class="cs-kpi">
            <i class="pi pi-chart-line cs-kpi__icon cs-kpi__icon--blue"></i>
            <span class="cs-kpi__value">{{ stats()?.totalObservations | number }}</span>
            <span class="cs-kpi__label">Observations</span>
          </div>
          <div class="cs-kpi">
            <i class="pi pi-users cs-kpi__icon cs-kpi__icon--teal"></i>
            <span class="cs-kpi__value">{{ stats()?.totalPatients }}</span>
            <span class="cs-kpi__label">Patients</span>
          </div>
          <div class="cs-kpi">
            <i class="pi pi-wifi cs-kpi__icon cs-kpi__icon--purple"></i>
            <span class="cs-kpi__value">{{ stats()?.totalDevices }}</span>
            <span class="cs-kpi__label">Dispositifs</span>
          </div>
          <div class="cs-kpi">
            <i class="pi pi-bell cs-kpi__icon cs-kpi__icon--orange"></i>
            <span class="cs-kpi__value">{{ stats()?.alertsLast24h }}</span>
            <span class="cs-kpi__label">Alertes 24h</span>
          </div>
          <div class="cs-kpi cs-kpi--critical">
            <i class="pi pi-exclamation-triangle cs-kpi__icon cs-kpi__icon--red"></i>
            <span class="cs-kpi__value">{{ stats()?.criticalAlerts24h }}</span>
            <span class="cs-kpi__label">Critiques 24h</span>
          </div>
        }
      </div>

      <!-- Stream panels -->
      <div class="cs-dashboard__panels">
        <!-- Observations stream -->
        <p-card header="Flux d'observations" styleClass="cs-stream-card">
          <ng-template pTemplate="header">
            <div class="cs-stream-card__head">
              <span>Flux d'observations</span>
              <p-tag [value]="obsCount() + ' reçues'" severity="info" />
            </div>
          </ng-template>
          <p-scrollPanel [style]="{ height: '380px' }">
            @for (obs of observations(); track obs.id) {
              <div class="cs-obs-item">
                <cs-severity-badge [severity]="obs.severity" />
                <div class="cs-obs-item__body">
                  <span class="cs-obs-item__value">{{ obs.value }} {{ obs.unit }}</span>
                  <span class="cs-obs-item__meta">{{ obs.deviceType }} · {{ obs.patientId }}</span>
                </div>
                <span class="cs-obs-item__time">{{ obs.observedAt | date:'HH:mm:ss' }}</span>
              </div>
            } @empty {
              <div class="cs-stream-empty">
                <i class="pi pi-spin pi-spinner"></i>
                <span>En attente du flux SSE…</span>
              </div>
            }
          </p-scrollPanel>
        </p-card>

        <!-- Alerts stream -->
        <p-card styleClass="cs-stream-card">
          <ng-template pTemplate="header">
            <div class="cs-stream-card__head">
              <span>Alertes critiques</span>
              @if (criticalCount() > 0) {
                <p-badge [value]="criticalCount().toString()" severity="danger" />
              }
            </div>
          </ng-template>
          <p-scrollPanel [style]="{ height: '380px' }">
            @for (alert of alerts(); track alert.id) {
              <div class="cs-alert-item" [class.cs-alert-item--critique]="alert.severity === 'CRITIQUE'">
                <cs-severity-badge [severity]="alert.severity" />
                <div class="cs-alert-item__body">
                  <span class="cs-alert-item__msg">{{ alert.message }}</span>
                  <span class="cs-alert-item__meta">Patient {{ alert.patientId }} · {{ alert.deviceType }}</span>
                </div>
                <span class="cs-alert-item__time">{{ alert.observedAt | date:'HH:mm:ss' }}</span>
              </div>
            } @empty {
              <div class="cs-stream-empty">
                <i class="pi pi-check-circle" style="color: var(--green-500)"></i>
                <span>Aucune alerte active</span>
              </div>
            }
          </p-scrollPanel>
        </p-card>
      </div>
    </div>
  `,
  styles: [`
    .cs-dashboard { display: flex; flex-direction: column; gap: 1.5rem; }
    .cs-dashboard__header { display: flex; align-items: center; gap: 1rem; }
    .cs-dashboard__title { margin: 0; font-size: 1.25rem; font-weight: 700; }
    .cs-dashboard__live {
      display: flex; align-items: center; gap: 0.4rem;
      background: var(--green-50); color: var(--green-700);
      padding: 0.25rem 0.75rem; border-radius: 999px; font-size: 0.75rem; font-weight: 700;
    }
    .cs-dashboard__pulse {
      width: 8px; height: 8px; border-radius: 50%;
      background: var(--green-500); animation: pulse 1.5s infinite;
    }
    @keyframes pulse {
      0%, 100% { opacity: 1; transform: scale(1); }
      50% { opacity: 0.4; transform: scale(1.3); }
    }
    .cs-kpi-row { display: grid; grid-template-columns: repeat(5, 1fr); gap: 1rem; }
    .cs-kpi {
      display: flex; flex-direction: column; align-items: center; justify-content: center;
      gap: 0.25rem; padding: 1rem; background: var(--surface-card);
      border-radius: 12px; border: 1px solid var(--surface-border); text-align: center;
    }
    .cs-kpi--critical { border-color: var(--red-200); background: var(--red-50); }
    .cs-kpi__icon { font-size: 1.5rem; margin-bottom: 0.25rem; }
    .cs-kpi__icon--blue { color: var(--blue-500); }
    .cs-kpi__icon--teal { color: var(--teal-500); }
    .cs-kpi__icon--purple { color: var(--purple-500); }
    .cs-kpi__icon--orange { color: var(--orange-500); }
    .cs-kpi__icon--red { color: var(--red-500); }
    .cs-kpi__value { font-size: 1.5rem; font-weight: 700; }
    .cs-kpi__label { font-size: 0.75rem; color: var(--text-color-secondary); }
    .cs-dashboard__panels { display: grid; grid-template-columns: 1fr 1fr; gap: 1.5rem; }
    .cs-stream-card__head { display: flex; align-items: center; justify-content: space-between; padding: 1rem 1rem 0; font-weight: 600; }
    .cs-obs-item, .cs-alert-item {
      display: flex; align-items: center; gap: 0.75rem;
      padding: 0.6rem 0; border-bottom: 1px solid var(--surface-border);
    }
    .cs-alert-item--critique { background: var(--red-50); border-radius: 6px; padding: 0.5rem; margin-bottom: 0.25rem; }
    .cs-obs-item__body, .cs-alert-item__body { flex: 1; display: flex; flex-direction: column; gap: 0.1rem; }
    .cs-obs-item__value, .cs-alert-item__msg { font-weight: 600; font-size: 0.875rem; }
    .cs-obs-item__meta, .cs-alert-item__meta { font-size: 0.75rem; color: var(--text-color-secondary); }
    .cs-obs-item__time, .cs-alert-item__time { font-size: 0.7rem; color: var(--text-color-secondary); white-space: nowrap; }
    .cs-stream-empty { display: flex; align-items: center; justify-content: center; gap: 0.5rem; padding: 2rem; color: var(--text-color-secondary); font-size: 0.875rem; }
  `],
})
export class DashboardComponent implements OnInit, OnDestroy {
  private statsService = inject(StatsService);
  private sse = inject(SseService);
  private subs = new Subscription();

  readonly loading = signal(true);
  readonly stats = signal<DashboardStats | null>(null);
  readonly observations = signal<GlucoseObservation[]>([]);
  readonly alerts = signal<GlucoseObservation[]>([]);

  readonly obsCount = computed(() => this.observations().length);
  readonly criticalCount = computed(() =>
    this.alerts().filter(a => a.severity === 'CRITIQUE').length,
  );

  ngOnInit(): void {
    this.loadStats();
    this.connectStreams();
  }

  private loadStats(): void {
    this.subs.add(
      this.statsService.getStats().subscribe({
        next: s => { this.stats.set(s); this.loading.set(false); },
        error: () => this.loading.set(false),
      }),
    );
    this.subs.add(
      this.sse.stats().subscribe(s => this.stats.set(s)),
    );
  }

  private connectStreams(): void {
    this.subs.add(
      this.sse.observations().subscribe(obs => {
        this.observations.update(list => [obs, ...list].slice(0, 50));
      }),
    );
    this.subs.add(
      this.sse.alerts().subscribe(alert => {
        this.alerts.update(list => [alert, ...list].slice(0, 30));
      }),
    );
  }

  ngOnDestroy(): void {
    this.subs.unsubscribe();
  }
}
