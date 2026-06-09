import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { KeycloakService } from '../../core/auth/keycloak.service';
import { ButtonModule } from 'primeng/button';
import { AvatarModule } from 'primeng/avatar';
import { TagModule } from 'primeng/tag';

@Component({
  selector: 'cs-navbar',
  standalone: true,
  imports: [ButtonModule, AvatarModule, TagModule],
  template: `
    <header class="cs-navbar">
      <div class="cs-navbar__brand">
        <span class="cs-navbar__logo">⚕</span>
        <span class="cs-navbar__title">CareSync</span>
        <p-tag value="MONITOR" severity="info" styleClass="cs-navbar__tag" />
      </div>
      <div class="cs-navbar__user">
        <p-avatar
          [label]="initials()"
          shape="circle"
          styleClass="cs-navbar__avatar"
        />
        <span class="cs-navbar__name">{{ keycloak.fullName() }}</span>
        <p-button
          icon="pi pi-sign-out"
          severity="secondary"
          [text]="true"
          (onClick)="logout()"
          pTooltip="Déconnexion"
        />
      </div>
    </header>
  `,
  styles: [`
    .cs-navbar {
      display: flex; align-items: center; justify-content: space-between;
      padding: 0 1.5rem; height: 56px;
      background: var(--surface-card); border-bottom: 1px solid var(--surface-border);
      z-index: 100;
    }
    .cs-navbar__brand { display: flex; align-items: center; gap: 0.5rem; }
    .cs-navbar__logo { font-size: 1.5rem; }
    .cs-navbar__title { font-size: 1.1rem; font-weight: 700; color: var(--primary-color); }
    .cs-navbar__user { display: flex; align-items: center; gap: 0.75rem; }
    .cs-navbar__name { font-size: 0.875rem; font-weight: 500; }
    .cs-navbar__avatar { background: var(--primary-color); color: white; width: 32px; height: 32px; font-size: 0.75rem; }
  `],
})
export class NavbarComponent {
  readonly keycloak = inject(KeycloakService);
  private router = inject(Router);

  initials(): string {
    const name = this.keycloak.fullName();
    return name.split(' ').map(n => n[0]).join('').toUpperCase().slice(0, 2);
  }

  logout(): void {
    this.keycloak.logout(window.location.origin + '/login');
  }
}
