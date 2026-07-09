import { Injectable, signal, computed, inject } from '@angular/core';
import { httpResource, HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { MessageDto } from '../models';

/**
 * MessagingService — /api/v1/messages (caresync-q-messaging, port 8098).
 *
 * ⚠️ Aucune Resource REST codée côté backend à ce jour (entités JPA seulement,
 * chiffrement AES-256-GCM des contenus) — endpoints spécifiés dans
 * messagerie-securisee.feature / UC08_messagerie.feature :
 *
 *   GET  /?patientId=...   → messages où l'utilisateur est expéditeur/destinataire
 *   GET  /{id}             → ouverture (déclenche l'accusé de lecture)
 *   POST /                 → envoi
 */
@Injectable({ providedIn: 'root' })
export class MessagingService {
  private readonly http    = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/messages`;

  readonly patientFilter = signal<string | null>(null);

  readonly messagesResource = httpResource<MessageDto[]>(() => {
    const patientId = this.patientFilter();
    const params: Record<string, string> = {};
    if (patientId) params['patientId'] = patientId;
    return { url: this.baseUrl, params };
  });

  readonly messages   = computed(() => this.messagesResource.hasValue() ? this.messagesResource.value() : []);
  readonly isLoading  = this.messagesResource.isLoading;
  readonly unreadCount = computed(() =>
    this.messages().filter(m => m.destinataires.some(d => d.statut === 'NON_LU')).length
  );

  filterByPatient(patientId: string | null): void {
    this.patientFilter.set(patientId);
  }

  open(id: string) {
    return this.http.get<MessageDto>(`${this.baseUrl}/${id}`);
  }

  send(objet: string, priorite: MessageDto['priorite'], destinataires: string[], patientRef?: string) {
    return this.http.post<MessageDto>(this.baseUrl, { objet, priorite, destinataires, patientRef });
  }

  reload(): void {
    this.messagesResource.reload();
  }
}
