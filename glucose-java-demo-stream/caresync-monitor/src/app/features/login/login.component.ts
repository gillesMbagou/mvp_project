import { Component, inject, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { KeycloakService } from '../../core/auth/keycloak.service';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';

@Component({
  selector: 'cs-login',
  standalone: true,
  imports: [ButtonModule, CardModule],
  template: `
    <div class="cs-login">
      <p-card styleClass="cs-login__card">
        <ng-template pTemplate="header">
          <div class="cs-login__header">
            <span class="cs-login__logo">⚕</span>
            <h1 class="cs-login__title">CareSync Monitor</h1>
            <p class="cs-login__subtitle">Portail de surveillance IoT des patients</p>
          </div>
        </ng-template>
        @if (isForbidden) {
          <div class="cs-login__forbidden">
            <i class="pi pi-lock cs-login__forbidden-icon"></i>
            <p>Vous n'avez pas les droits requis pour accéder à cette page.</p>
            <p>Rôles nécessaires : MEDECIN, INFIRMIERE, HEALTH_PROFESSIONAL ou ADMIN.</p>
          </div>
        }
        <p-button
          label="Se connecter avec Keycloak"
          icon="pi pi-key"
          styleClass="cs-login__btn"
          (onClick)="login()"
          size="large"
        />
      </p-card>
    </div>
  `,
  styles: [`
    .cs-login {
      display: flex; align-items: center; justify-content: center;
      min-height: 100vh; background: var(--surface-ground);
    }
    .cs-login__card { width: 420px; }
    .cs-login__header { text-align: center; padding: 2rem 1rem 1rem; }
    .cs-login__logo { font-size: 3rem; }
    .cs-login__title { margin: 0.5rem 0 0.25rem; font-size: 1.75rem; font-weight: 700; color: var(--primary-color); }
    .cs-login__subtitle { color: var(--text-color-secondary); font-size: 0.875rem; margin: 0; }
    .cs-login__btn { width: 100%; margin-top: 1rem; }
    .cs-login__forbidden {
      text-align: center; padding: 1rem;
      background: var(--red-50); border-radius: 8px;
      color: var(--red-700); margin-bottom: 1rem;
    }
    .cs-login__forbidden-icon { font-size: 2rem; margin-bottom: 0.5rem; display: block; }
  `],
})
export class LoginComponent implements OnInit {
  private keycloak = inject(KeycloakService);
  private router = inject(Router);
  isForbidden = this.router.url.includes('forbidden');

  ngOnInit(): void {
    if (this.keycloak.isAuthenticated()) {
      this.router.navigate(['/dashboard']);
    }
  }

  login(): void {
    this.keycloak.login();
  }
}
