import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { KeycloakService } from '../auth/keycloak.service';

export const authGuard: CanActivateFn = () => {
  const keycloak = inject(KeycloakService);
  const router = inject(Router);

  if (keycloak.isAuthenticated()) {
    return true;
  }
  keycloak.login();
  return router.createUrlTree(['/login']);
};
