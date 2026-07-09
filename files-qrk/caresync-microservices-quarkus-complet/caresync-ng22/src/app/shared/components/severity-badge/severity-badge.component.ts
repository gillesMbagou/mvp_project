import { Component, input, computed, ChangeDetectionStrategy } from '@angular/core';
import { TagModule } from 'primeng/tag';

type Severity = 'CRITIQUE' | 'URGENTE' | 'INFORMATIVE' | 'NORMAL' | string | undefined;

/**
 * SeverityBadgeComponent — Badge PrimeNG réutilisable.
 *
 * Utilise les inputs signaux d'Angular 22 (input()).
 * Pas besoin de @Input() decorator.
 */
@Component({
  selector:        'app-severity-badge',
  standalone:      true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TagModule],
  template: `
    <p-tag
      [severity]="tagSeverity()"
      [value]="label()"
      [rounded]="true"
      [icon]="icon()"
      [styleClass]="'severity-badge severity-badge--' + (severity() ?? 'normal').toLowerCase()"
    />
  `,
})
export class SeverityBadgeComponent {

  /** Valeur de sévérité (CRITIQUE, URGENTE, INFORMATIVE, NORMAL) */
  readonly severity = input<Severity>('NORMAL');
  readonly compact  = input<boolean>(false);

  readonly label = computed(() => {
    if (this.compact()) return '';
    const map: Record<string, string> = {
      CRITIQUE:    'Critique',
      URGENTE:     'Urgente',
      INFORMATIVE: 'Info',
      NORMAL:      'Stable',
    };
    return map[this.severity()?.toUpperCase() ?? ''] ?? (this.severity() ?? 'Stable');
  });

  readonly tagSeverity = computed((): 'danger' | 'warn' | 'info' | 'success' => {
    const map: Record<string, 'danger' | 'warn' | 'info' | 'success'> = {
      CRITIQUE:    'danger',
      URGENTE:     'warn',
      INFORMATIVE: 'info',
      NORMAL:      'success',
    };
    return map[this.severity()?.toUpperCase() ?? ''] ?? 'success';
  });

  readonly icon = computed(() => {
    if (this.compact()) return undefined;
    const map: Record<string, string> = {
      CRITIQUE:    'pi pi-times-circle',
      URGENTE:     'pi pi-exclamation-circle',
      INFORMATIVE: 'pi pi-info-circle',
      NORMAL:      'pi pi-check-circle',
    };
    return map[this.severity()?.toUpperCase() ?? ''] ?? 'pi pi-check-circle';
  });
}
