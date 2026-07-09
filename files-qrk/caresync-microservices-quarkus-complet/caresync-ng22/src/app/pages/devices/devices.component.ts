import { Component, inject, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { TagModule } from 'primeng/tag';
import { ButtonModule } from 'primeng/button';
import { DeviceService } from '../../core/services/device.service';

@Component({
  selector: 'app-devices',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TagModule, ButtonModule],
  template: `
<div style="display:flex;flex-direction:column;gap:20px">

  <div style="display:flex;align-items:center;gap:12px">
    <div style="flex:1">
      <h2 style="font-size:18px;font-weight:700;color:var(--cs-text);margin:0">Dispositifs IoT</h2>
      <p style="font-size:13px;color:var(--cs-text-3);margin:2px 0 0">
        {{ deviceSvc.devices().length }} dispositif(s) enregistré(s) ·
        @if (deviceSvc.connected()) {
          <span style="color:var(--cs-normal)">● Flux SSE actif</span>
        } @else {
          <span style="color:var(--cs-critique)">● Flux SSE déconnecté</span>
        }
      </p>
    </div>
    <button pButton icon="pi pi-refresh" class="p-button-outlined p-button-sm"
            [loading]="deviceSvc.registryResource.isLoading()"
            (click)="deviceSvc.reloadRegistry()"></button>
  </div>

  <div class="cs-card">
    @if (deviceSvc.registryResource.error()) {
      <div style="text-align:center;padding:32px;color:var(--cs-text-3)">
        <i class="pi pi-exclamation-triangle" style="font-size:28px;color:var(--cs-urgente)"></i>
        <p style="margin-top:8px;font-size:13px">Registre des dispositifs indisponible — affichage du flux temps réel uniquement.</p>
      </div>
    }
    <div style="display:grid;grid-template-columns:repeat(auto-fill,minmax(260px,1fr));gap:10px">
      @for (d of deviceSvc.devices(); track d.deviceSerial) {
        <div style="display:flex;align-items:center;gap:12px;padding:12px;border-radius:10px;
                    background:var(--cs-surface-2);border:1px solid var(--cs-border)"
             [style.opacity]="d.active ? 1 : 0.5">
          <div style="width:36px;height:36px;border-radius:8px;display:flex;align-items:center;
                      justify-content:center;flex-shrink:0;color:var(--cs-teal-contrast);background:var(--cs-teal)">
            <i class="pi pi-wifi"></i>
          </div>
          <div style="flex:1;min-width:0">
            <div style="font-size:13px;font-weight:600;color:var(--cs-text)">{{ d.deviceType }}</div>
            <div style="font-size:11px;color:var(--cs-text-3)">
              {{ d.deviceSerial }} @if (d.patientId) { · patient {{ d.patientId }} }
            </div>
          </div>
          <div style="text-align:right">
            @if (d.value != null) {
              <div style="font-size:15px;font-weight:700;color:var(--cs-text)">{{ d.value }} {{ d.unit }}</div>
              <p-tag [severity]="tagSeverity(d.severity)" [value]="d.severity ?? 'N/A'" [rounded]="true"/>
            } @else {
              <div style="font-size:11px;color:var(--cs-text-3)">Aucune mesure reçue</div>
            }
          </div>
        </div>
      }

      @if (deviceSvc.devices().length === 0) {
        <div style="text-align:center;padding:32px;color:var(--cs-text-3);grid-column:1/-1">
          <i class="pi pi-wifi" style="font-size:28px"></i>
          <p style="margin-top:8px;font-size:13px">Aucun dispositif enregistré ni détecté sur le flux temps réel</p>
        </div>
      }
    </div>
  </div>
</div>
  `,
})
export class DevicesComponent implements OnInit {
  readonly deviceSvc = inject(DeviceService);

  ngOnInit(): void {
    this.deviceSvc.connect();
  }

  tagSeverity(s: string | undefined): 'danger' | 'warn' | 'info' | 'success' {
    const m: Record<string, 'danger' | 'warn' | 'info' | 'success'> = {
      CRITIQUE: 'danger', URGENTE: 'warn', INFORMATIVE: 'info', NORMAL: 'success',
    };
    return m[s ?? ''] ?? 'info';
  }
}
