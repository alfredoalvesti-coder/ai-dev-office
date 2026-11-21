import { Component, DestroyRef, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { timeout } from 'rxjs';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'app-root',
  imports: [MatCardModule, MatButtonModule],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  private readonly http = inject(HttpClient);
  private readonly destroyRef = inject(DestroyRef);
  readonly status = signal<'loading' | 'up' | 'down'>('loading');

  constructor() {
    this.checkHealth();
  }

  checkHealth(): void {
    this.status.set('loading');
    this.http
      .get<{ status: string }>('/actuator/health/readiness')
      .pipe(timeout(8000), takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => this.status.set(response?.status === 'UP' ? 'up' : 'down'),
        error: () => this.status.set('down'),
      });
  }
}
