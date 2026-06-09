import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { KeycloakService } from '../../core/auth/keycloak.service';

interface NavItem {
  icon: string;
  label: string;
  route: string;
  roles?: string[];
}

@Component({
  selector: 'cs-sidebar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  template: `
    <nav class="cs-sidebar">
      @for (item of visibleItems(); track item.route) {
        <a
          [routerLink]="item.route"
          routerLinkActive="cs-sidebar__item--active"
          class="cs-sidebar__item"
        >
          <i [class]="'pi ' + item.icon"></i>
          <span>{{ item.label }}</span>
        </a>
      }
    </nav>
  `,
  styles: [`
    .cs-sidebar {
      width: 220px; min-width: 220px;
      display: flex; flex-direction: column; gap: 0.25rem;
      padding: 1rem 0.75rem;
      background: var(--surface-card); border-right: 1px solid var(--surface-border);
      overflow-y: auto;
    }
    .cs-sidebar__item {
      display: flex; align-items: center; gap: 0.75rem;
      padding: 0.65rem 0.875rem; border-radius: 8px;
      text-decoration: none; font-size: 0.875rem; font-weight: 500;
      color: var(--text-color-secondary);
      transition: background 0.15s, color 0.15s;
    }
    .cs-sidebar__item:hover { background: var(--surface-hover); color: var(--text-color); }
    .cs-sidebar__item--active { background: var(--primary-100); color: var(--primary-color); }
    .cs-sidebar__item i { font-size: 1rem; width: 20px; text-align: center; }
  `],
})
export class SidebarComponent {
  private keycloak = inject(KeycloakService);

  private items: NavItem[] = [
    { icon: 'pi-home', label: 'Tableau de bord', route: '/dashboard', roles: ['MEDECIN', 'INFIRMIERE', 'HEALTH_PROFESSIONAL', 'ADMIN'] },
    { icon: 'pi-users', label: 'Patients', route: '/patients' },
    { icon: 'pi-wifi', label: 'Dispositifs IoT', route: '/devices' },
    { icon: 'pi-bell', label: 'Alertes', route: '/alerts', roles: ['MEDECIN', 'INFIRMIERE', 'HEALTH_PROFESSIONAL', 'ADMIN'] },
  ];

  visibleItems(): NavItem[] {
    return this.items.filter(item =>
      !item.roles || item.roles.some(r => this.keycloak.hasRole(r)),
    );
  }
}
