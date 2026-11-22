import { HttpClient } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { EMPTY, Observable, Subject, catchError, finalize, merge, switchMap, timer } from 'rxjs';

type Tool = 'NODE_CHECK' | 'NODE_RUN';
type Status = 'AWAITING_APPROVAL' | 'QUEUED' | 'RUNNING' | 'CANCEL_REQUESTED' |
  'CANCELLED' | 'SUCCEEDED' | 'FAILED' | 'TIMED_OUT' | 'EXPIRED';

interface ToolRun {
  id: string;
  tool: Tool;
  source: string;
  hash: string;
  image: string | null;
  status: Status;
  approvedBy: string | null;
  output: string | null;
  outputTruncated: boolean;
  errorCode: string | null;
}

@Component({
  selector: 'app-tool-runs',
  imports: [FormsModule, MatButtonModule],
  templateUrl: './tool-runs.component.html',
  styles: `
    :host { display: block; }
    form, label { display: grid; gap: .5rem; }
    form button { justify-self: start; }
    textarea, pre { font-family: ui-monospace, monospace; }
    textarea { width: 100%; box-sizing: border-box; resize: vertical; }
    article { border-top: 1px solid currentColor; margin-top: 1rem; padding: .75rem 0; }
    pre { white-space: pre-wrap; overflow-wrap: anywhere; max-height: 18rem; overflow: auto; }
    .identity { overflow-wrap: anywhere; }
    .actions { display: flex; flex-wrap: wrap; gap: .5rem; }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ToolRunsComponent implements OnInit {
  readonly projectId = input.required<string>();
  private readonly http = inject(HttpClient);
  private readonly destroyRef = inject(DestroyRef);
  private readonly reload = new Subject<void>();
  private readonly headers = { 'X-Office-Tool-Request': '1' };
  readonly enabled = signal<boolean | undefined>(undefined);
  readonly runs = signal<ToolRun[]>([]);
  readonly pending = signal(false);
  readonly listError = signal('');
  readonly actionError = signal('');
  readonly labels: Record<Status, string> = {
    AWAITING_APPROVAL: 'Aguarda aprovação',
    QUEUED: 'Na fila',
    RUNNING: 'Em execução',
    CANCEL_REQUESTED: 'Cancelamento solicitado',
    CANCELLED: 'Cancelada',
    SUCCEEDED: 'Concluída',
    FAILED: 'Falhou',
    TIMED_OUT: 'Limite de tempo atingido',
    EXPIRED: 'Aprovação expirada',
  };
  tool: Tool = 'NODE_CHECK';
  source = '';

  ngOnInit(): void {
    merge(timer(0, 3000), this.reload).pipe(
      switchMap(() => this.http.get<{ enabled: boolean; runs: ToolRun[] }>(this.url()).pipe(
        catchError(() => {
          this.listError.set('Não foi possível carregar o histórico de ferramentas.');
          return EMPTY;
        }),
      )),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe(({ enabled, runs }) => {
      this.listError.set('');
      this.enabled.set(enabled);
      this.runs.set(runs);
    });
  }

  propose(): void {
    if (!this.source.trim() || this.pending()) return;
    this.mutate(this.http.post<ToolRun>(this.url(), { tool: this.tool, source: this.source }, { headers: this.headers }),
      'A proposta não pôde ser criada. Use UTF-8 sem controles invisíveis e até 8 KiB.',
      () => { this.source = ''; });
  }

  decide(run: ToolRun, action: 'approve' | 'cancel'): void {
    if (this.pending()) return;
    const body = action === 'approve' ? { hash: run.hash } : null;
    this.mutate(this.http.post<ToolRun>(`${this.url()}/${run.id}/${action}`, body, { headers: this.headers }),
      'Não foi possível registrar a decisão. Confira o estado atualizado.');
  }

  private mutate(request: Observable<ToolRun>, error: string, onSuccess?: () => void): void {
    this.pending.set(true);
    this.actionError.set('');
    request.pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => this.pending.set(false)),
    ).subscribe({
      next: (run) => {
        onSuccess?.();
        this.runs.update((runs) => runs.some((item) => item.id === run.id)
          ? runs.map((item) => item.id === run.id ? run : item)
          : [run, ...runs].slice(0, 20));
        this.reload.next();
      },
      error: () => {
        this.actionError.set(error);
        this.reload.next();
      },
    });
  }

  private url(): string {
    return `/api/projects/${encodeURIComponent(this.projectId())}/tool-runs`;
  }
}
