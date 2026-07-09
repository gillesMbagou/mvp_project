import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { KeycloakAuthService } from './keycloak.service';

/**
 * Guard de rôle (utilisé en inline dans les routes).
 * Exemple : canActivate: [() => roleGuard(['medecin', 'admin_etablissement'])]
 */
export function roleGuard(requiredRoles: string[]) {
  const auth   = inject(KeycloakAuthService);
  const router = inject(Router);

  if (auth.hasRole(...requiredRoles)) return true;

  console.warn(`[RoleGuard] Rôles requis: ${requiredRoles.join(', ')} | Rôles actuels: ${auth.userRoles().join(', ')}`);
  return router.createUrlTree(['/unauthorized'], { queryParams: { requis: requiredRoles.join(',') } });
}
