import { Component, inject, input, computed, ChangeDetectionStrategy } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { KeycloakAuthService } from '../../core/auth/keycloak.service';

@Component({
  selector: 'app-unauthorized',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, ButtonModule],
  template: `
    <div style="min-height:60vh;display:flex;flex-direction:column;align-items:center;
                justify-content:center;text-align:center;gap:12px">
      <i class="pi pi-lock" style="font-size:40px;color:var(--cs-urgente)"></i>
      <h2 style="font-size:20px;font-weight:700;color:var(--cs-text);margin:0">Accès refusé</h2>
      <p style="font-size:13px;color:var(--cs-text-3);max-width:360px">
        @if (requiredRoles().length) {
          Cette page nécessite un des rôles suivants : {{ requiredRoles().join(', ') }}.
        } @else {
          Vous n'avez pas les droits nécessaires pour accéder à cette page.
        }
        <br/>Vos rôles actuels : {{ userRoles().join(', ') || 'aucun' }}.
      </p>
      <a routerLink="/dashboard">
        <button pButton icon="pi pi-arrow-left" label="Retour au dashboard" class="p-button-sm"></button>
      </a>
    </div>
  `,
})
export class UnauthorizedComponent {
  private readonly auth = inject(KeycloakAuthService);

  // Bindé depuis le queryParam 'requis' (withComponentInputBinding)
  readonly requis = input<string>('');
  readonly requiredRoles = computed(() => this.requis() ? this.requis().split(',') : []);

  readonly userRoles = this.auth.userRoles;
}
