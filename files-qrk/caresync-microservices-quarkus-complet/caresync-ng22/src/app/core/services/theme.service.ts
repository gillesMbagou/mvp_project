import { Injectable, signal, effect, inject } from '@angular/core';
import { DOCUMENT } from '@angular/common';

export type Theme = 'light' | 'dark';

/**
 * ThemeService — gestion dark/light mode via Signals.
 *
 * Persistance : localStorage 'cs_theme'.
 * Application : classe CSS 'dark' sur <html> (détectée par PrimeNG Aura
 * via darkModeSelector: 'html.dark' dans app.config.ts).
 *
 * Initialisation dans index.html (script inline) pour éviter le FOUC
 * (Flash Of Unstyled Content) — le thème est appliqué avant Angular.
 */
@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly document = inject(DOCUMENT);

  readonly theme = signal<Theme>(this._getSavedTheme());

  readonly isDark = () => this.theme() === 'dark';

  constructor() {
    // Effet réactif : applique le thème dès que le signal change
    effect(() => {
      const t = this.theme();
      this._applyTheme(t);
      localStorage.setItem('cs_theme', t);
    });
  }

  toggle(): void {
    this.theme.update(t => t === 'light' ? 'dark' : 'light');
  }

  set(theme: Theme): void {
    this.theme.set(theme);
  }

  private _applyTheme(theme: Theme): void {
    const html = this.document.documentElement;
    html.className = theme;  // 'light' ou 'dark'

    // Met à jour la couleur de la barre d'adresse mobile
    const metaTheme = this.document.querySelector('meta[name="theme-color"]');
    if (metaTheme) {
      metaTheme.setAttribute('content', theme === 'dark' ? '#0F172A' : '#0D1B35');
    }
  }

  private _getSavedTheme(): Theme {
    const saved = localStorage.getItem('cs_theme') as Theme | null;
    if (saved === 'light' || saved === 'dark') return saved;
    return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
  }
}
