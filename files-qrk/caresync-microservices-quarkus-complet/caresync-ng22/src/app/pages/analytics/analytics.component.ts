import {
  Component, inject, signal, computed, ChangeDetectionStrategy, OnInit
} from '@angular/core';
import { httpResource } from '@angular/common/http';
import { ChartModule } from 'primeng/chart';
import { ButtonModule } from 'primeng/button';
import { SkeletonModule } from 'primeng/skeleton';
import { ThemeService } from '../../core/services/theme.service';
import { environment } from '../../../environments/environment';

type Period = '7j' | '30j' | '90j';

interface GlucosePoint {
  bucket: string;
  avgGlucose: number;
  minGlucose: number;
  maxGlucose: number;
  nbCritiques: number;
  nbUrgentes:  number;
}

/**
 * AnalyticsComponent — httpResource + PrimeNG Charts.
 *
 * Le httpResource se re-fetch automatiquement quand :
 *   - selectedPatientId() change → changement de patient
 *   - selectedPeriod() change → changement de période
 *
 * PrimeNG Chart utilise Chart.js en dessous.
 * Le thème (dark/light) est appliqué dynamiquement via ThemeService.
 */
@Component({
  selector: 'app-analytics',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ChartModule, ButtonModule, SkeletonModule],
  template: `
<div style="display:flex;flex-direction:column;gap:20px">

  <!-- Header -->
  <div style="display:flex;align-items:center;gap:12px;flex-wrap:wrap">
    <div style="flex:1">
      <h2 style="font-size:18px;font-weight:700;color:var(--cs-text);margin:0">Analytics</h2>
      <p style="font-size:13px;color:var(--cs-text-3);margin:2px 0 0">
        Séries temporelles TimescaleDB · time_bucket()
      </p>
    </div>

    <!-- Sélecteur période -->
    <div style="display:flex;gap:4px;background:var(--cs-surface-2);
                padding:3px;border-radius:8px;border:1px solid var(--cs-border)">
      @for (p of periods; track p.value) {
        <button
          style="padding:5px 14px;border-radius:6px;border:none;font-size:12px;
                 font-weight:500;cursor:pointer;transition:all .15s"
          [style.background]="period() === p.value ? 'var(--cs-teal)' : 'transparent'"
          [style.color]="period() === p.value ? 'var(--cs-teal-contrast)' : 'var(--cs-text-2)'"
          (click)="period.set(p.value)">
          {{ p.label }}
        </button>
      }
    </div>
  </div>

  <!-- KPI row -->
  <div style="display:grid;grid-template-columns:repeat(auto-fit,minmax(160px,1fr));gap:12px">
    @if (glucoseResource.isLoading()) {
      @for (_ of [1,2,3,4]; track $index) { <p-skeleton height="80px" borderRadius="10px"/> }
    } @else {
      <div class="cs-kpi cs-kpi--normal" style="padding:14px">
        <div class="kpi-label">Glycémie moyenne</div>
        <div class="kpi-value" style="font-size:24px">{{ avgGlucose() }}</div>
        <div class="kpi-delta">g/L sur {{ period() }}</div>
      </div>
      <div class="cs-kpi cs-kpi--normal" style="padding:14px">
        <div class="kpi-label">Minimum</div>
        <div class="kpi-value" style="font-size:24px">{{ minGlucose() }}</div>
        <div class="kpi-delta">g/L</div>
      </div>
      <div class="cs-kpi cs-kpi--urgente" style="padding:14px">
        <div class="kpi-label">Maximum</div>
        <div class="kpi-value" style="font-size:24px">{{ maxGlucose() }}</div>
        <div class="kpi-delta">g/L</div>
      </div>
      <div class="cs-kpi cs-kpi--critique" style="padding:14px">
        <div class="kpi-label">Épisodes critiques</div>
        <div class="kpi-value" style="font-size:24px">{{ totalCritiques() }}</div>
        <div class="kpi-delta">détectés</div>
      </div>
    }
  </div>

  <!-- Charts row -->
  <div style="display:grid;grid-template-columns:2fr 1fr;gap:16px">

    <!-- Glucose timeline chart -->
    <div class="cs-card">
      <div class="cs-card__header">
        <div>
          <div class="cs-card__title">Glycémie capillaire — {{ period() }}</div>
          <div class="cs-card__subtitle">Agrégée par heure · TimescaleDB time_bucket()</div>
        </div>
      </div>
      @if (glucoseResource.isLoading()) {
        <p-skeleton height="240px" borderRadius="8px"/>
      } @else {
        <p-chart type="line" [data]="glucoseChartData()" [options]="chartOptions()"/>
      }
    </div>

    <!-- Sévérité donut -->
    <div class="cs-card">
      <div class="cs-card__header">
        <div>
          <div class="cs-card__title">Répartition sévérités</div>
          <div class="cs-card__subtitle">{{ period() }}</div>
        </div>
      </div>
      @if (glucoseResource.isLoading()) {
        <p-skeleton height="240px" borderRadius="8px"/>
      } @else {
        <p-chart type="doughnut" [data]="severityChartData()" [options]="donutOptions()"/>
      }
    </div>

  </div>

  <!-- KPI établissement -->
  <div class="cs-card">
    <div class="cs-card__header">
      <div>
        <div class="cs-card__title">KPI Établissement</div>
        <div class="cs-card__subtitle">Mois courant</div>
      </div>
    </div>
    <div style="display:grid;grid-template-columns:repeat(auto-fit,minmax(180px,1fr));gap:12px">
      @for (kpi of kpiData; track kpi.label) {
        <div style="padding:16px;border-radius:10px;background:var(--cs-surface-2);
                    border:1px solid var(--cs-border);text-align:center">
          <div style="font-size:11px;font-weight:600;color:var(--cs-text-3);
                      text-transform:uppercase;letter-spacing:.05em;margin-bottom:8px">
            {{ kpi.label }}
          </div>
          <div style="font-size:28px;font-weight:700;color:var(--cs-text)">{{ kpi.value }}</div>
          <div style="font-size:11px;color:var(--cs-text-3);margin-top:4px">{{ kpi.sub }}</div>
        </div>
      }
    </div>
  </div>

</div>
  `,
})
export class AnalyticsComponent implements OnInit {
  readonly theme = inject(ThemeService);

