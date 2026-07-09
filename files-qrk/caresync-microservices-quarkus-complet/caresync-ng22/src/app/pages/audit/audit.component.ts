import { Component, inject, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { AuditService } from '../../core/services/audit.service';

@Component({
  selector: 'app-audit',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ButtonModule, TagModule],
  template: `
<div style="display:flex;flex-direction:column;gap:20px">

  <div style="display:flex;align-items:center;gap:12px">
    <div style="flex:1">
      <h2 style="font-size:18px;font-weight:700;color:var(--cs-text);margin:0">Journal d'audit</h2>
      <p style="font-size:13px;color:var(--cs-text-3);margin:2px 0 0">
        Traçabilité HDS · immuable · rétention 2 ans
      </p>
    </div>
    <button pButton icon="pi pi-refresh" class="p-button-outlined p-button-sm"
            [loading]="auditSvc.isLoading()" (click)="auditSvc.reload()"></button>
  </div>

  <div class="cs-card">
    @if (auditSvc.error()) {
      <div style="text-align:center;padding:32px;color:var(--cs-text-3)">
        <i class="pi pi-exclamation-triangle" style="font-size:28px;color:var(--cs-urgente)"></i>
        <p style="margin-top:8px;font-size:13px">
          Service d'audit indisponible (caresync-q-audit n'expose pas encore d'API REST).
        </p>
      </div>
    } @else {
      <div style="display:flex;flex-direction:column;gap:4px">
        @for (log of auditSvc.logs(); track log.eventId) {
          <div style="display:flex;align-items:center;gap:10px;padding:8px;border-radius:6px;background:var(--cs-surface-2)">
            <p-tag [severity]="log.success ? 'success' : 'danger'" [value]="log.success ? 'OK' : 'ÉCHEC'" [rounded]="true"/>
            <span style="font-size:12px;font-weight:600;color:var(--cs-text)">{{ log.action }}</span>
            <span style="font-size:11px;color:var(--cs-text-3)">{{ log.actorRole }} · {{ log.actorId }}</span>
            <span style="font-size:11px;color:var(--cs-text-3);margin-left:auto">{{ formatTime(log.timestamp) }}</span>
          </div>
        }
        @if (auditSvc.logs().length === 0) {
          <div style="text-align:center;padding:32px;color:var(--cs-text-3);font-size:13px">Aucun événement</div>
        }
      </div>
    }
  </div>
</div>
  `,
})
export class AuditComponent implements OnInit {
  readonly auditSvc = inject(AuditService);

  ngOnInit(): void {
    this.auditSvc.reload();
  }

  formatTime(iso: string): string {
    return new Date(iso).toLocaleString('fr-BE');
  }
}
