import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { Router } from '@angular/router';
import { KeycloakService } from '../auth/keycloak.service';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const keycloak = inject(KeycloakService);

  return next(req).pipe(
    catchError((err: HttpErrorResponse) => {
      if (err.status === 401) {
        keycloak.login();
      } else if (err.status === 403) {
        router.navigate(['/forbidden']);
      } else if (err.status === 0) {
        console.error('API unreachable', err);
      }
      return throwError(() => err);
    }),
  );
};
