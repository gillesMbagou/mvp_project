import {
  Component, inject, signal, computed, OnInit, ChangeDetectionStrategy
} from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { AvatarModule } from 'primeng/avatar';
import { BadgeModule } from 'primeng/badge';
import { ButtonModule } from 'primeng/button';
import { ToastModule } from 'primeng/toast';
import { PopoverModule } from 'primeng/popover';
import { MessageService } from 'primeng/api';

import { KeycloakAuthService } from '../../core/auth/keycloak.service';
import { ThemeService } from '../../core/services/theme.service';
import { AlertService } from '../../core/services/alert.service';
import { AccentThemeService, AccentColor, ACCENT_SWATCH_COLOR } from '../../core/services/accent-theme.service';

interface NavItem {
  label: string;
  icon:  string;
  route: string;
  roles?: string[];
  badgeKey?: 'alerts' | 'critiques';
}

@Component({
  selector: 'app-main-layout',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, AvatarModule, BadgeModule, ButtonModule, ToastModule, PopoverModule],
  providers: [MessageService],
  template: `
    <p-toast position="top-right" [breakpoints]="{'920px': {width: '100%', right: '0', left: '0'}}"/>

    <div class="cs-layout">
      <!-- Sidebar -->
      <aside class="cs-sidebar" [class.collapsed]="sidebarCollapsed()">

        <!-- Logo -->
        <a routerLink="/dashboard" class="cs-sidebar__logo">
          <div class="logo-icon">⚕</div>
          <div>
            <div class="logo-text">CareSync</div>
            <div class="logo-sub">Monitor</div>
          </div>
        </a>

        <!-- Navigation principale -->
        <nav class="cs-sidebar__nav">
          <div class="cs-sidebar__section-label">Principal</div>

          @for (item of mainNav; track item.route) {
            @if (!item.roles || auth.hasRole(...item.roles)) {
              <a class="cs-sidebar__item"
                 [routerLink]="item.route"
                 routerLinkActive="active"
                 [routerLinkActiveOptions]="{ exact: item.route === '/dashboard' }">
                <i class="pi {{ item.icon }}"></i>
                <span class="item-label">{{ item.label }}</span>
                @if (item.badgeKey === 'alerts' && alertService.openAlertsCount() > 0) {
                  <span class="item-badge">{{ alertService.openAlertsCount() }}</span>
                }
                @if (item.badgeKey === 'critiques' && alertService.critiquesCount() > 0) {
                  <span class="item-badge" style="background:var(--cs-critique)">
                    {{ alertService.critiquesCount() }}
                  </span>
                }
              </a>
            }
          }

          @if (adminNav.length && auth.hasRole('admin_etablissement', 'super_admin')) {
            <div class="cs-sidebar__section-label" style="margin-top:8px">Administration</div>
            @for (item of adminNav; track item.route) {
              @if (!item.roles || auth.hasRole(...item.roles)) {
                <a class="cs-sidebar__item" [routerLink]="item.route" routerLinkActive="active">
                  <i class="pi {{ item.icon }}"></i>
                  <span class="item-label">{{ item.label }}</span>
                </a>
              }
            }
          }

          <div class="cs-sidebar__section-label" style="margin-top:8px">Paramètres</div>

          @for (item of settingsNav; track item.route) {
            @if (!item.roles || auth.hasRole(...item.roles)) {
              <a class="cs-sidebar__item" [routerLink]="item.route" routerLinkActive="active">
                <i class="pi {{ item.icon }}"></i>
                <span class="item-label">{{ item.label }}</span>
              </a>
            }
          }
        </nav>

        <!-- Footer utilisateur -->
        <div class="cs-sidebar__footer">
          <div class="cs-sidebar__item" (click)="toggleSidebar()"
               style="justify-content:center;margin-bottom:4px">
            <i class="pi" [class]="sidebarCollapsed() ? 'pi-chevron-right' : 'pi-chevron-left'"></i>
          </div>

          @if (auth.currentUser(); as user) {
            <div class="cs-sidebar__user" (click)="showUserMenu = !showUserMenu">
              <p-avatar
                [label]="auth.userInitials()"
                styleClass="mr-2"
                [style]="{ background: 'var(--cs-teal)', color: '#fff', 'font-size': '12px' }"
                shape="circle" size="normal"/>
              <div style="overflow:hidden">
                <div class="user-name">{{ user.firstName }} {{ user.lastName }}</div>
                <div class="user-role">{{ user.roles[0] || 'utilisateur' }}</div>
              </div>
              <i class="pi pi-ellipsis-v" style="margin-left:auto;color:rgba(255,255,255,.4)"></i>
            </div>
          }

          <!-- Menu contextuel utilisateur -->
          @if (showUserMenu) {
            <div style="background:rgba(255,255,255,.05);border-radius:8px;padding:4px;margin-top:4px">
              <button class="cs-sidebar__item" (click)="auth.logout()">
                <i class="pi pi-sign-out" style="color:var(--cs-critique)"></i>
                <span class="item-label" style="color:var(--cs-critique)">Se déconnecter</span>
              </button>
            </div>
          }
        </div>
      </aside>

      <!-- Main  -->
      <div class="cs-main">

        <!-- Topbar -->
        <header class="cs-topbar">
          <!-- Titre de la page courante injecté via router -->
          <span class="topbar-title">{{ pageTitle() }}</span>

          <!-- SSE status -->
          @if (alertService.sseConnected()) {
            <div class="cs-live" style="margin-left:12px">
              <div class="dot"></div>
              <span>Live</span>
            </div>
          }

          <div class="topbar-spacer"></div>

          <!-- Notifications -->
          @if (alertService.openAlertsCount() > 0) {
            <a routerLink="/alerts" style="position:relative;display:inline-flex">
              <button class="cs-theme-toggle" style="color:var(--cs-urgente);border-color:var(--cs-urgente-bg)">
                <i class="pi pi-bell"></i>
              </button>
              <span style="position:absolute;top:-4px;right:-4px;background:var(--cs-critique);
                           color:#fff;font-size:9px;font-weight:700;padding:1px 5px;
                           border-radius:8px;min-width:16px;text-align:center">
                {{ alertService.openAlertsCount() }}
              </span>
            </a>
          }

          <!-- Theme toggle -->
          <button class="cs-theme-toggle" (click)="theme.toggle()"
                  [title]="theme.isDark() ? 'Mode clair' : 'Mode sombre'">
            <i class="pi" [class]="theme.isDark() ? 'pi-sun' : 'pi-moon'"></i>
          </button>

          <!-- Sélecteur de couleur d'accent -->
          <button class="cs-theme-toggle" (click)="accentPanel.toggle($event)" title="Couleur du thème">
            <i class="pi pi-palette"></i>
          </button>
          <p-popover #accentPanel>
            <div class="cs-accent-picker">
              <button class="cs-accent-swatch cs-accent-swatch--reset"
                      [class.active]="accentTheme.accent() === null"
                      title="Couleur par défaut"
                      (click)="accentTheme.set(null); accentPanel.hide()">
                <i class="pi pi-ban"></i>
              </button>
              @for (color of accentTheme.colors; track color) {
                <button class="cs-accent-swatch"
                        [class.active]="accentTheme.accent() === color"
                        [style.background]="accentSwatchColor(color)"
                        [title]="color"
                        (click)="accentTheme.set(color); accentPanel.hide()">
                </button>
              }
            </div>
          </p-popover>

          <!-- Avatar -->
          @if (auth.currentUser(); as user) {
            <p-avatar
              [label]="auth.userInitials()"
              [style]="{ background: 'var(--cs-teal)', color: '#fff', 'font-size': '12px', cursor: 'pointer' }"
              shape="circle" size="normal"
              (click)="showUserMenu = !showUserMenu"/>
          }
        </header>

        <!-- Content avec router-outlet -->
        <main class="cs-content">
          <router-outlet/>
        </main>
      </div>
    </div>
  `,
})
export class MainLayoutComponent implements OnInit {
  readonly auth         = inject(KeycloakAuthService);
  readonly theme        = inject(ThemeService);
  readonly accentTheme  = inject(AccentThemeService);
  readonly alertService = inject(AlertService);

