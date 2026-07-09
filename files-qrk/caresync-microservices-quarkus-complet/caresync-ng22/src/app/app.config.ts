import { ApplicationConfig, APP_INITIALIZER, provideZonelessChangeDetection } from '@angular/core';
import { provideRouter, withViewTransitions, withComponentInputBinding } from '@angular/router';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { providePrimeNG } from 'primeng/config';
import Aura from '@primeng/themes/aura';
import { KeycloakAngularModule, KeycloakService } from 'keycloak-angular';

import { routes } from './app.routes';
import { authInterceptor } from './core/auth/auth.interceptor';
import { environment } from '../environments/environment';

/**
 * Initialise Keycloak avant le rendu de l'application.
 *
 * RememberMe (localStorage 'cs_remember_me') :
 *   true  → 'login-required' : l'utilisateur est renvoyé vers Keycloak s'il
 *            n'est pas connecté (flux standard en prod)
 *   false → 'check-sso'     : vérification silencieuse, pas de redirect forcé
 *
 * silent-check-sso.html doit être dans src/assets/ pour éviter les CORS.
 *
 * Ne doit jamais rejeter : APP_INITIALIZER bloque le bootstrap de toute
 * l'application tant que cette promesse est pending. Si Keycloak est
 * injoignable (serveur down, DNS, etc.), on logue et on laisse l'app
 * démarrer non authentifiée — authGuard redirigera vers /login.
 */
function initializeKeycloak(keycloak: KeycloakService) {
  return () => {
    const rememberMe = localStorage.getItem('cs_remember_me') === 'true';

    return keycloak.init({
      config: {
        url:      environment.keycloak.url,
        realm:    environment.keycloak.realm,
        clientId: environment.keycloak.clientId,
      },
      initOptions: {
        onLoad: rememberMe ? 'login-required' : 'check-sso',
        silentCheckSsoRedirectUri:
          window.location.origin + '/assets/silent-check-sso.html',
        pkceMethod: 'S256',           // sécurité PKCE obligatoire
        checkLoginIframe: false,      // évite les problèmes cross-origin
      },
      // Les rôles Keycloak utilisés dans CareSync
      bearerExcludedUrls: ['/assets'],
    }).catch(err => {
      console.error('[Keycloak] Init impossible, démarrage non authentifié', err);
      return false;
    });
  };
}

export const appConfig: ApplicationConfig = {
  providers: [
    // Zoneless Change Detection
    provideZonelessChangeDetection(),

    // Router avec View Transitions API (page transitions fluides) + input binding
    provideRouter(routes, withViewTransitions(), withComponentInputBinding()),

    // HttpClient avec fetch API + intercepteur JWT
    provideHttpClient(withFetch(), withInterceptors([authInterceptor])),

    // Animations asynchrones (lazy-loaded)
    provideAnimationsAsync(),

    // PrimeNG Aura theme — darkModeSelector aligne sur la classe HTML de index.html
    providePrimeNG({
      theme: {
        preset: Aura,
        options: {
          darkModeSelector: 'html.dark',
          cssLayer: { name: 'primeng', order: 'tailwind-base, primeng, app-styles' },
        },
      },
      ripple: true,
    }),

    // Keycloak
    KeycloakService,
    {
      provide: APP_INITIALIZER,
      useFactory: initializeKeycloak,
      deps: [KeycloakService],
      multi: true,
    },
  ],
};
