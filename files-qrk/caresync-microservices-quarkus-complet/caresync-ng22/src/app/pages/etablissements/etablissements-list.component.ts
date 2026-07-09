import { Component, inject, ChangeDetectionStrategy } from '@angular/core';
import { Router } from '@angular/router';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ButtonModule } from 'primeng/button';
import { SkeletonModule } from 'primeng/skeleton';
import { EtablissementService } from '../../core/services/etablissement.service';
import { EtablissementDto } from '../../core/models';

@Component({
  selector: 'app-etablissements-list',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TableModule, TagModule, ButtonModule, SkeletonModule],
  template: `
<div class="cs-card">
  <div style="display:flex;align-items:center;gap:12px;margin-bottom:16px">
    <div style="flex:1">
      <h2 style="font-size:18px;font-weight:700;color:var(--cs-text);margin:0">Établissements</h2>
      <p style="font-size:13px;color:var(--cs-text-3);margin:2px 0 0">
        {{ etabSvc.totalElements() }} établissement(s)
      </p>
    </div>
    <button pButton icon="pi pi-refresh" class="p-button-outlined p-button-sm"
            [loading]="etabSvc.isLoading()" (click)="etabSvc.reload()"></button>
  </div>

  @if (etabSvc.isLoading()) {
    <div style="display:flex;flex-direction:column;gap:8px">
      @for (_ of [1,2,3]; track $index) { <p-skeleton height="48px" borderRadius="8px"/> }
    </div>
  } @else if (etabSvc.etablissementsResource.error()) {
    <div style="text-align:center;padding:32px;color:var(--cs-text-3)">
      <i class="pi pi-exclamation-triangle" style="font-size:28px;color:var(--cs-urgente)"></i>
      <p style="margin-top:8px;font-size:13px">
        Impossible de charger les établissements (accès réservé super_admin / admin_etablissement,
        ou service caresync-q-etablissement indisponible).
      </p>
    </div>
  } @else {
    <p-table [value]="etabSvc.etablissements()" [rowHover]="true" styleClass="p-datatable-sm"
             selectionMode="single" (onRowSelect)="open($any($event.data))">
      <ng-template pTemplate="header">
        <tr>
          <th>Nom</th><th>Type</th><th>Ville</th><th>FINESS</th><th>Statut</th>
        </tr>
      </ng-template>
      <ng-template pTemplate="body" let-etab>
        <tr [pSelectableRow]="etab" style="cursor:pointer">
          <td style="font-weight:600;color:var(--cs-text)">{{ etab.nom }}</td>
          <td>{{ etab.type }}</td>
          <td>{{ etab.ville ?? '—' }}</td>
          <td>{{ etab.finess }}</td>
          <td><p-tag [severity]="statutTag(etab.statut)" [value]="etab.statut" [rounded]="true"/></td>
        </tr>
      </ng-template>
      <ng-template pTemplate="emptymessage">
        <tr><td colspan="5" style="text-align:center;padding:32px;color:var(--cs-text-3)">Aucun établissement</td></tr>
      </ng-template>
    </p-table>
  }
</div>
  `,
})
export class EtablissementsListComponent {
  readonly etabSvc = inject(EtablissementService);
  private readonly router = inject(Router);

  open(etab: EtablissementDto): void {
    this.router.navigate(['/etablissements', etab.id]);
  }

  statutTag(s: EtablissementDto['statut']): 'success' | 'danger' | 'warn' {
    return s === 'ACTIF' ? 'success' : s === 'SUSPENDU' ? 'warn' : 'danger';
  }
}
