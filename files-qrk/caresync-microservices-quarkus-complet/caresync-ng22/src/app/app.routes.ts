import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';
import { roleGuard } from './core/auth/role.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () =>
      import('./pages/login/login.component').then(m => m.LoginComponent),
  },
  {
    path: '',
    loadComponent: () =>
      import('./layout/main-layout/main-layout.component').then(m => m.MainLayoutComponent),
    canActivate: [authGuard],
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./pages/dashboard/dashboard.component').then(m => m.DashboardComponent),
        title: 'Dashboard — CareSync',
      },
      {
        path: 'patients',
        loadComponent: () =>
          import('./pages/patients/patients-list.component').then(m => m.PatientsListComponent),
        title: 'Patients — CareSync',
      },
      {
        path: 'patients/:id',
        loadComponent: () =>
          import('./pages/patients/patient-detail.component').then(m => m.PatientDetailComponent),
        title: 'Dossier Patient — CareSync',
      },
      {
        path: 'alerts',
        loadComponent: () =>
          import('./pages/alerts/alerts.component').then(m => m.AlertsComponent),
        title: 'Alertes — CareSync',
      },
      {
        path: 'analytics',
        canActivate: [() => roleGuard(['medecin', 'admin_etablissement'])],
        loadComponent: () =>
          import('./pages/analytics/analytics.component').then(m => m.AnalyticsComponent),
        title: 'Analytics — CareSync',
      },
      {
        path: 'devices',
        loadComponent: () =>
          import('./pages/devices/devices.component').then(m => m.DevicesComponent),
        title: 'Dispositifs IoT — CareSync',
      },
      {
        path: 'messaging',
        loadComponent: () =>
          import('./pages/messaging/messaging.component').then(m => m.MessagingComponent),
        title: 'Messagerie — CareSync',
      },
      {
        path: 'etablissements',
        canActivate: [() => roleGuard(['admin_etablissement', 'super_admin'])],
        loadComponent: () =>
          import('./pages/etablissements/etablissements-list.component').then(m => m.EtablissementsListComponent),
        title: 'Établissements — CareSync',
      },
      {
        path: 'etablissements/:id',
        canActivate: [() => roleGuard(['admin_etablissement', 'super_admin'])],
        loadComponent: () =>
          import('./pages/etablissements/etablissement-detail.component').then(m => m.EtablissementDetailComponent),
        title: 'Établissement — CareSync',
      },
      {
        path: 'audit',
        canActivate: [() => roleGuard(['super_admin'])],
        loadComponent: () =>
          import('./pages/audit/audit.component').then(m => m.AuditComponent),
        title: 'Audit — CareSync',
      },
      {
        path: 'profile',
        loadComponent: () =>
          import('./pages/profile/profile.component').then(m => m.ProfileComponent),
        title: 'Profil — CareSync',
      },
      {
        path: 'settings',
        canActivate: [() => roleGuard(['admin_etablissement', 'super_admin'])],
        loadComponent: () =>
          import('./pages/settings/settings.component').then(m => m.SettingsComponent),
        title: 'Paramètres — CareSync',
      },
      {
        path: 'unauthorized',
        loadComponent: () =>
          import('./pages/unauthorized/unauthorized.component').then(m => m.UnauthorizedComponent),
        title: 'Accès refusé — CareSync',
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
