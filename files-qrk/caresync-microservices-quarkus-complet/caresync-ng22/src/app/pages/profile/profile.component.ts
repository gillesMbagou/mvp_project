import { Component, inject, ChangeDetectionStrategy } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { KeycloakAuthService } from '../../core/auth/keycloak.service';

@Component({
  selector: 'app-profile',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ButtonModule],
  template: `
<div style="display:flex;flex-direction:column;gap:20px;max-width:560px">
  <h2 style="font-size:18px;font-weight:700;color:var(--cs-text);margin:0">Profil</h2>

  @if (auth.currentUser(); as user) {
    <div class="cs-card" style="display:flex;align-items:center;gap:16px">
      <div style="width:56px;height:56px;border-radius:50%;display:flex;align-items:center;
                  justify-content:center;font-size:18px;font-weight:700;color:var(--cs-teal-contrast);flex-shrink:0;
                  background:var(--cs-teal)">
        {{ auth.userInitials() }}
      </div>
      <div>
        <div style="font-size:16px;font-weight:700;color:var(--cs-text)">{{ auth.userFullName() }}</div>
        <div style="font-size:12px;color:var(--cs-text-3)">{{ user.email || user.username }}</div>
      </div>
    </div>

    <div class="cs-card">
      <div class="cs-card__header"><div class="cs-card__title">Informations</div></div>
      <dl style="display:grid;grid-template-columns:140px 1fr;gap:10px;font-size:13px;margin:0">
        <dt style="color:var(--cs-text-3)">Nom d'utilisateur</dt><dd style="margin:0;color:var(--cs-text)">{{ user.username }}</dd>
        <dt style="color:var(--cs-text-3)">Rôles</dt><dd style="margin:0;color:var(--cs-text)">{{ user.roles.join(', ') || '—' }}</dd>
        <dt style="color:var(--cs-text-3)">Établissement (tenant)</dt><dd style="margin:0;color:var(--cs-text)">{{ user.tenantId || '—' }}</dd>
      </dl>
    </div>

    <div>
      <button pButton icon="pi pi-sign-out" label="Se déconnecter"
              class="p-button-outlined p-button-danger p-button-sm"
              (click)="auth.logout()"></button>
    </div>
  }
</div>
  `,
})
export class ProfileComponent {
  readonly auth = inject(KeycloakAuthService);
}
