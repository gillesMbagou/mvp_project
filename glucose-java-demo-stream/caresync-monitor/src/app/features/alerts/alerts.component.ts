import { Component, OnInit, OnDestroy, inject, signal, computed } from '@angular/core';
import { Subscription } from 'rxjs';
import { ObservationService } from '../../core/services/observation.service';
import { SseService } from '../../core/services/sse.service';
import { ObservationRecord } from '../../core/models/observation.model';
import { GlucoseObservation } from '../../core/models/alert.model';
import { SeverityBadgeComponent } from '../../shared/components/severity-badge/severity-badge.component';
import { CardModule } from 'primeng/card';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { SkeletonModule } from 'primeng/skeleton';
import { SelectButtonModule } from 'primeng/selectbutton';
import { BadgeModule } from 'primeng/badge';
import { ScrollPanelModule } from 'primeng/scrollpanel';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'cs-alerts',
  standalone: true,
  imports: [
    CardModule, TableModule, ButtonModule, SkeletonModule,
    SelectButtonModule, BadgeModule, ScrollPanelModule,
    SeverityBadgeComponent, DatePipe, FormsModule,
  ],
  template: `
    <div class="cs-page">
      <div class="cs-page__header">
        <h2 class="cs-page__title">
          Alertes cliniques
          @if (liveAlerts().length > 0) {
            <p-badge [value]="liveAlerts().length.toString()" severity="danger" />
          }
        </h2>
        <p-selectButton
          [options]="hoursOptions"
          [(ngModel)]="hours"
          optionLabel="label"
          optionValue="value"
          (onChange)="loadHistory()"
        />
      </div>

      <div class="cs-alerts-grid">
        <!-- Live stream -->
        <p-card>
          <ng-template pTemplate="header">
            <div class="cs-card-head">
              <span>Flux en direct</span>
              <span class="cs-live-dot"></span>
            </div>
          </ng-template>
          <p-scrollPanel [style]="{ height: '400px' }">
            @for (a of liveAlerts(); track a.id) {
              <div class="cs-alert-row" [class.cs-alert-row--critique]="a.severity === 'CRITIQUE'">
                <cs-severity-badge [severity]="a.severity" />
                <div class="cs-alert-row__body">
                  <span class="cs-alert-row__msg">{{ a.message }}</span>
                  <span class="cs-alert-row__meta">Patient {{ a.patientId }} · {{ a.deviceType }}</span>
                </div>
                <span class="cs-alert-row__time">{{ a.observedAt | date:'HH:mm:ss' }}</span>
              </div>
            } @empty {
              <div class="cs-empty"><i class="pi pi-check-circle"></i> Aucune alerte active</div>
            }
          </p-scrollPanel>
        </p-card>

        <!-- History -->
        <p-card [header]="'Historique (' + hours + 'h)'">
          @if (loadingHistory()) {
            <p-skeleton height="400px" />
          } @else {
            <p-table [value]="history()" [scrollable]="true" scrollHeight="400px" styleClass="p-datatable-sm">
              <ng-template pTemplate="header">
                <tr>
                  <th>Date/Heure</th>
                  <th>Patient</th>
                  <th>Type</th>
                  <th>Valeur</th>
                  <th>Sévérité</th>
                </tr>
              </ng-template>
              <ng-template pTemplate="body" let-a>
                <tr>
                  <td>{{ a.observedAt | date:'dd/MM HH:mm:ss' }}</td>
                  <td>{{ a.patientId }}</td>
                  <td>{{ a.deviceType }}</td>
                  <td><strong>{{ a.value }} {{ a.unit }}</strong></td>
                  <td><cs-severity-badge [severity]="a.severity" /></td>
                </tr>
              </ng-template>
            </p-table>
          }
        </p-card>
      </div>
    </div>
  `,
  styles: [`
    .cs-page { display: flex; flex-direction: column; gap: 1.25rem; }
    .cs-page__header { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 0.75rem; }
    .cs-page__title { margin: 0; font-size: 1.25rem; font-weight: 700; display: flex; align-items: center; gap: 0.5rem; }
    .cs-alerts-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 1.25rem; }
    .cs-card-head { display: flex; align-items: center; gap: 0.5rem; padding: 1rem 1rem 0; font-weight: 600; }
    .cs-live-dot { width: 8px; height: 8px; border-radius: 50%; background: var(--green-500); animation: pulse 1.5s infinite; }
    @keyframes pulse { 0%,100%{opacity:1} 50%{opacity:0.4} }
    .cs-alert-row {
      display: flex; align-items: flex-start; gap: 0.75rem;
      padding: 0.65rem 0; border-bottom: 1px solid var(--surface-border);
    }
    .cs-alert-row--critique { background: var(--red-50); border-radius: 8px; padding: 0.5rem; margin-bottom: 0.25rem; }
    .cs-alert-row__body { flex: 1; display: flex; flex-direction: column; gap: 0.1rem; }
    .cs-alert-row__msg { font-size: 0.875rem; font-weight: 600; }
    .cs-alert-row__meta { font-size: 0.75rem; color: var(--text-color-secondary); }
    .cs-alert-row__time { font-size: 0.7rem; color: var(--text-color-secondary); white-space: nowrap; margin-top: 0.1rem; }
    .cs-empty { display: flex; align-items: center; gap: 0.5rem; padding: 2rem; justify-content: center; color: var(--text-color-secondary); }
  `],
})
export class AlertsComponent implements OnInit, OnDestroy {
  private obsService = inject(ObservationService);
  private sse = inject(SseService);
  private sub = new Subscription();

  hours = 24;
  hoursOptions = [{ label: '6h', value: 6 }, { label: '24h', value: 24 }, { label: '48h', value: 48 }];

  readonly loadingHistory = signal(true);
  readonly history = signal<ObservationRecord[]>([]);
  readonly liveAlerts = signal<GlucoseObservation[]>([]);

  ngOnInit(): void {
    this.loadHistory();
    this.sub.add(
      this.sse.alerts().subscribe(a => {
        this.liveAlerts.update(list => [a, ...list].slice(0, 50));
      }),
    );
  }

  loadHistory(): void {
    this.loadingHistory.set(true);
    this.obsService.getAlerts(this.hours).subscribe({
      next: list => { this.history.set(list); this.loadingHistory.set(false); },
      error: () => this.loadingHistory.set(false),
    });
  }

  ngOnDestroy(): void {
    this.sub.unsubscribe();
  }
}
