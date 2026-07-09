import { Component, inject, ChangeDetectionStrategy } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { ThemeService } from '../../core/services/theme.service';

@Component({
  selector: 'app-settings',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ButtonModule],
  template: `
<div style="display:flex;flex-direction:column;gap:20px;max-width:560px">
  <h2 style="font-size:18px;font-weight:700;color:var(--cs-text);margin:0">Paramètres</h2>

  <div class="cs-card">
    <div class="cs-card__header"><div class="cs-card__title">Apparence</div></div>
    <div style="display:flex;align-items:center;justify-content:space-between">
      <span style="font-size:13px;color:var(--cs-text-2)">Thème</span>
      <button pButton [icon]="theme.isDark() ? 'pi pi-sun' : 'pi pi-moon'"
              [label]="theme.isDark() ? 'Mode clair' : 'Mode sombre'"
              class="p-button-outlined p-button-sm"
              (click)="theme.toggle()"></button>
    </div>
  </div>

  <div class="cs-card" style="color:var(--cs-text-3);font-size:12px">
    D'autres paramètres (notifications, préférences d'alerte, gestion des accès établissement)
    seront ajoutés au fur et à mesure de l'implémentation des services correspondants côté backend.
  </div>
</div>
  `,
})
export class SettingsComponent {
  readonly theme = inject(ThemeService);
}
