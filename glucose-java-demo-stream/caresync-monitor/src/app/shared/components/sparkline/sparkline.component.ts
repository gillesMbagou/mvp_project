import { Component, input, computed } from '@angular/core';

@Component({
  selector: 'cs-sparkline',
  standalone: true,
  template: `
    <svg [attr.width]="width()" [attr.height]="height()" [attr.viewBox]="viewBox()">
      <defs>
        <linearGradient [id]="gradId()" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" [attr.stop-color]="color()" stop-opacity="0.3" />
          <stop offset="100%" [attr.stop-color]="color()" stop-opacity="0" />
        </linearGradient>
      </defs>
      @if (areaPath()) {
        <path [attr.d]="areaPath()!" [attr.fill]="'url(#' + gradId() + ')'" />
      }
      @if (linePath()) {
        <path [attr.d]="linePath()!" fill="none" [attr.stroke]="color()" stroke-width="2" stroke-linejoin="round" />
      }
      @if (threshold() !== undefined) {
        <line
          x1="0" [attr.y1]="threshY()" [attr.x2]="width()" [attr.y2]="threshY()"
          [attr.stroke]="threshColor()" stroke-width="1" stroke-dasharray="4,2"
        />
      }
    </svg>
  `,
})
export class SparklineComponent {
  readonly data = input<number[]>([]);
  readonly color = input('var(--primary-color)');
  readonly threshColor = input('var(--red-400)');
  readonly threshold = input<number | undefined>(undefined);
  readonly width = input(120);
  readonly height = input(40);

  private readonly _id = Math.random().toString(36).slice(2);
  readonly gradId = computed(() => `sg-${this._id}`);
  readonly viewBox = computed(() => `0 0 ${this.width()} ${this.height()}`);

  private scaled = computed(() => {
    const d = this.data();
    if (!d.length) return [];
    const min = Math.min(...d);
    const max = Math.max(...d);
    const range = max - min || 1;
    const w = this.width();
    const h = this.height();
    return d.map((v, i) => ({
      x: (i / (d.length - 1)) * w,
      y: h - ((v - min) / range) * (h - 4) - 2,
    }));
  });

  readonly linePath = computed(() => {
    const pts = this.scaled();
    if (pts.length < 2) return null;
    return pts.map((p, i) => `${i === 0 ? 'M' : 'L'}${p.x.toFixed(1)},${p.y.toFixed(1)}`).join(' ');
  });

  readonly areaPath = computed(() => {
    const pts = this.scaled();
    if (pts.length < 2) return null;
    const h = this.height();
    const line = pts.map((p, i) => `${i === 0 ? 'M' : 'L'}${p.x.toFixed(1)},${p.y.toFixed(1)}`).join(' ');
    return `${line} L${pts[pts.length - 1].x},${h} L0,${h} Z`;
  });

  readonly threshY = computed(() => {
    const d = this.data();
    const t = this.threshold();
    if (t === undefined || !d.length) return 0;
    const min = Math.min(...d);
    const max = Math.max(...d);
    const range = max - min || 1;
    const h = this.height();
    return h - ((t - min) / range) * (h - 4) - 2;
  });
}
