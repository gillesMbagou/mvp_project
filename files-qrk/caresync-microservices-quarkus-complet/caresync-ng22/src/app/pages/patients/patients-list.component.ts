import {
  Component, inject, signal, ChangeDetectionStrategy, OnInit, DestroyRef
} from '@angular/core';
import { Router } from '@angular/router';
import { FormControl, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { TableModule } from 'primeng/table';
import { InputTextModule } from 'primeng/inputtext';
import { ButtonModule } from 'primeng/button';
import { SkeletonModule } from 'primeng/skeleton';
import { PaginatorModule } from 'primeng/paginator';
import { TooltipModule } from 'primeng/tooltip';

import { PatientService, PatientDto } from '../../core/services/patient.service';

/**
 * PatientsListComponent — Signal Form + httpResource.
 *
 * Signal Form pattern :
 *   FormControl → toSignal(valueChanges.pipe(debounce)) → signal réactif
 *   Ce signal est consommé par PatientService.searchQuery
 *   qui déclenche automatiquement un nouveau fetch httpResource.
 *
 * Pas de subscribe() visible dans ce composant.
 */
@Component({
  selector: 'app-patients-list',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, ReactiveFormsModule, TableModule, InputTextModule,
            ButtonModule, SkeletonModule, PaginatorModule, TooltipModule],
  template: `
<div class="cs-card">

  <!-- Header avec recherche -->
  <div style="display:flex;align-items:center;gap:12px;margin-bottom:20px;flex-wrap:wrap">
    <div style="flex:1;min-width:220px">
      <h2 style="font-size:18px;font-weight:700;color:var(--cs-text);margin:0">Patients</h2>
      <p style="font-size:13px;color:var(--cs-text-3);margin:2px 0 0">
        {{ patientSvc.totalElements() }} patients · {{ patientSvc.patients().length }} affichés
      </p>
    </div>

    <!-- Signal Form Search — debounce 350ms, distinctUntilChanged -->
    <div style="position:relative;flex:1;max-width:320px">
      <i class="pi pi-search" style="position:absolute;left:12px;top:50%;transform:translateY(-50%);
                                      color:var(--cs-text-3);font-size:13px;pointer-events:none"></i>
      <input pInputText
             [formControl]="searchControl"
             placeholder="Rechercher par nom, pathologie..."
             style="width:100%;padding-left:36px;padding-right:36px"
             class="p-inputtext-sm"/>
      @if (searchControl.value) {
        <i class="pi pi-times"
           style="position:absolute;right:12px;top:50%;transform:translateY(-50%);
                  color:var(--cs-text-3);font-size:12px;cursor:pointer"
           (click)="clearSearch()"></i>
      }
    </div>

    <!-- Filtre pathologie -->
    <select [(ngModel)]="selectedPathology" (ngModelChange)="filterByPathology($event)"
            style="padding:7px 12px;border:1px solid var(--cs-border);border-radius:8px;
                   background:var(--cs-surface);color:var(--cs-text);font-size:13px;cursor:pointer">
      <option value="">Toutes pathologies</option>
      <option value="DIABETE_T2">Diabète T2</option>
      <option value="ICFE">Insuffisance cardiaque</option>
      <option value="BPCO">BPCO</option>
    </select>

    <button pButton icon="pi pi-refresh" class="p-button-outlined p-button-sm"
            (click)="patientSvc.reload()" [loading]="patientSvc.isLoading()"
            pTooltip="Rafraîchir" tooltipPosition="top"></button>
  </div>

  <!-- Loading skeletons -->
  @if (patientSvc.isLoading()) {
    <div style="display:flex;flex-direction:column;gap:8px">
      @for (_ of [1,2,3,4,5]; track $index) {
        <p-skeleton height="52px" borderRadius="8px"/>
      }
    </div>
  } @else {

    <!-- Table PrimeNG -->
    <p-table
      [value]="patientSvc.patients()"
      [rowHover]="true"
      (onRowSelect)="openPatient($any($event.data))"
      selectionMode="single"
      styleClass="p-datatable-sm"
      [tableStyle]="{'min-width':'640px'}">

      <ng-template pTemplate="header">
        <tr>
          <th style="width:44px"></th>
          <th pSortableColumn="lastName">Patient <p-sortIcon field="lastName"/></th>
          <th>Pathologie</th>
          <th>Service</th>
          <th pSortableColumn="riskLevel">Risque <p-sortIcon field="riskLevel"/></th>
          <th>Alertes</th>
          <th>Dernière mesure</th>
          <th style="width:48px"></th>
        </tr>
      </ng-template>

      <ng-template pTemplate="body" let-patient>
        <tr [pSelectableRow]="patient" style="cursor:pointer"
            (click)="openPatient(patient)">

          <!-- Avatar initial -->
          <td>
            <div style="width:32px;height:32px;border-radius:50%;display:flex;align-items:center;
                        justify-content:center;font-size:11px;font-weight:700;color:#fff"
                 [style.background]="riskColor(patient.riskLevel)">
              {{ patient.firstName[0] }}{{ patient.lastName[0] }}
            </div>
          </td>

          <!-- Nom + prénom -->
          <td>
            <div style="font-weight:600;color:var(--cs-text);font-size:13px">
              {{ patient.lastName }}, {{ patient.firstName }}
            </div>
            <div style="font-size:11px;color:var(--cs-text-3)">
              {{ patient.age }} ans · {{ patient.gender }}
            </div>
          </td>

          <!-- Pathologie -->
          <td>
            <span style="font-size:12px;padding:3px 8px;border-radius:6px;
                         background:var(--cs-teal-light);color:var(--cs-teal-dark);font-weight:500">
              {{ patient.pathology }}
            </span>
          </td>

          <td>
            <span style="font-size:12px;color:var(--cs-text-2)">{{ patient.service ?? '—' }}</span>
          </td>

          <!-- Risk level badge -->
          <td>
            <span class="cs-badge cs-badge--{{ patient.riskLevel }}">
              {{ patient.riskLevel }}
            </span>
          </td>

          <!-- Alertes count -->
          <td>
            @if (patient.activeAlerts > 0) {
              <span style="font-size:12px;background:var(--cs-critique-bg);
                           color:var(--cs-critique);padding:2px 8px;border-radius:6px;font-weight:600">
                {{ patient.activeAlerts }}
              </span>
            } @else {
              <span style="font-size:12px;color:var(--cs-text-3)">—</span>
            }
          </td>

          <!-- Dernière mesure -->
          <td>
            @if (patient.lastMeasure) {
              <div style="font-size:12px;font-weight:600;color:var(--cs-text)">
                {{ patient.lastMeasure }}
              </div>
              <div style="font-size:10px;color:var(--cs-text-3)">
                {{ patient.lastMeasureAt }}
              </div>
            } @else {
              <span style="font-size:12px;color:var(--cs-text-3)">—</span>
            }
          </td>

          <td>
            <i class="pi pi-chevron-right" style="font-size:12px;color:var(--cs-text-3)"></i>
          </td>
        </tr>
      </ng-template>

      <ng-template pTemplate="emptymessage">
        <tr>
          <td colspan="8" style="text-align:center;padding:40px;color:var(--cs-text-3)">
            <i class="pi pi-search" style="font-size:28px;display:block;margin-bottom:8px"></i>
            Aucun patient trouvé pour "{{ patientSvc.searchQuery() }}"
          </td>
        </tr>
      </ng-template>
    </p-table>

    <!-- Pagination -->
    <p-paginator
      [rows]="patientSvc.pageSize()"
      [totalRecords]="patientSvc.totalElements()"
      [first]="patientSvc.currentPage() * patientSvc.pageSize()"
      (onPageChange)="onPageChange($event)"
      [rowsPerPageOptions]="[10, 20, 50]"
      styleClass="mt-3"/>
  }
</div>
  `,
})
export class PatientsListComponent implements OnInit {
  readonly patientSvc     = inject(PatientService);
  private  readonly router = inject(Router);
  private  readonly destroyRef = inject(DestroyRef);

