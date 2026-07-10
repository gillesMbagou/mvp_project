import {
  Component, inject, input, effect, computed, ChangeDetectionStrategy
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { ButtonModule } from 'primeng/button';
import { SkeletonModule } from 'primeng/skeleton';
import { TagModule } from 'primeng/tag';
import { TabsModule } from 'primeng/tabs';

import { PatientService } from '../../core/services/patient.service';
import { CarePlanService } from '../../core/services/careplan.service';
import { PrescriptionService } from '../../core/services/prescription.service';
import { DossierService } from '../../core/services/dossier.service';
import { AlertService } from '../../core/services/alert.service';
import { DeviceService } from '../../core/services/device.service';

/**
 * PatientDetailComponent — fiche patient.
 *
 * L'id vient du paramètre de route ':id', injecté automatiquement
 * comme input signal grâce à withComponentInputBinding() (app.config.ts).
 *
 * Regroupe les vues patient-scopées de plusieurs microservices :
 *   - PatientService     → /api/v1/patients (identité, KPI)
 *   - DossierService     → /api/v1/dossiers (vue consolidée, planifié backend)
 *   - CarePlanService    → /api/v1/care-plans (plans de soins)
 *   - PrescriptionService→ /api/v1/prescriptions (ordonnances, planifié backend)
 */
@Component({
  selector: 'app-patient-detail',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, DatePipe, ButtonModule, SkeletonModule, TagModule, TabsModule],
  template: `
<div style="display:flex;flex-direction:column;gap:20px">

  <div style="display:flex;align-items:center;gap:12px">
    <a routerLink="/patients">
      <button pButton icon="pi pi-arrow-left" class="p-button-text p-button-sm"
              label="Retour aux patients"></button>
    </a>
  </div>

  @if (patientSvc.detailLoading()) {
    <div class="cs-card">
      <p-skeleton height="120px" borderRadius="12px"/>
    </div>
  } @else if (patientSvc.selectedPatient(); as patient) {

    <div class="cs-card" style="display:flex;align-items:center;gap:16px">
      <div style="width:56px;height:56px;border-radius:50%;display:flex;align-items:center;
                  justify-content:center;font-size:18px;font-weight:700;color:#fff;flex-shrink:0"
           [style.background]="riskColor(patient.riskLevel)">
        {{ patient.firstName[0] }}{{ patient.lastName[0] }}
      </div>
      <div style="flex:1;min-width:0">
        <h2 style="font-size:20px;font-weight:700;color:var(--cs-text);margin:0">
          {{ patient.firstName }} {{ patient.lastName }}
        </h2>
        <p style="font-size:13px;color:var(--cs-text-3);margin:4px 0 0">
          {{ patient.age }} ans · {{ patient.gender }} · {{ patient.pathology }}
          @if (patient.service) { · {{ patient.service }} }
        </p>
      </div>
      <span class="cs-badge cs-badge--{{ patient.riskLevel }}">{{ patient.riskLevel }}</span>
    </div>

    <div style="display:grid;grid-template-columns:repeat(auto-fit,minmax(180px,1fr));gap:14px">
      <div class="cs-kpi cs-kpi--critique">
        <i class="pi pi-bell kpi-icon"></i>
        <div class="kpi-label">Alertes actives</div>
        <div class="kpi-value">{{ activeAlertsCount() }}</div>
      </div>
      <div class="cs-kpi cs-kpi--info">
        <i class="pi pi-wifi kpi-icon"></i>
        <div class="kpi-label">Dispositifs connectés</div>
        <div class="kpi-value">{{ patientDevices().length }}</div>
      </div>
      <div class="cs-kpi cs-kpi--normal">
        <i class="pi pi-heart kpi-icon"></i>
        <div class="kpi-label">Dernière mesure</div>
        @if (lastMeasure(); as m) {
          <div class="kpi-value" style="font-size:20px">{{ m.value }} {{ m.unit }}</div>
          <div class="kpi-delta">{{ m.deviceType }} · {{ m.observedAt | date:'short' }}</div>
        } @else {
          <div class="kpi-value" style="font-size:20px">—</div>
        }
      </div>
    </div>

    <p-tabs value="dossier">
      <p-tablist>
        <p-tab value="dossier">Dossier complet</p-tab>
        <p-tab value="careplans">Plans de soins</p-tab>
        <p-tab value="prescriptions">Prescriptions</p-tab>
      </p-tablist>
      <p-tabpanels>

        <p-tabpanel value="dossier">
          @if (dossierSvc.isLoading()) {
            <p-skeleton height="80px" borderRadius="8px"/>
          } @else if (dossierSvc.error()) {
            <div style="text-align:center;padding:24px;color:var(--cs-text-3);font-size:13px">
              Dossier consolidé indisponible (caresync-q-dossier n'expose pas encore d'API REST).
            </div>
          } @else if (dossierSvc.dossier(); as d) {
            <div style="display:flex;flex-direction:column;gap:8px">
              <div>{{ d.conditions.length }} condition(s) · {{ d.carePlans.length }} plan(s) de soins ·
                   {{ d.prescriptions.length }} prescription(s) · {{ d.alertes.length }} alerte(s)</div>
            </div>
          }
        </p-tabpanel>

        <p-tabpanel value="careplans">
          @if (carePlanSvc.isLoading()) {
            <p-skeleton height="80px" borderRadius="8px"/>
          } @else if (carePlanSvc.carePlans().length === 0) {
            <div style="text-align:center;padding:24px;color:var(--cs-text-3);font-size:13px">
              Aucun plan de soins actif
            </div>
          } @else {
            <div style="display:flex;flex-direction:column;gap:6px">
              @for (plan of carePlanSvc.carePlans(); track plan.id) {
                <div style="display:flex;align-items:center;gap:10px;padding:9px;border-radius:8px;background:var(--cs-surface-2)">
                  <div style="flex:1;min-width:0">
                    <div style="font-size:13px;font-weight:600;color:var(--cs-text)">{{ plan.typePathologie }}</div>
                    @if (plan.objectif) { <div style="font-size:11px;color:var(--cs-text-3)">{{ plan.objectif }}</div> }
                  </div>
                  <p-tag [severity]="plan.statut === 'ACTIF' ? 'success' : 'warn'" [value]="plan.statut" [rounded]="true"/>
                </div>
              }
            </div>
          }
        </p-tabpanel>

        <p-tabpanel value="prescriptions">
          @if (prescriptionSvc.isLoading()) {
            <p-skeleton height="80px" borderRadius="8px"/>
          } @else if (prescriptionSvc.prescriptionsResource.error()) {
            <div style="text-align:center;padding:24px;color:var(--cs-text-3);font-size:13px">
              Prescriptions indisponibles (caresync-q-prescription n'expose pas encore d'API REST).
            </div>
          } @else if (prescriptionSvc.prescriptions().length === 0) {
            <div style="text-align:center;padding:24px;color:var(--cs-text-3);font-size:13px">
              Aucune prescription
            </div>
          } @else {
            <div style="display:flex;flex-direction:column;gap:6px">
              @for (rx of prescriptionSvc.prescriptions(); track rx.id) {
                <div style="display:flex;align-items:center;gap:10px;padding:9px;border-radius:8px;background:var(--cs-surface-2)">
                  <div style="flex:1;min-width:0">
                    <div style="font-size:13px;font-weight:600;color:var(--cs-text)">{{ rx.lignes.length }} ligne(s)</div>
                    <div style="font-size:11px;color:var(--cs-text-3)">Émise le {{ rx.dateEmission }}</div>
                  </div>
                  <p-tag [severity]="rx.statut === 'SIGNEE' ? 'success' : 'info'" [value]="rx.statut" [rounded]="true"/>
                </div>
              }
            </div>
          }
        </p-tabpanel>

      </p-tabpanels>
    </p-tabs>

  } @else {
    <div class="cs-card" style="text-align:center;padding:40px;color:var(--cs-text-3)">
      <i class="pi pi-user" style="font-size:28px;display:block;margin-bottom:8px"></i>
      Patient introuvable
    </div>
  }
</div>
  `,
})
export class PatientDetailComponent {
  readonly patientSvc      = inject(PatientService);
  readonly carePlanSvc     = inject(CarePlanService);
  readonly prescriptionSvc = inject(PrescriptionService);
  readonly dossierSvc      = inject(DossierService);
  readonly alertSvc        = inject(AlertService);
  readonly deviceSvc       = inject(DeviceService);

