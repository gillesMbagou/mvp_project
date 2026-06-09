import { Component, input, computed } from '@angular/core';
import { TagModule } from 'primeng/tag';

type Severity = 'CRITIQUE' | 'URGENTE' | 'INFORMATIVE' | 'NORMAL';

@Component({
  selector: 'cs-severity-badge',
  standalone: true,
  imports: [TagModule],
  template: `<p-tag [value]="label()" [severity]="severity()" [rounded]="true" />`,
})
export class SeverityBadgeComponent {
  readonly severityInput = input<Severity>('NORMAL', { alias: 'severity' });

  readonly label = computed(() => {
    const map: Record<Severity, string> = {
      CRITIQUE: 'CRITIQUE', URGENTE: 'URGENTE', INFORMATIVE: 'INFO', NORMAL: 'OK',
    };
    return map[this.severityInput()] ?? this.severityInput();
  });

  readonly severity = computed((): 'danger' | 'warn' | 'info' | 'success' => {
    const map: Record<Severity, 'danger' | 'warn' | 'info' | 'success'> = {
      CRITIQUE: 'danger', URGENTE: 'warn', INFORMATIVE: 'info', NORMAL: 'success',
    };
    return map[this.severityInput()] ?? 'info';
  });
}
