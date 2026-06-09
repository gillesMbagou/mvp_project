import { Injectable, signal, computed } from '@angular/core';
import Keycloak, { KeycloakProfile } from 'keycloak-js';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class KeycloakService {
  private kc = new Keycloak({
    url: environment.keycloak.url,
    realm: environment.keycloak.realm,
    clientId: environment.keycloak.clientId,
  });

  private _initialized = signal(false);
  private _authenticated = signal(false);
  private _profile = signal<KeycloakProfile | null>(null);
  private _roles = signal<string[]>([]);

  readonly isInitialized = this._initialized.asReadonly();
  readonly isAuthenticated = this._authenticated.asReadonly();
  readonly profile = this._profile.asReadonly();
  readonly roles = this._roles.asReadonly();
  readonly fullName = computed(() => {
    const p = this._profile();
    return p ? `${p.firstName ?? ''} ${p.lastName ?? ''}`.trim() : '';
  });

  async init(): Promise<boolean> {
    try {
      const authenticated = await this.kc.init({
        onLoad: 'login-required',
        checkLoginIframe: false,
        pkceMethod: 'S256',
      });
      this._authenticated.set(authenticated);
      if (authenticated) {
        const profile = await this.kc.loadUserProfile();
        this._profile.set(profile);
        this._roles.set(this.kc.realmAccess?.roles ?? []);
      }
      this._initialized.set(true);
      return authenticated;
    } catch {
      this._initialized.set(true);
      return false;
    }
  }

  getToken(): string | undefined {
    return this.kc.token;
  }

  async refreshToken(minValidity = 30): Promise<boolean> {
    return this.kc.updateToken(minValidity);
  }

  hasRole(role: string): boolean {
    return this._roles().includes(role);
  }

  hasAnyRole(...roles: string[]): boolean {
    return roles.some(r => this._roles().includes(r));
  }

  logout(redirectUri?: string): void {
    this.kc.logout({ redirectUri: redirectUri ?? window.location.origin });
  }

  login(): void {
    this.kc.login();
  }
}
