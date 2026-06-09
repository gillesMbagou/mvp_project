import { Component, OnInit, inject, signal, input } from '@angular/core';
import { ObservationService } from '../../core/services/observation.service';
import { SseService } from '../../core/services/sse.service';
import { ObservationRecord, ObservationPage } from '../../core/models/observation.model';
import { SeverityBadgeComponent } from '../../shared/components/severity-badge/severity-badge.component';
import { CardModule } from 'primeng/card';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { SkeletonModule } from 'primeng/skeleton';
import { TagModule } from 'primeng/tag';
import { SelectModule } from 'primeng/select';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';

const DEVICE_TYPES = ['GLUCOSE_MONITOR', 'HEART_RATE', 'BLOOD_PRESSURE', 'SPO2', 'TEMPERATURE', 'WEIGHT'];

@Component({
  selector: 'cs-patient-detail',
  standalone: true,
  imports: [
    CardModule, TableModule, ButtonModule, SkeletonModule,
    TagModule, SelectModule, SeverityBadgeComponent, DatePipe, FormsModule,
  ],
  template: `
    <div class="cs-page">
      <div class="cs-page__header">
        <h2 class="cs-page__title">Patient : {{ id() }}</h2>
        <p-select
          [options]="deviceTypes"
          [(ngModel)]="selectedDevice"
          placeholder="Type de dispositif"
          (onChange)="loadTimeSeries()"
        />
      </div>

      <div class="cs-detail-grid">
        <!-- Timeseries -->
        <p-card header="Séries temporelles (24h)">
          @if (loadingTs()) {
            <p-skeleton height="300px" />
          } @else {
            <p-table [value]="timeSeries()" [scrollable]="true" scrollHeight="300px" styleClass="p-datatable-sm">
              <ng-template pTemplate="header">
                <tr>
                  <th>Date/Heure</th>
                  <th>Valeur</th>
                  <th>Unité</th>
                  <th>Sévérité</th>
                </tr>
              </ng-template>
              <ng-template pTemplate="body" let-obs>
                <tr>
                  <td>{{ obs.observedAt | date:'dd/MM HH:mm:ss' }}</td>
                  <td><strong>{{ obs.value }}</strong></td>
                  <td>{{ obs.unit }}</td>
                  <td><cs-severity-badge [severity]="obs.severity" /></td>
                </tr>
              </ng-template>
            </p-table>
          }
        </p-card>

        <!-- Live stream -->
        <p-card header="Flux en temps réel">
          <div class="cs-live-feed">
            @for (obs of liveObs(); track obs.id) {
              <div class="cs-live-item">
                <cs-severity-badge [severity]="obs.severity" />
                <span>{{ obs.value }} {{ obs.unit }}</span>
                <span class="cs-live-item__time">{{ obs.observedAt | date:'HH:mm:ss' }}</span>
              </div>
            } @empty {
              <p class="cs-stream-empty">En attente du flux…</p>
            }
          </div>
        </p-card>
      </div>

      <!-- Paginated observations -->
      <p-card header="Toutes les observations">
        <p-table
          [value]="page()?.content ?? []"
          [paginator]="true"
          [rows]="(page()?.size ?? 50)"
          [totalRecords]="(page()?.totalElements ?? 0)"
          [lazy]="true"
          (onPage)="onPage($event)"
          styleClass="p-datatable-sm"
          responsiveLayout="scroll"
        >
          <ng-template pTemplate="header">
            <tr>
              <th>Date/Heure</th>
              <th>Type</th>
              <th>LOINC</th>
              <th>Valeur</th>
              <th>Unité</th>
              <th>Sévérité</th>
            </tr>
          </ng-template>
          <ng-template pTemplate="body" let-obs>
            <tr>
              <td>{{ obs.observedAt | date:'dd/MM/yy HH:mm:ss' }}</td>
              <td>{{ obs.deviceType }}</td>
              <td><code>{{ obs.loincCode }}</code></td>
              <td><strong>{{ obs.value }}</strong></td>
              <td>{{ obs.unit }}</td>
              <td><cs-severity-badge [severity]="obs.severity" /></td>
            </tr>
          </ng-template>
        </p-table>
      </p-card>
    </div>
  `,
  styles: [`
    .cs-page { display: flex; flex-direction: column; gap: 1.25rem; }
    .cs-page__header { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 0.75rem; }
    .cs-page__title { margin: 0; font-size: 1.25rem; font-weight: 700; }
    .cs-detail-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 1.25rem; }
    .cs-live-feed { display: flex; flex-direction: column; gap: 0.5rem; max-height: 300px; overflow-y: auto; }
    .cs-live-item { display: flex; align-items: center; gap: 0.75rem; padding: 0.4rem 0; border-bottom: 1px solid var(--surface-border); font-size: 0.875rem; }
    .cs-live-item__time { margin-left: auto; color: var(--text-color-secondary); font-size: 0.75rem; }
    .cs-stream-empty { text-align: center; color: var(--text-color-secondary); padding: 2rem; }
  `],
})
export class PatientDetailComponent implements OnInit {
  readonly id = input.required<string>();

  private obsService = inject(ObservationService);
  private sse = inject(SseService);
  private sub = new Subscription();

  readonly loading = signal(true);
  readonly loadingTs = signal(false);
  readonly page = signal<ObservationPage | null>(null);
  readonly timeSeries = signal<ObservationRecord[]>([]);
  readonly liveObs = signal<ObservationRecord[]>([]);

  deviceTypes = DEVICE_TYPES;
  selectedDevice = DEVICE_TYPES[0];

  ngOnInit(): void {
    this.loadObservations(0);
    this.loadTimeSeries();
    this.sub.add(
      this.sse.observations(undefined, this.id()).subscribe(obs => {
        this.liveObs.update(list => [obs as unknown as ObservationRecord, ...list].slice(0, 30));
      }),
    );
  }

  loadObservations(pageIndex: number): void {
    this.loading.set(true);
    this.obsService.getObservations(this.id(), pageIndex).subscribe({
      next: p => { this.page.set(p); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  loadTimeSeries(): void {
    this.loadingTs.set(true);
    this.obsService.getTimeSeries(this.id(), this.selectedDevice).subscribe({
      next: ts => { this.timeSeries.set(ts); this.loadingTs.set(false); },
      error: () => this.loadingTs.set(false),
    });
  }

  onPage(event: { first: number; rows: number }): void {
    this.loadObservations(event.first / event.rows);
  }
}
