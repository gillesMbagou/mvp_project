# CareSync Monitor — Frontend Angular 22

## Stack

| Technologie | Version | Rôle |
|---|---|---|
| Angular | 22 | Framework frontend |
| PrimeNG | 18 | Composants UI |
| Keycloak Angular | 19 | Auth OAuth2/OIDC |
| RxJS | 7 | Réactivité |
| Zod | 3 | Validation DTOs |

## Fonctionnalités Angular 22 utilisées

### `httpResource()` — remplace HttpClient + subscribe
```typescript
// Re-fetch automatique quand searchQuery() change
readonly patientsResource = httpResource<PageResult<PatientDto>>(
  () => ({ url: '/api/v1/patients', params: { search: this.searchQuery() } })
);

// Accès
this.patientsResource.value()     // signal<T | undefined>
this.patientsResource.isLoading() // signal<boolean>
this.patientsResource.error()     // signal<unknown>
this.patientsResource.reload()    // force un nouveau fetch
```

### Signal Form — debounce sans subscribe
```typescript
readonly searchControl = new FormControl('');
readonly debouncedSearch = toSignal(
  this.searchControl.valueChanges.pipe(debounceTime(350), distinctUntilChanged()),
  { initialValue: '' }
);
// debouncedSearch() est un signal → peut être consommé dans httpResource()
```

### `@for`, `@if`, `@switch` — syntaxe Angular 22
```html
@if (resource.isLoading()) { <p-skeleton/> }
@else { @for (item of items(); track item.id) { <div>{{ item.name }}</div> } }
```

### Signals partout
```typescript
readonly theme    = signal<'dark'|'light'>('light');
readonly isDark   = computed(() => this.theme() === 'dark');
// effect() dans ThemeService applique la classe CSS automatiquement
```

## Structure

```
src/app/
├── app.config.ts          ← Keycloak + PrimeNG + Router + HttpClient
├── app.routes.ts          ← Lazy loading + authGuard + roleGuard
├── core/
│   ├── auth/
│   │   ├── keycloak.service.ts   ← Wrapper Keycloak avec Signals
│   │   ├── auth.interceptor.ts   ← Injection JWT automatique
│   │   ├── auth.guard.ts         ← Protection des routes
│   │   └── role.guard.ts         ← Guard par rôle Keycloak
│   └── services/
│       ├── theme.service.ts      ← Dark/light mode Signal
│       ├── patient.service.ts    ← httpResource patients
│       └── alert.service.ts      ← httpResource + SSE alertes
├── layout/main-layout/
│   └── main-layout.component.ts  ← Sidebar + Topbar + Router
└── pages/
    ├── login/                    ← Keycloak redirect + RememberMe
    ├── dashboard/                ← KPIs + alertes live + IoT
    ├── patients/                 ← DataTable + Signal Form search
    ├── alerts/                   ← SSE stream + acquittement
    └── analytics/                ← httpResource + Charts TimescaleDB
```

## Dark/Light Mode

```typescript
// Dans ThemeService — signal réactif
readonly theme = signal<Theme>(this._getSavedTheme());

effect(() => {
  document.documentElement.className = this.theme(); // 'dark' ou 'light'
  localStorage.setItem('cs_theme', this.theme());
});

// Dans un composant
theme.toggle(); // bascule
theme.set('dark'); // force
```

PrimeNG détecte la classe `html.dark` via `darkModeSelector: 'html.dark'` dans `app.config.ts`.

## RememberMe

```typescript
// Login — avant le redirect Keycloak
localStorage.setItem('cs_remember_me', String(rememberMe));
keycloak.login({ onLoad: rememberMe ? 'login-required' : 'check-sso' });
```

## SSE (Server-Sent Events)

EventSource ne supporte pas les headers HTTP → token JWT en query param :
```typescript
const url = `/api/stream/alerts?token=${encodeURIComponent(token)}`;
const eventSource = new EventSource(url);
```

## Lancer le projet

```bash
npm install
npm start
# → http://localhost:4200
# → Keycloak requis sur http://localhost:8180
```
