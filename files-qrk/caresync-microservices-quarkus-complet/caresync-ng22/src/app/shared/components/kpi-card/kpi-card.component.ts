import { Component, input, ChangeDetectionStrategy } from '@angular/core';

export type KpiTone = 'critique' | 'urgente' | 'info' | 'normal';

/**
 * KpiCardComponent — tuile KPI réutilisable (cf. .cs-kpi dans styles.scss).
 * Utilise les inputs signaux (Angular 22).
 */
@Component({
  selector: 'app-kpi-card',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="cs-kpi" [class]="'cs-kpi--' + tone()">
      @if (icon()) { <i class="pi kpi-icon" [class]="icon()"></i> }
      <div class="kpi-label">{{ label() }}</div>
      <div class="kpi-value">{{ value() }}</div>
      @if (delta()) { <div class="kpi-delta">{{ delta() }}</div> }
    </div>
  `,
})
export class KpiCardComponent {
  readonly label = input.required<string>();
  readonly value = input.required<string | number>();
  readonly delta = input<string>('');
  readonly icon  = input<string>('');
  readonly tone  = input<KpiTone>('info');
}