  sidebarCollapsed = signal(false);
  showUserMenu     = false;

  readonly pageTitle = signal('Dashboard');

  readonly mainNav: NavItem[] = [
    { label: 'Dashboard',      icon: 'pi-home',       route: '/dashboard' },
    { label: 'Patients',       icon: 'pi-users',      route: '/patients' },
    { label: 'Alertes',        icon: 'pi-bell',       route: '/alerts',   badgeKey: 'alerts' },
    { label: 'Analytics',      icon: 'pi-chart-line', route: '/analytics', roles: ['medecin', 'admin_etablissement'] },
    { label: 'Dispositifs IoT',icon: 'pi-wifi',        route: '/devices' },
    { label: 'Messagerie',     icon: 'pi-envelope',    route: '/messaging' },
  ];

  readonly adminNav: NavItem[] = [
    { label: 'Établissements', icon: 'pi-building',    route: '/etablissements', roles: ['admin_etablissement', 'super_admin'] },
    { label: 'Audit',          icon: 'pi-shield',       route: '/audit',          roles: ['super_admin'] },
  ];

  readonly settingsNav: NavItem[] = [
    { label: 'Profil',      icon: 'pi-user',            route: '/profile' },
    { label: 'Paramètres',  icon: 'pi-cog',             route: '/settings', roles: ['admin_etablissement', 'super_admin'] },
  ];

  async ngOnInit(): Promise<void> {
    await this.auth.loadUser();
    await this.alertService.connectSSE();
  }

  toggleSidebar(): void {
    this.sidebarCollapsed.update(v => !v);
  }

  accentSwatchColor(color: AccentColor): string {
    return ACCENT_SWATCH_COLOR[color];
  }
}
