import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { healthProfessionalGuard } from './core/guards/role.guard';

export const routes: Routes = [
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
  {
    path: 'login',
    loadComponent: () =>
      import('./features/login/login.component').then(m => m.LoginComponent),
  },
  {
    path: '',
    loadComponent: () =>
      import('./layout/shell/shell.component').then(m => m.ShellComponent),
    canActivate: [authGuard],
    children: [
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent),
        canActivate: [healthProfessionalGuard],
        title: 'CareSync – Tableau de bord',
      },
      {
        path: 'patients',
        loadComponent: () =>
          import('./features/patients/patients.component').then(m => m.PatientsComponent),
        title: 'CareSync – Patients',
      },
      {
        path: 'patients/:id',
        loadComponent: () =>
          import('./features/patient-detail/patient-detail.component').then(
            m => m.PatientDetailComponent,
          ),
        title: 'CareSync – Détail patient',
      },
      {
        path: 'devices',
        loadComponent: () =>
          import('./features/devices/devices.component').then(m => m.DevicesComponent),
        title: 'CareSync – Dispositifs IoT',
      },
      {
        path: 'alerts',
        loadComponent: () =>
          import('./features/alerts/alerts.component').then(m => m.AlertsComponent),
        canActivate: [healthProfessionalGuard],
        title: 'CareSync – Alertes',
      },
      {
        path: 'forbidden',
        loadComponent: () =>
          import('./features/login/login.component').then(m => m.LoginComponent),
      },
    ],
  },
  { path: '**', redirectTo: 'dashboard' },
];
