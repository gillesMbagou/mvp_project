import { Injectable, inject, signal, computed, OnDestroy } from '@angular/core';
import { httpResource, HttpClient } from '@angular/common/http';
import { tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { KeycloakAuthService } from '../auth/keycloak.service';

export interface AlertEvent {
  alertId:        string;
  patientId:      string;
  patientName:    string;
  deviceType:     string;
  severity:       'CRITIQUE' | 'URGENTE' | 'INFORMATIVE';
  message:        string;
  triggeredValue: string;
  createdAt:      string;
  statut:         'OUVERTE' | 'ACQUITTEE' | 'ESCALADEE' | 'RESOLUE';
}

export interface IoTObservation {
  observationId: string;
  patientId:     string;
  patientName:   string;
  deviceSerial:  string;
  deviceType:    string;
  loincCode:     string;
  value:         number;
  unit:          string;
  severity:      string;
  observedAt:    string;
  ts:            number;
}

/**
 * Payload réel envoyé par caresync-q-iot sur /api/stream/{alerts,observations}
 * (be.caresync.common.events.IoTObservationEvent côté backend). Ni /api/stream/alerts
 * ni /api/stream/observations n'utilisent de champ SSE "event:" nommé — tout arrive
 * en tant qu'événement générique "message", donc ce n'est PAS un AlertEvent/IoTObservation
 * prêt à l'emploi : il faut le mapper explicitement (voir toAlertEvent/toIoTObservation).
 */
interface IoTObservationPayload {
  patientId:    string;
  deviceSerial: string;
  deviceType:   string;
  loincCode:    string;
  value:        number;
  unit:         string;
  ts:           number;
  latencyMs?:   number;
  severity:     string;
}

/**
 * AlertService — combine httpResource (historique) + SSE (temps réel).
 *
 * EventSource ne supporte pas les headers HTTP → le token JWT est passé
 * en query param (?token=...) côté Gateway CareSync, qui le valide.
 *
 * Deux connexions SSE distinctes, à l'image des deux endpoints réellement
 * exposés par caresync-q-iot :
 *   /api/stream/alerts       → observations déjà filtrées CRITIQUE/URGENTE
 *   /api/stream/observations → toutes les observations (tout device, tout patient)
 * (caresync-q-alert, qui aurait dû exposer un vrai flux d'AlertEvent avec
 * alertId/message/statut, n'a pas encore de Resource REST — voir le rapport
 * d'exploration backend. En attendant, on synthétise un AlertEvent utilisable
 * à partir de l'observation brute.)
 */
@Injectable({ providedIn: 'root' })
export class AlertService implements OnDestroy {
  private readonly auth    = inject(KeycloakAuthService);
  private readonly http    = inject(HttpClient);
  private readonly baseUrl = environment.apiUrl;
  private readonly sseUrl  = environment.sseUrl;

  // ── Alertes live (SSE) ────────────────────────────────────────────────────
  readonly liveAlerts   = signal<AlertEvent[]>([]);
  readonly liveObs      = signal<IoTObservation[]>([]);
  readonly sseConnected = signal(false);

  private alertsSource: EventSource | null = null;
  private obsSource:    EventSource | null = null;

  // ── Alertes historique (httpResource) ────────────────────────────────────
  readonly alertsHistoryResource = httpResource<AlertEvent[]>(
    () => ({ url: `${this.baseUrl}/alertes`, params: { hours: '24' } })
  );

  // resource.value() lève une exception si la resource est en état 'error'
  // (au lieu de renvoyer undefined) — hasValue() est le garde-fou officiel.
  // Sans lui, une seule requête en échec (ex. caresync-q-alert indisponible)
  // fait planter TOUS les computed() en aval, y compris ceux d'autres pages.
  // ── Alertes scopées à un patient (fiche patient) ─────────────────────────
  // AlertResource.list() supporte déjà patientId en query param côté backend
  // (be.caresync.alert.resource.AlertResource) — fenêtre large (720h = 30j)
  // car une alerte OUVERTE non traitée doit rester visible au-delà de 24h.
  readonly selectedPatientId = signal<string | null>(null);

  readonly patientAlertsResource = httpResource<AlertEvent[]>(() =>
    this.selectedPatientId()
      ? { url: `${this.baseUrl}/alertes`, params: { patientId: this.selectedPatientId()!, hours: '720' } }
      : undefined
  );

  readonly patientAlerts = computed(() =>
    this.patientAlertsResource.hasValue() ? this.patientAlertsResource.value() : []
  );

  readonly patientActiveAlertsCount = computed(() =>
    this.patientAlerts().filter(a => a.statut === 'OUVERTE').length
  );

  forPatient(patientId: string): void {
    this.selectedPatientId.set(patientId);
  }

  readonly alertsHistory   = computed(() =>
    this.alertsHistoryResource.hasValue() ? this.alertsHistoryResource.value() : []
  );
  readonly openAlertsCount = computed(() =>
    this.liveAlerts().filter(a => a.statut === 'OUVERTE').length +
    this.alertsHistory().filter(a => a.statut === 'OUVERTE').length
  );

  readonly critiquesCount = computed(() =>
    this.liveAlerts().filter(a => a.severity === 'CRITIQUE').length
  );

  // ── Connexion SSE ─────────────────────────────────────────────────────────
  async connectSSE(): Promise<void> {
    if (this.alertsSource || this.obsSource) return;  // déjà connecté

    const token = await this.auth.getToken();
    const qs    = `token=${encodeURIComponent(token)}`;

    this.alertsSource = new EventSource(`${this.sseUrl}/alerts?${qs}`);
    this.alertsSource.onmessage = (event: MessageEvent) => {
      try {
        const obs: IoTObservationPayload = JSON.parse(event.data);
        this.liveAlerts.update(alerts => [toAlertEvent(obs), ...alerts].slice(0, 100));
      } catch (e) { console.error('[SSE] Erreur parsing alert', e); }
    };
    this.alertsSource.onopen  = () => this.sseConnected.set(true);
    this.alertsSource.onerror = () => {
      this.sseConnected.set(false);
      setTimeout(() => this.reconnectSSE(), 5000);
    };

    this.obsSource = new EventSource(`${this.sseUrl}/observations?${qs}`);
    this.obsSource.onmessage = (event: MessageEvent) => {
      try {
        const obs: IoTObservationPayload = JSON.parse(event.data);
        const mapped = toIoTObservation(obs);
        this.liveObs.update(current => {
          const updated = [...current];
          const idx = updated.findIndex(o => o.deviceSerial === mapped.deviceSerial);
          if (idx >= 0) updated[idx] = mapped;
          else updated.unshift(mapped);
          return updated.slice(0, 50);
        });
      } catch (e) { /* ignore payload malformé */ }
    };
  }

  // ── Acquittement / résolution ────────────────────────────────────────────
  // Endpoints planifiés (UC07_alerte.feature), pas encore de Resource REST
  // côté caresync-q-alert — la mise à jour locale du signal reste optimiste.

  acquitter(alertId: string) {
    return this.http.patch<AlertEvent>(`${this.baseUrl}/alertes/${alertId}/acquitter`, {}).pipe(
      tap(() => this.applyStatutLocal(alertId, 'ACQUITTEE'))
    );
  }

  resoudre(alertId: string) {
    return this.http.patch<AlertEvent>(`${this.baseUrl}/alertes/${alertId}/resoudre`, {}).pipe(
      tap(() => this.applyStatutLocal(alertId, 'RESOLUE'))
    );
  }

  private applyStatutLocal(alertId: string, statut: AlertEvent['statut']): void {
    this.liveAlerts.update(alerts =>
      alerts.map(a => a.alertId === alertId ? { ...a, statut } : a)
    );
  }

  private async reconnectSSE(): Promise<void> {
    this.disconnectSSE();
    await this.connectSSE();
  }

  disconnectSSE(): void {
    this.alertsSource?.close();
    this.obsSource?.close();
    this.alertsSource = null;
    this.obsSource    = null;
    this.sseConnected.set(false);
  }

  ngOnDestroy(): void {
    this.disconnectSSE();
  }
}

function toAlertEvent(obs: IoTObservationPayload): AlertEvent {
  return {
    alertId:        `${obs.deviceSerial}-${obs.ts}`,
    patientId:      obs.patientId,
    patientName:    obs.patientId,
    deviceType:     obs.deviceType,
    severity:       obs.severity as AlertEvent['severity'],
    message:        `${obs.deviceType} — valeur ${obs.value} ${obs.unit} hors seuil`,
    triggeredValue: `${obs.value} ${obs.unit}`,
    createdAt:      new Date(obs.ts).toISOString(),
    statut:         'OUVERTE',
  };
}

function toIoTObservation(obs: IoTObservationPayload): IoTObservation {
  return {
    observationId: `${obs.deviceSerial}-${obs.ts}`,
    patientId:     obs.patientId,
    patientName:   obs.patientId,
    deviceSerial:  obs.deviceSerial,
    deviceType:    obs.deviceType,
    loincCode:     obs.loincCode,
    value:         obs.value,
    unit:          obs.unit,
    severity:      obs.severity,
    observedAt:    new Date(obs.ts).toISOString(),
    ts:            obs.ts,
  };
}
