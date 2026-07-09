import { Component, inject, signal, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { MessagingService } from '../../core/services/messaging.service';
import { MessageDto } from '../../core/models';

@Component({
  selector: 'app-messaging',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ButtonModule, TagModule],
  template: `
<div style="display:flex;flex-direction:column;gap:20px">

  <div style="display:flex;align-items:center;gap:12px">
    <div style="flex:1">
      <h2 style="font-size:18px;font-weight:700;color:var(--cs-text);margin:0">Messagerie sécurisée</h2>
      <p style="font-size:13px;color:var(--cs-text-3);margin:2px 0 0">
        {{ messagingSvc.unreadCount() }} message(s) non lu(s)
      </p>
    </div>
    <button pButton icon="pi pi-refresh" class="p-button-outlined p-button-sm"
            [loading]="messagingSvc.isLoading()"
            (click)="messagingSvc.reload()"></button>
  </div>

  <div class="cs-card">
    @if (messagingSvc.isLoading()) {
      <div style="text-align:center;padding:32px;color:var(--cs-text-3)">Chargement…</div>
    } @else if (messagingSvc.messagesResource.error()) {
      <div style="text-align:center;padding:32px;color:var(--cs-text-3)">
        <i class="pi pi-exclamation-triangle" style="font-size:28px;color:var(--cs-urgente)"></i>
        <p style="margin-top:8px;font-size:13px">
          Service de messagerie indisponible (caresync-q-messaging n'expose pas encore d'API REST).
        </p>
      </div>
    } @else {
      <div style="display:flex;flex-direction:column;gap:6px">
        @for (m of messagingSvc.messages(); track m.id) {
          <div style="display:flex;gap:10px;padding:10px;border-radius:8px;background:var(--cs-surface-2)">
            <div style="flex:1;min-width:0">
              <div style="display:flex;align-items:center;gap:8px;margin-bottom:4px">
                <p-tag [severity]="priorityTag(m.priorite)" [value]="m.priorite" [rounded]="true"/>
                <span style="font-size:11px;color:var(--cs-text-3)">{{ formatTime(m.creeAt) }}</span>
              </div>
              <div style="font-size:13px;font-weight:600;color:var(--cs-text)">{{ m.objet }}</div>
              @if (m.patientRef) {
                <div style="font-size:11px;color:var(--cs-text-3)">Réf. patient {{ m.patientRef }}</div>
              }
            </div>
          </div>
        }

        @if (messagingSvc.messages().length === 0) {
          <div style="text-align:center;padding:32px;color:var(--cs-text-3)">
            <i class="pi pi-envelope" style="font-size:28px"></i>
            <p style="margin-top:8px;font-size:13px">Aucun message</p>
          </div>
        }
      </div>
    }
  </div>
</div>
  `,
})
export class MessagingComponent implements OnInit {
  readonly messagingSvc = inject(MessagingService);

  ngOnInit(): void {
    this.messagingSvc.filterByPatient(null);
  }

  priorityTag(p: MessageDto['priorite']): 'danger' | 'warn' | 'info' {
    return p === 'CRITIQUE' ? 'danger' : p === 'URGENTE' ? 'warn' : 'info';
  }

  formatTime(iso: string): string {
    return new Date(iso).toLocaleString('fr-BE', { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' });
  }
}
