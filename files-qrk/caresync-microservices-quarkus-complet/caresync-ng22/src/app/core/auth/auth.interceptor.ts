import { HttpInterceptorFn, HttpRequest, HttpHandlerFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { from, switchMap } from 'rxjs';
import { KeycloakAuthService } from './keycloak.service';

/**
 * Intercepteur fonctionnel Angular 22.
 *
 * Injecte automatiquement 'Authorization: Bearer <token>' sur toutes
 * les requêtes vers /api (pas sur les assets ou les appels externes).
 *
 * Pour les SSE (EventSource) : EventSource ne supporte pas les headers HTTP.
 * Le token est passé en query param → géré côté SseService séparément.
 */
export const authInterceptor: HttpInterceptorFn = (
  req: HttpRequest<unknown>,
  next: HttpHandlerFn
) => {
  const auth = inject(KeycloakAuthService);

  // Ne pas intercepter les assets ou requêtes non-API
  if (!req.url.includes('/api') && !req.url.includes('/api/v1')) {
    return next(req);
  }

  return from(auth.getToken()).pipe(
    switchMap(token => {
      const authReq = token
        ? req.clone({
            headers: req.headers
              .set('Authorization', `Bearer ${token}`)
              .set('Content-Type', req.headers.get('Content-Type') ?? 'application/json'),
          })
        : req;
      return next(authReq);
    })
  );
};
