import { Injectable, inject, signal, computed } from '@angular/core';
import { Router } from '@angular/router';
import { KeycloakService } from 'keycloak-angular';

export interface CareUser {
  id:          string;
  username:    string;
  firstName:   string;
  lastName:    string;
  email:       string;
  roles:       string[];
  tenantId:    string;   // établissement Keycloak claim
  token:       string;
}

/**
 * KeycloakAuthService — wrapper réactif (Signals) autour de keycloak-angular.
 *
 * Expose des signaux réactifs pour :
 *  - l'utilisateur courant
 *  - l'état de connexion
 *  - les rôles (pour *ngIf hasRole)
 *
 * RememberMe : stocké dans localStorage 'cs_remember_me'.
 * Au prochain boot, app.config.ts lit cette valeur pour choisir
 * onLoad: 'login-required' (true) ou 'check-sso' (false).
 */
@Injectable({ providedIn: 'root' })
export class KeycloakAuthService {
  private readonly kc     = inject(KeycloakService);
  private readonly router = inject(Router);

  // ── Signals ────────────────────────────────────────────────────────────────
  // authenticated reflète l'état du token Keycloak (kc.isLoggedIn()), indépendamment
  // de la réussite de loadUserProfile() — un aléa sur l'endpoint /account (CORS,
  // permissions du client "account") ne doit jamais faire passer un utilisateur
  // réellement connecté pour déconnecté et le boucler vers /login.
  private readonly authenticated = signal(false);

  readonly currentUser  = signal<CareUser | null>(null);
  readonly isLoggedIn   = computed(() => this.authenticated());
  readonly userRoles    = computed(() => this.currentUser()?.roles ?? []);
  readonly userFullName = computed(() => {
    const u = this.currentUser();
    return u ? `${u.firstName} ${u.lastName}` : '';
  });
  readonly userInitials = computed(() => {
    const u = this.currentUser();
    if (!u) return '?';
    return (u.firstName[0] ?? '') + (u.lastName[0] ?? '');
  });

  /** Charge le profil utilisateur depuis Keycloak après init */
  async loadUser(): Promise<void> {
    if (!this.kc.isLoggedIn()) { this.authenticated.set(false); return; }
    this.authenticated.set(true);

    const instance = this.kc.getKeycloakInstance();
    const claims    = (instance.tokenParsed ?? {}) as Record<string, unknown>;
    const token     = await this.kc.getToken();

    // Rôles dans realm_access.roles (structure Keycloak)
    const roles    = instance.realmAccess?.roles ?? [];
    const tenantId = (claims['tenant_id'] as string) ?? '';

    // Valeurs de base tirées directement des claims du token — ne dépend pas
    // d'un aller-retour réseau vers l'endpoint /account (voir commentaire sur
    // `authenticated` ci-dessus).
    this.currentUser.set({
      id:        (claims['sub'] as string) ?? '',
      username:  (claims['preferred_username'] as string) ?? '',
      firstName: (claims['given_name'] as string) ?? '',
      lastName:  (claims['family_name'] as string) ?? '',
      email:     (claims['email'] as string) ?? '',
      roles,
      tenantId,
      token,
    });

    // Enrichissement best-effort via /account (peut échouer selon la config
    // CORS/permissions du client Keycloak "account" — sans conséquence sur
    // l'état de connexion puisque currentUser est déjà renseigné ci-dessus).
    try {
      const profile = await this.kc.loadUserProfile();
      this.currentUser.update(u => u && ({
        ...u,
        id:        profile.id ?? u.id,
        username:  profile.username ?? u.username,
        firstName: profile.firstName ?? u.firstName,
        lastName:  profile.lastName ?? u.lastName,
        email:     profile.email ?? u.email,
      }));
    } catch (e) {
      console.warn('[Keycloak] Profil détaillé indisponible (/account), claims du token utilisées', e);
    }
  }

  /** Vérifie si l'utilisateur possède au moins un des rôles donnés */
  hasRole(...roles: string[]): boolean {
    return roles.some(r => this.userRoles().includes(r));
  }

  // ── Login / Logout / RememberMe ───────────────────────────────────────────

  login(rememberMe = false): void {
    localStorage.setItem('cs_remember_me', String(rememberMe));
    this.kc.login({ redirectUri: window.location.origin + '/dashboard' });
  }

  async logout(): Promise<void> {
    localStorage.removeItem('cs_remember_me');
    this.currentUser.set(null);
    this.authenticated.set(false);
    await this.kc.logout(window.location.origin + '/login');
  }

  /**
   * Retourne le token Bearer courant.
   * Utilisé par l'intercepteur HTTP pour injecter le header Authorization.
   */
  async getToken(): Promise<string> {
    // Rafraîchit automatiquement si expiré dans moins de 30s
    await this.kc.updateToken(30);
    return this.kc.getToken();
  }
}