  readonly period = signal<Period>('30j');
  readonly periods = [
    { value: '7j'  as Period, label: '7 jours' },
    { value: '30j' as Period, label: '30 jours' },
    { value: '90j' as Period, label: '90 jours' },
  ];

  private readonly patientId = signal('PAT-001'); // TODO: sélecteur patient

  // httpResource → re-fetch automatique quand period() ou patientId() change
  readonly glucoseResource = httpResource<GlucosePoint[]>(() => ({
    url: `${environment.apiUrl}/analytics/patients/${this.patientId()}/glucose`,
    params: { days: this.period().replace('j', '') },
  }));

  readonly data = computed(() => this.glucoseResource.hasValue() ? this.glucoseResource.value() : []);

  readonly avgGlucose   = computed(() => this.avg('avgGlucose'));
  readonly minGlucose   = computed(() => this.min('minGlucose'));
  readonly maxGlucose   = computed(() => this.max('maxGlucose'));
  readonly totalCritiques = computed(() => this.data().reduce((s, d) => s + (d.nbCritiques || 0), 0));

  readonly glucoseChartData = computed(() => ({
    labels: this.data().map(d => new Date(d.bucket).toLocaleDateString('fr-BE', { day:'2-digit', month:'short' })),
    datasets: [
      {
        label: 'Glycémie moy. (g/L)',
        data: this.data().map(d => d.avgGlucose),
        borderColor: '#0EA5B4',
        backgroundColor: 'rgba(14,165,180,.1)',
        fill: true, tension: .4,
        pointBackgroundColor: '#0EA5B4', pointRadius: 3,
      },
      {
        label: 'Min',
        data: this.data().map(d => d.minGlucose),
        borderColor: '#10B981',
        borderDash: [4,4], fill: false, tension: .4, pointRadius: 0,
      },
      {
        label: 'Max',
        data: this.data().map(d => d.maxGlucose),
        borderColor: '#EF4444',
        borderDash: [4,4], fill: false, tension: .4, pointRadius: 0,
      },
    ],
  }));

  readonly severityChartData = computed(() => {
    const total = this.data().length || 1;
    const critiques = this.totalCritiques();
    const urgentes  = this.data().reduce((s, d) => s + (d.nbUrgentes || 0), 0);
    const normales  = total - critiques - urgentes;
    return {
      labels: ['Normal', 'Urgente', 'Critique'],
      datasets: [{
        data: [normales, urgentes, critiques],
        backgroundColor: ['#10B981', '#F59E0B', '#EF4444'],
        borderWidth: 0,
      }],
    };
  });

  readonly chartOptions = computed(() => ({
    responsive: true, maintainAspectRatio: false,
    plugins: { legend: { position: 'bottom', labels: { color: this.theme.isDark() ? '#94A3B8' : '#475569', font: { size: 11 } } } },
    scales: {
      x: { ticks: { color: this.theme.isDark() ? '#64748B' : '#94A3B8', maxTicksLimit: 8, font: { size: 10 } }, grid: { color: this.theme.isDark() ? '#1E293B' : '#F1F5F9' } },
      y: { ticks: { color: this.theme.isDark() ? '#64748B' : '#94A3B8', font: { size: 10 } }, grid: { color: this.theme.isDark() ? '#1E293B' : '#F1F5F9' } },
    },
  }));

  readonly donutOptions = () => ({
    responsive: true, maintainAspectRatio: false,
    plugins: { legend: { position: 'bottom', labels: { color: this.theme.isDark() ? '#94A3B8' : '#475569', font: { size: 11 } } } },
    cutout: '65%',
  });

  readonly kpiData = [
    { label: 'Patients actifs',       value: '847',   sub: 'ce mois' },
    { label: 'Alertes critiques',      value: '23',    sub: '98.3% acquittées' },
    { label: 'Latence IoT P95',        value: '320ms', sub: 'MQTT → SSE' },
    { label: 'Dispositifs actifs',     value: '1253',  sub: 'connectés' },
    { label: 'Prescriptions émises',   value: '412',   sub: 'ce mois' },
    { label: 'Taux observance soins',  value: '91%',   sub: 'tâches réalisées' },
  ];

  ngOnInit(): void {}

  private avg(key: keyof GlucosePoint): string {
    const d = this.data();
    if (!d.length) return '—';
    return (d.reduce((s, p) => s + (p[key] as number), 0) / d.length).toFixed(2);
  }
  private min(key: keyof GlucosePoint): string {
    const d = this.data();
    if (!d.length) return '—';
    return Math.min(...d.map(p => p[key] as number)).toFixed(2);
  }
  private max(key: keyof GlucosePoint): string {
    const d = this.data();
    if (!d.length) return '—';
    return Math.max(...d.map(p => p[key] as number)).toFixed(2);
  }
}
