import { Component, OnInit, inject, signal } from '@angular/core';
import { DeviceService } from '../../core/services/device.service';
import { SimulatedDevice, DeviceScenario } from '../../core/models/device.model';
import { CardModule } from 'primeng/card';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { SkeletonModule } from 'primeng/skeleton';
import { TagModule } from 'primeng/tag';
import { SelectButtonModule } from 'primeng/selectbutton';
import { TooltipModule } from 'primeng/tooltip';
import { FormsModule } from '@angular/forms';
import { DatePipe } from '@angular/common';

const SCENARIOS: { label: string; value: DeviceScenario }[] = [
  { label: 'Normal', value: 'NORMAL' },
  { label: 'Hypoglycémie', value: 'HYPOGLYCEMIA' },
  { label: 'Hyperglycémie', value: 'HYPERGLYCEMIA' },
  { label: 'Critique', value: 'CRITICAL' },
];

@Component({
  selector: 'cs-devices',
  standalone: true,
  imports: [CardModule, TableModule, ButtonModule, SkeletonModule, TagModule, SelectButtonModule, TooltipModule, FormsModule, DatePipe],
  template: `
    <div class="cs-page">
      <div class="cs-page__header">
        <h2 class="cs-page__title">Dispositifs IoT simulés</h2>
        <p-button icon="pi pi-refresh" label="Actualiser" severity="secondary" (onClick)="load()" />
      </div>

      @if (loading()) {
        <p-skeleton height="400px" borderRadius="12px" />
      } @else {
        <p-card>
          <p-table [value]="devices()" styleClass="p-datatable-sm" responsiveLayout="scroll">
            <ng-template pTemplate="header">
              <tr>
                <th>Série</th>
                <th>Patient</th>
                <th>Type</th>
                <th>Protocole</th>
                <th>Statut</th>
                <th>Scénario actuel</th>
                <th>Changer scénario</th>
                <th>Dernière obs.</th>
              </tr>
            </ng-template>
            <ng-template pTemplate="body" let-dev>
              <tr>
                <td><code>{{ dev.serial }}</code></td>
                <td>{{ dev.patientId }}</td>
                <td>{{ dev.deviceType }}</td>
                <td><p-tag [value]="dev.protocol" severity="secondary" /></td>
                <td>
                  <p-tag
                    [value]="dev.active ? 'Actif' : 'Inactif'"
                    [severity]="dev.active ? 'success' : 'danger'"
                  />
                </td>
                <td>
                  <p-tag
                    [value]="dev.currentScenario"
                    [severity]="scenarioSeverity(dev.currentScenario)"
                  />
                </td>
                <td>
                  <p-selectButton
                    [options]="scenarios"
                    [(ngModel)]="selectedScenario[dev.serial]"
                    optionLabel="label"
                    optionValue="value"
                    (onChange)="applyScenario(dev.serial, $event.value)"
                    styleClass="cs-scenario-btn"
                  />
                </td>
                <td>{{ dev.lastObservationAt | date:'HH:mm:ss' }}</td>
              </tr>
            </ng-template>
          </p-table>
        </p-card>
      }
    </div>
  `,
  styles: [`
    .cs-page { display: flex; flex-direction: column; gap: 1.25rem; }
    .cs-page__header { display: flex; align-items: center; justify-content: space-between; }
    .cs-page__title { margin: 0; font-size: 1.25rem; font-weight: 700; }
    :host ::ng-deep .cs-scenario-btn .p-button { padding: 0.25rem 0.5rem; font-size: 0.75rem; }
  `],
})
export class DevicesComponent implements OnInit {
  private deviceService = inject(DeviceService);

  readonly loading = signal(true);
  readonly devices = signal<SimulatedDevice[]>([]);
  readonly scenarios = SCENARIOS;
  selectedScenario: Record<string, DeviceScenario> = {};

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.deviceService.getDevices().subscribe({
      next: list => {
        this.devices.set(list);
        list.forEach(d => (this.selectedScenario[d.serial] = d.currentScenario));
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  applyScenario(serial: string, scenario: DeviceScenario): void {
    this.deviceService.setScenario(serial, scenario).subscribe(() => {
      this.devices.update(list =>
        list.map(d => d.serial === serial ? { ...d, currentScenario: scenario } : d),
      );
    });
  }

  scenarioSeverity(s: DeviceScenario): 'success' | 'warn' | 'danger' | 'secondary' {
    const map: Record<DeviceScenario, 'success' | 'warn' | 'danger' | 'secondary'> = {
      NORMAL: 'success', HYPOGLYCEMIA: 'warn', HYPERGLYCEMIA: 'warn', CRITICAL: 'danger',
    };
    return map[s] ?? 'secondary';
  }
}
