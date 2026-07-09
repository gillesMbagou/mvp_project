import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { KeycloakAuthService } from './keycloak.service';

/**
 * Guard fonctionnel Angular 22.
 * Redirige vers /login si l'utilisateur n'est pas connecté.
 */
export const authGuard: CanActivateFn = async () => {
  const auth   = inject(KeycloakAuthService);
  const router = inject(Router);

  if (auth.isLoggedIn()) return true;

  // Charger l'utilisateur si Keycloak a déjà un token (check-sso)
  await auth.loadUser();
  if (auth.isLoggedIn()) return true;

  return router.createUrlTree(['/login']);
};
