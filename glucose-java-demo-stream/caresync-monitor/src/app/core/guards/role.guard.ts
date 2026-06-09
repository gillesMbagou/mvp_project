import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { KeycloakService } from '../auth/keycloak.service';

export const healthProfessionalGuard: CanActivateFn = () => {
  const keycloak = inject(KeycloakService);
  const router = inject(Router);

  const allowed = keycloak.hasAnyRole('MEDECIN', 'INFIRMIERE', 'HEALTH_PROFESSIONAL', 'ADMIN');
  if (!allowed) {
    return router.createUrlTree(['/forbidden']);
  }
  return true;
};

export const adminGuard: CanActivateFn = () => {
  const keycloak = inject(KeycloakService);
  const router = inject(Router);

  if (!keycloak.hasRole('ADMIN')) {
    return router.createUrlTree(['/forbidden']);
  }
  return true;
};