  // Bindé automatiquement depuis le paramètre de route ':id'
  readonly id = input<string>('');

  // Les cartes KPI (Alertes actives / Dispositifs connectés / Dernière mesure)
  // lisaient patient.activeAlerts/connectedDevices/lastMeasure — des champs
  // qui n'existent pas sur l'entité Patient backend (patient-svc ne les
  // calcule pas). Recalculées ici côté frontend à partir des services dédiés
  // (AlertResource supporte déjà ?patientId=, DeviceService.devices() couvre
  // le registre iot-devices) plutôt que d'attendre un futur champ agrégé.
  readonly activeAlertsCount = this.alertSvc.patientActiveAlertsCount;

  readonly patientDevices = computed(() =>
    this.deviceSvc.devices().filter(d => d.patientId === this.id())
  );

  readonly lastMeasure = computed(() => {
    const withMeasure = this.patientDevices().filter(d => d.observedAt && d.value !== undefined);
    if (withMeasure.length === 0) return null;
    return withMeasure.reduce((latest, d) => (d.observedAt! > latest.observedAt! ? d : latest));
  });

  constructor() {
    effect(() => {
      const id = this.id();
      if (!id) return;
      this.patientSvc.selectPatient(id);
      this.carePlanSvc.forPatient(id);
      this.prescriptionSvc.forPatient(id);
      this.dossierSvc.open(id);
      this.alertSvc.forPatient(id);
    });
  }

  riskColor(r?: string): string {
    const m: Record<string, string> = {
      critique: '#EF4444', urgente: '#F59E0B',
      informative: '#3B82F6', normal: '#10B981'
    };
    return m[r ?? ''] ?? '#64748B';
  }
}
