import { Injectable, signal, computed } from '@angular/core';
import { httpResource } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { AuditLogDto } from '../models';

/**
 * AuditService — /api/v1/audit (caresync-q-audit, port 8100).
 *
 * ⚠️ Module vide côté backend (que application.yml) — endpoints spécifiés dans
 * UC10_audit.feature :
 *
 *   GET    /export?from=&to=  → export JSON signé HMAC-SHA256 — rôle super_admin
 *   DELETE /{id}               → toujours 405 (journal immuable)
 *
 * Limité à 10 req/s par le rate-limiter du gateway.
 */
@Injectable({ providedIn: 'root' })
export class AuditService {
  private readonly baseUrl = `${environment.apiUrl}/audit`;

  readonly fromDate = signal<string | null>(null);
  readonly toDate   = signal<string | null>(null);

  readonly logsResource = httpResource<AuditLogDto[]>(() => {
    const params: Record<string, string> = {};
    const from = this.fromDate();
    const to   = this.toDate();
    if (from) params['from'] = from;
    if (to)   params['to']   = to;
    return { url: `${this.baseUrl}/export`, params };
  });

  readonly logs      = computed(() => this.logsResource.hasValue() ? this.logsResource.value() : []);
  readonly isLoading = this.logsResource.isLoading;
  readonly error     = this.logsResource.error;

  setRange(from: string | null, to: string | null): void {
    this.fromDate.set(from);
    this.toDate.set(to);
  }

  reload(): void {
    this.logsResource.reload();
  }
}
