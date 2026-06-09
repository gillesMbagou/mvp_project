import { Component, OnInit, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { PatientService } from '../../core/services/patient.service';
import { PatientProfile } from '../../core/models/patient.model';
import { TableModule } from 'primeng/table';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { SkeletonModule } from 'primeng/skeleton';
import { InputTextModule } from 'primeng/inputtext';
import { TagModule } from 'primeng/tag';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'cs-patients',
  standalone: true,
  imports: [TableModule, CardModule, ButtonModule, SkeletonModule, InputTextModule, TagModule, FormsModule],
  template: `
    <div class="cs-page">
      <div class="cs-page__header">
        <h2 class="cs-page__title">Patients</h2>
        <div class="cs-page__search">
          <span class="p-input-icon-left">
            <i class="pi pi-search"></i>
            <input pInputText [(ngModel)]="searchTerm" placeholder="Rechercher…" />
          </span>
        </div>
      </div>

      @if (loading()) {
        <p-skeleton height="400px" borderRadius="12px" />
      } @else {
        <p-card>
          <p-table
            [value]="filteredPatients()"
            [paginator]="true"
            [rows]="15"
            [rowHover]="true"
            styleClass="p-datatable-sm"
            responsiveLayout="scroll"
          >
            <ng-template pTemplate="header">
              <tr>
                <th pSortableColumn="lastName">Nom <p-sortIcon field="lastName" /></th>
                <th pSortableColumn="firstName">Prénom <p-sortIcon field="firstName" /></th>
                <th>INS-NIR</th>
                <th pSortableColumn="gender">Genre <p-sortIcon field="gender" /></th>
                <th>Date de naissance</th>
                <th>Diagnostic</th>
                <th></th>
              </tr>
            </ng-template>
            <ng-template pTemplate="body" let-patient>
              <tr>
                <td><strong>{{ patient.lastName }}</strong></td>
                <td>{{ patient.firstName }}</td>
                <td><code>{{ patient.insNir ?? '—' }}</code></td>
                <td>
                  <p-tag
                    [value]="patient.gender"
                    [severity]="patient.gender === 'F' ? 'info' : 'secondary'"
                  />
                </td>
                <td>{{ patient.birthDate }}</td>
                <td>{{ patient.primaryDiagnosis ?? '—' }}</td>
                <td>
                  <p-button
                    icon="pi pi-eye"
                    severity="secondary"
                    [text]="true"
                    size="small"
                    (onClick)="view(patient.patientId)"
                    pTooltip="Voir les observations"
                  />
                </td>
              </tr>
            </ng-template>
            <ng-template pTemplate="emptymessage">
              <tr><td colspan="7" class="text-center p-4">Aucun patient trouvé</td></tr>
            </ng-template>
          </p-table>
        </p-card>
      }
    </div>
  `,
  styles: [`
    .cs-page { display: flex; flex-direction: column; gap: 1.25rem; }
    .cs-page__header { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 0.75rem; }
    .cs-page__title { margin: 0; font-size: 1.25rem; font-weight: 700; }
  `],
})
export class PatientsComponent implements OnInit {
  private patientService = inject(PatientService);
  private router = inject(Router);

  readonly loading = signal(true);
  readonly patients = signal<PatientProfile[]>([]);
  searchTerm = '';

  filteredPatients(): PatientProfile[] {
    const q = this.searchTerm.toLowerCase();
    return q
      ? this.patients().filter(p =>
          `${p.firstName} ${p.lastName}`.toLowerCase().includes(q) ||
          p.patientId.toLowerCase().includes(q),
        )
      : this.patients();
  }

  ngOnInit(): void {
    this.patientService.getPatients().subscribe({
      next: list => { this.patients.set(list); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  view(id: string): void {
    this.router.navigate(['/patients', id]);
  }
}
