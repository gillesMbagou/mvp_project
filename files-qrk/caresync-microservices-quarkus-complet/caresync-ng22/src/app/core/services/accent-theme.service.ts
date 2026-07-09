import { Injectable, signal, effect, inject } from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { ThemeService } from './theme.service';

export type AccentColor = 'blue' | 'green' | 'orange' | 'purple' | 'red' | 'yellow';

/**
 * Feuilles de style non-injectées déclarées dans angular.json (styles[]),
 * une par combinaison couleur/mode. Chaque fichier ne fait que surcharger
 * les tokens PrimeNG --p-primary-* / --p-highlight-* (cf.
 * src/styles/themes/_palettes.scss) — le reste de l'UI (surfaces, dark
 * mode) reste piloté par ThemeService.
 */
const BUNDLE_NAMES: Record<AccentColor, { light: string; dark: string }> = {
  blue:   { light: 'theme-saga-blue',    dark: 'theme-dark-blue' },
  green:  { light: 'theme-light-green',  dark: 'theme-dark-green' },
  orange: { light: 'theme-light-orange', dark: 'theme-dark-orange' },
  purple: { light: 'theme-light-purple', dark: 'theme-dark-purple' },
  red:    { light: 'theme-light-red',    dark: 'theme-dark-red' },
  yellow: { light: 'theme-light-yellow', dark: 'theme-dark-yellow' },
};

/** Nuance 500 de chaque accent, utilisée pour l'aperçu des pastilles. */
export const ACCENT_SWATCH_COLOR: Record<AccentColor, string> = {
  blue: '#3b82f6', green: '#22c55e', orange: '#f97316',
  purple: '#a855f7', red: '#ef4444', yellow: '#eab308',
};

const LINK_ID = 'cs-accent-stylesheet';
const STORAGE_KEY = 'cs_accent_color';

@Injectable({ providedIn: 'root' })
export class AccentThemeService {
  private readonly document = inject(DOCUMENT);
  private readonly themeService = inject(ThemeService);

  readonly colors: AccentColor[] = ['blue', 'green', 'orange', 'purple', 'red', 'yellow'];

  /** null = pas de surcharge, couleur par défaut du preset Aura. */
  readonly accent = signal<AccentColor | null>(this._getSavedAccent());

  constructor() {
    effect(() => {
      const color = this.accent();
      const dark = this.themeService.isDark();
      this._applyAccent(color, dark);
    });
  }

  set(color: AccentColor | null): void {
    this.accent.set(color);
  }

  private _applyAccent(color: AccentColor | null, dark: boolean): void {
    const link = this.document.getElementById(LINK_ID) as HTMLLinkElement | null;

    if (!color) {
      link?.remove();
      localStorage.removeItem(STORAGE_KEY);
      return;
    }

    const bundle = BUNDLE_NAMES[color][dark ? 'dark' : 'light'];
    const href = `${bundle}.css`;

    if (link) {
      link.href = href;
    } else {
      const el = this.document.createElement('link');
      el.id = LINK_ID;
      el.rel = 'stylesheet';
      el.href = href;
      this.document.head.appendChild(el);
    }
    localStorage.setItem(STORAGE_KEY, color);
  }

  private _getSavedAccent(): AccentColor | null {
    const saved = localStorage.getItem(STORAGE_KEY) as AccentColor | null;
    return saved && this.colors.includes(saved) ? saved : null;
  }
}
