import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'severityLabel', standalone: true })
export class SeverityLabelPipe implements PipeTransform {
  transform(value: string): string {
    const map: Record<string, string> = {
      CRITIQUE: '🔴 Critique',
      URGENTE: '🟠 Urgente',
      INFORMATIVE: '🔵 Informative',
      NORMAL: '🟢 Normal',
    };
    return map[value] ?? value;
  }
}