  selectedPathology = '';

  // Signal Form pattern — le signal suit les changements du FormControl avec debounce
  readonly searchControl = new FormControl(this.patientSvc.searchQuery());

  // toSignal() convertit l'Observable valueChanges en Signal réactif
  private readonly debouncedSearch = toSignal(
    this.searchControl.valueChanges.pipe(
      debounceTime(350),
      distinctUntilChanged()
    ),
    { initialValue: this.patientSvc.searchQuery() }
  );

  ngOnInit(): void {
    // Brancher le signal debounced sur le service → déclenche httpResource
    // On utilise effect() implicitement via takeUntilDestroyed
    this.searchControl.valueChanges.pipe(
      debounceTime(350),
      distinctUntilChanged(),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(q => this.patientSvc.search(q ?? ''));
  }

  openPatient(patient: PatientDto): void {
    this.router.navigate(['/patients', patient.id]);
  }

  clearSearch(): void {
    this.searchControl.setValue('');
    this.patientSvc.search('');
  }

  filterByPathology(pathology: string): void {
    this.patientSvc.search(pathology);
  }

  onPageChange(event: { page?: number; rows?: number }): void {
    this.patientSvc.goToPage(event.page ?? 0);
  }

  riskColor(r: string): string {
    const m: Record<string,string> = {
      critique:'#EF4444', urgente:'#F59E0B',
      informative:'#3B82F6', normal:'#10B981'
    };
    return m[r] ?? '#64748B';
  }
}
