import { Injectable, signal, computed, inject, OnDestroy } from '@angular/core';
import { httpResource } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { KeycloakAuthService } from '../auth/keycloak.service';

/** Payload réel de be.caresync.common.events.IoTObservationEvent (voir alert.service.ts). */
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

/** Réponse réelle de GET /api/v1/iot-devices (be.caresync.iot.entity.IoTDevice). */
interface IoTDeviceRegistryEntry {
  serial:          string;
  patientId?:      string;
  deviceType:      string;
  manufacturer?:   string;
  model?:          string;
  loincCode?:      string;
  unit?:           string;
  active:          boolean;
  currentScenario?:string;
  registeredAt?:   string;
  lastObservedAt?: string;
  lastValue?:      number;
}

/** Vue fusionnée registre + dernière mesure live, consommée par DevicesComponent. */
export interface DeviceView {
  deviceSerial: string;
  patientId?:   string;
  deviceType:   string;
  manufacturer?:string;
  model?:       string;
  active:       boolean;
  value?:       number;
  unit?:        string;
  severity?:    string;
  observedAt?:  string;
}

/**
 * DeviceService — combine le registre REST (GET /api/v1/iot-devices,
 * caresync-q-iot) et le flux SSE (/api/stream/observations) pour la page
 * Dispositifs IoT.
 *
 * Avant l'ajout d'IoTDeviceResource côté backend, cette liste ne pouvait
 * montrer QUE les dispositifs ayant émis une mesure dans la dernière minute
 * via SSE — un dispositif enregistré mais silencieux (ou la page ouverte
 * avant toute télémétrie) restait invisible. Le registre REST donne une base
 * stable ; le flux SSE la met à jour en direct par-dessus (valeur/sévérité
 * les plus récentes) sans attendre un rechargement de page.
 *
 * Le flux SSE n'envoie pas de champ "event:" nommé (tout arrive en "message"
 * générique) — onmessage() + mapping explicite, pas addEventListener('observation').
 */
@Injectable({ providedIn: 'root' })
export class DeviceService implements OnDestroy {
  private readonly auth   = inject(KeycloakAuthService);
  private readonly sseUrl = environment.sseUrl;

  private eventSource: EventSource | null = null;

  readonly connected = signal(false);
  private readonly liveBySerial = signal<Map<string, IoTObservationPayload>>(new Map());

  readonly registryResource = httpResource<IoTDeviceRegistryEntry[]>(
    () => ({ url: `${environment.apiUrl}/iot-devices` })
  );

  // .value() lève si la resource est en état 'error' — hasValue() garde-fou.
  private readonly registry = computed(() =>
    this.registryResource.hasValue() ? this.registryResource.value() : []
  );

  readonly devices = computed<DeviceView[]>(() => {
    const live = this.liveBySerial();
    const seen = new Set<string>();

    const fromRegistry = this.registry().map(d => {
      seen.add(d.serial);
      const overlay = live.get(d.serial);
      return {
        deviceSerial: d.serial,
        patientId:    overlay?.patientId ?? d.patientId,
        deviceType:   d.deviceType,
        manufacturer: d.manufacturer,
        model:        d.model,
        active:       d.active,
        value:        overlay?.value ?? d.lastValue,
        unit:         overlay?.unit ?? d.unit,
        severity:     overlay?.severity,
        observedAt:   overlay ? new Date(overlay.ts).toISOString() : d.lastObservedAt,
      } satisfies DeviceView;
    });

    // Mesures live d'un device pas (encore) présent dans le registre REST
    // (ex: registre en erreur, ou latence de réplication) — ne pas les perdre.
    const fromLiveOnly = Array.from(live.values())
      .filter(o => !seen.has(o.deviceSerial))
      .map((o): DeviceView => ({
        deviceSerial: o.deviceSerial,
        patientId:    o.patientId,
        deviceType:   o.deviceType,
        active:       true,
        value:        o.value,
        unit:         o.unit,
        severity:     o.severity,
        observedAt:   new Date(o.ts).toISOString(),
      }));

    return [...fromRegistry, ...fromLiveOnly]
      .sort((a, b) => (b.observedAt ?? '').localeCompare(a.observedAt ?? ''));
  });

  async connect(): Promise<void> {
    if (this.eventSource) return;

    const token = await this.auth.getToken();
    const url   = `${this.sseUrl}/observations?token=${encodeURIComponent(token)}`;
    this.eventSource = new EventSource(url);

    this.eventSource.onmessage = (event: MessageEvent) => {
      try {
        const obs: IoTObservationPayload = JSON.parse(event.data);
        this.liveBySerial.update(map => {
          const next = new Map(map);
          next.set(obs.deviceSerial, obs);
          return next;
        });
      } catch { /* ignore payload malformé */ }
    };

    this.eventSource.onopen  = () => this.connected.set(true);
    this.eventSource.onerror = () => this.connected.set(false);
  }

  reloadRegistry(): void {
    this.registryResource.reload();
  }

  disconnect(): void {
    this.eventSource?.close();
    this.eventSource = null;
    this.connected.set(false);
  }

  ngOnDestroy(): void {
    this.disconnect();
  }
}
