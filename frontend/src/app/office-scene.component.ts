import {
  AfterViewInit, ChangeDetectionStrategy, Component, ElementRef, OnDestroy, computed, effect, input,
  signal, viewChild,
} from '@angular/core';
import type { OfficeRenderer } from './office-renderer';
import { OFFICE_CREW, OFFICE_STATUS, officeStatuses } from './office-state';
import type { TeamRun } from './team.service';

@Component({
  selector: 'app-office-scene',
  templateUrl: './office-scene.component.html',
  styleUrl: './office-scene.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OfficeSceneComponent implements AfterViewInit, OnDestroy {
  readonly run = input<TeamRun | null>(null);
  readonly crew = OFFICE_CREW;
  readonly statuses = computed(() => officeStatuses(this.run()));
  readonly activeCrew = computed(() => this.crew.filter(({ role }) => this.statuses()[role] === 'RUNNING'));
  readonly state = signal<'loading' | 'ready' | 'unavailable'>('loading');
  readonly paused = signal(false);
  readonly reducedMotion = signal(false);
  readonly motionPaused = computed(() => this.paused() || this.reducedMotion());
  private readonly runKey = computed(() => {
    const run = this.run();
    return run ? run.id + '/' + run.attempt_count : '';
  });
  private readonly surface = viewChild.required<ElementRef<HTMLElement>>('surface');
  private readonly canvas = viewChild.required<ElementRef<HTMLCanvasElement>>('canvas');
  private viewport?: IntersectionObserver;
  private resizeObserver?: ResizeObserver;
  private motionPreference?: MediaQueryList;
  private renderer?: OfficeRenderer;
  private initialized = false;
  private destroyed = false;
  private visible = true;

  constructor() {
    effect(() => {
      const statuses = this.statuses();
      const key = this.runKey();
      this.renderer?.setStatuses(statuses, key);
    });
    effect(() => {
      const paused = this.motionPaused();
      this.renderer?.setMotion(this.visible, paused);
    });
  }

  ngAfterViewInit(): void {
    this.motionPreference = window.matchMedia?.('(prefers-reduced-motion: reduce)');
    this.syncMotionPreference();
    this.motionPreference?.addEventListener('change', this.syncMotionPreference);
    this.canvas().nativeElement.addEventListener('webglcontextlost', this.onContextLost);
    const surface = this.surface().nativeElement;
    if (typeof IntersectionObserver === 'undefined') {
      void this.initialize();
      return;
    }
    this.viewport = new IntersectionObserver(([entry]) => {
      this.visible = entry.isIntersecting;
      this.renderer?.setMotion(this.visible, this.motionPaused());
      if (this.visible) void this.initialize();
    });
    this.viewport.observe(surface);
  }

  ngOnDestroy(): void {
    this.destroyed = true;
    this.viewport?.disconnect();
    this.resizeObserver?.disconnect();
    this.motionPreference?.removeEventListener('change', this.syncMotionPreference);
    this.canvas().nativeElement.removeEventListener('webglcontextlost', this.onContextLost);
    this.renderer?.dispose();
  }

  statusFor(role: (typeof OFFICE_CREW)[number]['role']) {
    return OFFICE_STATUS[this.statuses()[role]];
  }

  private async initialize(): Promise<void> {
    if (this.initialized || this.destroyed) return;
    this.initialized = true;
    try {
      const { OfficeRenderer } = await import('./office-renderer');
      if (this.destroyed) return;
      this.renderer = new OfficeRenderer(this.canvas().nativeElement);
      const surface = this.surface().nativeElement;
      this.renderer.resize(surface.clientWidth, surface.clientHeight);
      this.renderer.setStatuses(this.statuses(), this.runKey());
      this.renderer.setMotion(this.visible, this.motionPaused());
      if (typeof ResizeObserver !== 'undefined') {
        this.resizeObserver = new ResizeObserver(([entry]) =>
          this.renderer?.resize(entry.contentRect.width, entry.contentRect.height));
        this.resizeObserver.observe(surface);
      }
      this.state.set('ready');
    } catch {
      this.renderer?.dispose();
      this.renderer = undefined;
      this.state.set('unavailable');
    }
  }

  private readonly syncMotionPreference = (): void => {
    this.reducedMotion.set(this.motionPreference?.matches ?? false);
  };

  private readonly onContextLost = (event: Event): void => {
    event.preventDefault();
    const renderer = this.renderer;
    this.renderer = undefined;
    this.resizeObserver?.disconnect();
    renderer?.dispose(false);
    this.state.set('unavailable');
  };
}
