import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { NavbarComponent } from '../navbar/navbar.component';
import { SidebarComponent } from '../sidebar/sidebar.component';

@Component({
  selector: 'cs-shell',
  standalone: true,
  imports: [RouterOutlet, NavbarComponent, SidebarComponent],
  template: `
    <div class="cs-shell">
      <cs-navbar />
      <div class="cs-shell__body">
        <cs-sidebar />
        <main class="cs-shell__content">
          <router-outlet />
        </main>
      </div>
    </div>
  `,
  styles: [`
    .cs-shell { display: flex; flex-direction: column; height: 100vh; overflow: hidden; }
    .cs-shell__body { display: flex; flex: 1; overflow: hidden; }
    .cs-shell__content { flex: 1; overflow-y: auto; padding: 1.5rem; background: var(--surface-ground); }
  `],
})
export class ShellComponent {}
