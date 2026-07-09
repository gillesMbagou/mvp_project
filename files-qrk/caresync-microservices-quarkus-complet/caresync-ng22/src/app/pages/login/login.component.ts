import {
  Component, inject, signal, ChangeDetectionStrategy, OnInit
} from '@angular/core';
import { Router } from '@angular/router';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { CheckboxModule } from 'primeng/checkbox';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';

import { KeycloakAuthService } from '../../core/auth/keycloak.service';
import { ThemeService } from '../../core/services/theme.service';

/**
 * LoginComponent — Page d'authentification CareSync.
 *
 * L'authentification réelle est déléguée à Keycloak.
 * Cette page ne contient PAS de formulaire username/password
 * (c'est Keycloak qui héberge la page de login via redirect OIDC).
 *
 * Ce composant gère :
 *  - Le bouton "Se connecter" → redirect vers Keycloak
 *  - RememberMe → stocké dans localStorage avant le redirect
 *  - Le toggle dark/light mode
 *  - Le redirect si déjà connecté
 */
@Component({
  selector: 'app-login',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, CheckboxModule, ButtonModule, CardModule],
  template: `
    <div class="login-page" [class.dark]="theme.isDark()">

      <!-- Fond décoratif -->
      <div class="login-bg">
        <div class="bg-circle bg-circle--1"></div>
        <div class="bg-circle bg-circle--2"></div>
      </div>

      <!-- Theme toggle (haut droit) -->
      <button class="theme-toggle" (click)="theme.toggle()">
        <i class="pi" [class]="theme.isDark() ? 'pi-sun' : 'pi-moon'"></i>
      </button>

      <!-- Card centrale -->
      <div class="login-card">

        <!-- Logo -->
        <div class="login-header">
          <div class="login-logo">⚕</div>
          <h1 class="login-title">CareSync</h1>
          <p class="login-subtitle">Plateforme de télésurveillance médicale</p>
        </div>

        <!-- Badges pathologies -->
        <div class="login-badges">
          <span class="path-badge">Diabète T2</span>
          <span class="path-badge">Insuffisance cardiaque</span>
          <span class="path-badge">BPCO</span>
        </div>

        <!-- Divider -->
        <div class="login-divider">
          <span>Connexion sécurisée</span>
        </div>

        <!-- RememberMe -->
        <label class="remember-row">
          <p-checkbox [formControl]="rememberMeControl" [binary]="true" inputId="rememberMe"/>
          <span>Rester connecté sur cet appareil</span>
        </label>

        <!-- Bouton Keycloak -->
        <p-button
          label="Se connecter avec Keycloak"
          icon="pi pi-shield"
          iconPos="left"
          [loading]="loading()"
          styleClass="w-full login-btn"
          (onClick)="login()"/>

        <!-- Info sécurité -->
        <p class="login-info">
          <i class="pi pi-lock" style="font-size:11px"></i>
          Authentification OAuth2 · PKCE · MFA · Conforme HDS
        </p>

        <!-- Environnement -->
        <div class="login-env">
          <span class="env-badge env-badge--dev">DEV</span>
          <span>Quarkus 3 · Java 21 · Kafka KRaft</span>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .login-page {
      min-height: 100vh;
      display: flex;
      align-items: center;
      justify-content: center;
      background: var(--cs-bg);
      position: relative;
      overflow: hidden;
      font-family: 'Inter', system-ui, sans-serif;
    }
    .login-bg {
      position: absolute; inset: 0; pointer-events: none;
    }
    .bg-circle {
      position: absolute; border-radius: 50%;
      background: var(--cs-teal); opacity: .06;
    }
    .bg-circle--1 { width: 600px; height: 600px; top: -200px; right: -200px; }
    .bg-circle--2 { width: 400px; height: 400px; bottom: -150px; left: -100px; }
    .theme-toggle {
      position: absolute; top: 20px; right: 20px;
      width: 40px; height: 40px; border-radius: 50%;
      border: 1px solid var(--cs-border); background: var(--cs-surface);
      color: var(--cs-text-2); display: flex; align-items: center; justify-content: center;
      cursor: pointer; font-size: 16px; transition: all .15s; z-index: 10;
    }
    .theme-toggle:hover { background: var(--cs-teal-light); color: var(--cs-teal); border-color: var(--cs-teal); }
    .login-card {
      background: var(--cs-surface);
      border: 1px solid var(--cs-border);
      border-radius: 20px;
      padding: 40px 36px;
      width: 100%; max-width: 400px;
      box-shadow: 0 20px 60px rgba(0,0,0,.1);
      position: relative; z-index: 1;
    }
    .login-header { text-align: center; margin-bottom: 24px; }
    .login-logo {
      width: 56px; height: 56px; border-radius: 16px;
      background: var(--cs-navy); color: var(--cs-teal);
      font-size: 28px; display: flex; align-items: center;
      justify-content: center; margin: 0 auto 12px;
    }
    .login-title { font-size: 24px; font-weight: 700; color: var(--cs-text); margin: 0; }
    .login-subtitle { font-size: 13px; color: var(--cs-text-3); margin: 4px 0 0; }
    .login-badges { display: flex; gap: 6px; flex-wrap: wrap; justify-content: center; margin-bottom: 24px; }
    .path-badge {
      font-size: 11px; font-weight: 500; padding: 3px 10px;
      border-radius: 20px; background: var(--cs-teal-light);
      color: var(--cs-teal-dark); border: 1px solid rgba(14,165,180,.2);
    }
    .login-divider {
      display: flex; align-items: center; gap: 10px;
      color: var(--cs-text-3); font-size: 12px; margin-bottom: 20px;
    }
    .login-divider::before, .login-divider::after {
      content: ''; flex: 1; height: 1px; background: var(--cs-border);
    }
    .remember-row {
      display: flex; align-items: center; gap: 10px;
      font-size: 13px; color: var(--cs-text-2);
      margin-bottom: 20px; cursor: pointer;
    }
    .login-btn { width: 100%; }
    ::ng-deep .login-btn.p-button {
      background: var(--cs-navy) !important; border-color: var(--cs-navy) !important;
      padding: 12px !important; font-size: 14px !important; border-radius: 10px !important;
      transition: all .2s !important;
    }
    ::ng-deep .login-btn.p-button:hover {
      background: var(--cs-navy-light) !important; border-color: var(--cs-navy-light) !important;
      transform: translateY(-1px); box-shadow: 0 8px 20px rgba(13,27,53,.3) !important;
    }
    .login-info {
      text-align: center; font-size: 11px; color: var(--cs-text-3);
      margin: 14px 0 0; display: flex; align-items: center; justify-content: center; gap: 5px;
    }
    .login-env {
      margin-top: 20px; padding-top: 16px;
      border-top: 1px solid var(--cs-border);
      display: flex; align-items: center; gap: 8px;
      font-size: 11px; color: var(--cs-text-3);
    }
    .env-badge { font-size: 9px; font-weight: 700; padding: 2px 6px; border-radius: 4px; }
    .env-badge--dev { background: rgba(245,158,11,.15); color: #D97706; }
    .env-badge--prod { background: rgba(16,185,129,.15); color: #059669; }
  `],
})
export class LoginComponent implements OnInit {
  readonly auth   = inject(KeycloakAuthService);
  readonly theme  = inject(ThemeService);
  readonly router = inject(Router);

  readonly rememberMeControl = new FormControl(
    localStorage.getItem('cs_remember_me') === 'true'
  );
  readonly loading = signal(false);

  ngOnInit(): void {
    // Déjà connecté → redirect vers dashboard
    if (this.auth.isLoggedIn()) {
      this.router.navigate(['/dashboard']);
    }
  }

  login(): void {
    this.loading.set(true);
    const rememberMe = !!this.rememberMeControl.value;
    this.auth.login(rememberMe);
  }
}
