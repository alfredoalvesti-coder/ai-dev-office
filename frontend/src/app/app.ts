import { Component, DestroyRef, computed, inject, signal, ChangeDetectionStrategy } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize, interval, Observable, Subscription, timeout } from 'rxjs';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { AuthService } from './auth.service';
import { Page, Project, ProjectsService } from './projects.service';
import { Task, TaskHistory, TaskStatus, TasksService } from './tasks.service';
import { AgentsService, AnalysisDraft, AnalysisRun } from './agents.service';
import { ProjectEvent, TeamFinalArtifact, TeamRun, TeamService } from './team.service';
import { OfficeSceneComponent } from './office-scene.component';
import { OFFICE_CREW, OFFICE_STATUS, officeStatuses } from './office-state';
import { ToolRunsComponent } from './tool-runs.component';

@Component({
  selector: 'app-root',
  imports: [FormsModule, MatCardModule, MatButtonModule, OfficeSceneComponent, ToolRunsComponent],
  templateUrl: './app.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './app.scss',
})
export class App {
  private readonly http = inject(HttpClient);
  private readonly destroyRef = inject(DestroyRef);
  private readonly auth = inject(AuthService);
  private readonly projectsApi = inject(ProjectsService);
  private readonly tasksApi = inject(TasksService);
  private readonly agentsApi = inject(AgentsService);
  private readonly teamApi = inject(TeamService);
  private teamEventsSubscription?: Subscription;
  private teamRunsSubscription?: Subscription;
  private teamRefreshPending = false;
  private teamProjectVersion = 0;
  readonly officeCrew = OFFICE_CREW;
  readonly officeStatusLabels = OFFICE_STATUS;
  readonly officeStatuses = officeStatuses;
  readonly status = signal<'loading' | 'up' | 'down'>('loading');
  readonly signedIn = computed(() => this.auth.authenticated());
  readonly projects = signal<Project[]>([]);
  readonly selectedProjectId = signal('');
  readonly tasks = signal<Task[]>([]);
  readonly historyTaskId = signal('');
  readonly history = signal<TaskHistory[]>([]);
  readonly page = signal(0);
  readonly totalPages = signal(0);
  readonly runs = signal<AnalysisRun[]>([]);
  readonly analyzing = signal(false);
  readonly teamRuns = signal<TeamRun[]>([]);
  readonly selectedTeamRunId = signal('');
  readonly selectedTeamRun = computed(() =>
    this.teamRuns().find((run) => run.id === this.selectedTeamRunId()) ?? null);
  readonly teamEvents = signal<ProjectEvent[]>([]);
  readonly eventConnection = signal<'connecting'|'connected'|'disconnected'>('disconnected');
  readonly teamBusy = signal(false);
  readonly busy = signal(false);
  readonly error = signal('');
  username = '';
  password = '';
  projectName = '';
  projectDescription = '';
  taskTitle = '';
  taskDescription = '';
  taskCriteria = '';
  taskDependencyIds: string[] = [];
  demand = '';
  teamDemand = '';

  constructor() {
    this.checkHealth();
    interval(3000).pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      if (this.selectedProjectId() && this.signedIn()) this.loadRuns();
      if (this.selectedProjectId() && this.signedIn()) this.loadTeamRuns();
    });
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

  signIn(): void {
    this.error.set('');
    this.auth.signIn(this.username, this.password);
    this.loadProjects(0, true);
  }

  signOut(): void {
    this.auth.signOut();
    this.teamEventsSubscription?.unsubscribe();
    this.stopTeamRunsRefresh();
    this.password = '';
    this.projects.set([]);
    this.tasks.set([]);
    this.selectedProjectId.set('');
    this.history.set([]);
    this.runs.set([]);
    this.teamRuns.set([]);
    this.selectedTeamRunId.set('');
    this.teamEvents.set([]);
    this.eventConnection.set('disconnected');
    this.error.set('');
  }

  loadProjects(page = this.page(), authenticating = false): void {
    this.busy.set(true);
    this.error.set('');
    this.projectsApi.list(page).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (result: Page<Project>) => {
        this.projects.set(result.content);
        this.page.set(result.number);
        this.totalPages.set(result.totalPages);
        if (authenticating) this.auth.confirmAuthentication();
        this.busy.set(false);
        if (this.selectedProjectId() && !result.content.some((project) => project.id === this.selectedProjectId())) {
          this.selectProject('');
        }
      },
      error: () => {
        this.busy.set(false);
        this.error.set(authenticating
          ? 'Não foi possível entrar. Confira as credenciais e tente novamente.'
          : 'Não foi possível carregar os projetos. Tente novamente.');
        if (authenticating) this.auth.signOut();
      },
    });
  }

  createProject(event: Event): void {
    event.preventDefault();
    this.busy.set(true);
    this.error.set('');
    this.projectsApi.create({ name: this.projectName, description: this.projectDescription })
      .pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        next: (project) => {
          this.projectName = '';
          this.projectDescription = '';
          this.busy.set(false);
          this.loadProjects(0);
          this.selectProject(project.id);
        },
        error: () => {
          this.busy.set(false);
          this.error.set('Não foi possível criar o projeto. Verifique os campos e tente novamente.');
        },
      });
  }

  selectProject(projectId: string): void {
    this.teamEventsSubscription?.unsubscribe();
    this.stopTeamRunsRefresh();
    const version = this.teamProjectVersion;
    this.eventConnection.set('disconnected');
    this.selectedProjectId.set(projectId);
    this.tasks.set([]);
    this.history.set([]);
    this.runs.set([]);
    this.teamRuns.set([]);
    this.selectedTeamRunId.set('');
    this.teamEvents.set([]);
    if (!projectId) return;
    this.busy.set(true);
    this.tasksApi.list(projectId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (tasks) => { this.tasks.set(tasks); this.busy.set(false); },
      error: () => { this.busy.set(false); this.error.set('Não foi possível carregar as tarefas.'); },
    });
    this.loadRuns();
    this.loadTeamRuns();
    this.teamEventsSubscription = this.teamApi.watch(projectId, (event) => {
      if (this.teamProjectVersion !== version || event.projectId !== projectId) return;
      this.teamEvents.update((events) => [...events, event].slice(-100));
      this.loadTeamRuns(true);
    }, (state) => {
      if (this.teamProjectVersion === version) this.eventConnection.set(state);
    });
  }

  loadTeamRuns(refreshAfterPending = false): void {
    const projectId = this.selectedProjectId();
    if (!projectId) return;
    if (this.teamRunsSubscription) {
      this.teamRefreshPending ||= refreshAfterPending;
      return;
    }
    const request = new Subscription();
    this.teamRunsSubscription = request;
    request.add(this.teamApi.list(projectId).pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => {
        if (this.teamRunsSubscription !== request) return;
        this.teamRunsSubscription = undefined;
        const refresh = this.teamRefreshPending;
        this.teamRefreshPending = false;
        if (refresh && !this.destroyRef.destroyed) this.loadTeamRuns();
      }),
    ).subscribe({
      next: (runs) => {
        if (this.selectedProjectId() !== projectId) return;
        this.teamRuns.set(runs);
        if (!runs.some((run) => run.id === this.selectedTeamRunId())) {
          this.selectedTeamRunId.set(runs[0]?.id ?? '');
        }
      },
      error: () => {
        if (this.selectedProjectId() === projectId) this.error.set('Não foi possível carregar o histórico da equipe.');
      },
    }));
  }

  private stopTeamRunsRefresh(): void {
    this.teamProjectVersion++;
    const request = this.teamRunsSubscription;
    this.teamRunsSubscription = undefined;
    this.teamRefreshPending = false;
    this.teamBusy.set(false);
    request?.unsubscribe();
  }

  startTeamRun(event:Event):void {
    event.preventDefault();const projectId=this.selectedProjectId();if(!projectId||!this.teamDemand.trim())return;
    const version = this.teamProjectVersion;
    this.teamBusy.set(true);
    this.teamApi.create(projectId,this.teamDemand.trim()).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next:run=>{
        if (this.teamProjectVersion !== version) return;
        this.teamBusy.set(false);
        this.teamDemand='';
        this.selectedTeamRunId.set(run.id);
        this.loadTeamRuns(true);
      },
      error:()=>{
        if (this.teamProjectVersion !== version) return;
        this.teamBusy.set(false);
        this.error.set('Não foi possível iniciar a equipe. Confirme Ollama, modelo e banco.');
      },
    });
  }

  teamArtifact(run:TeamRun):TeamFinalArtifact|null {
    if(!run.artifact_json)return null;try{return JSON.parse(run.artifact_json) as TeamFinalArtifact;}catch{return null;}
  }

  teamDecision(run:TeamRun,decision:'APPROVED'|'REJECTED'):void {
    this.updateTeamRun(this.teamApi.decide(run, decision),
      'Não foi possível registrar a decisão. Recarregue o plano e tente novamente.',
      decision === 'APPROVED' ? () => this.selectProject(run.project_id) : undefined);
  }
  retryTeam(run: TeamRun): void {
    this.updateTeamRun(this.teamApi.retry(run.id), 'Não foi possível repetir a execução da equipe.');
  }
  cancelTeam(run: TeamRun): void {
    this.updateTeamRun(this.teamApi.cancel(run.id), 'Não foi possível cancelar a equipe.');
  }
  private updateTeamRun(operation: Observable<TeamRun>, message: string, onSuccess = () => this.loadTeamRuns(true)): void {
    const version = this.teamProjectVersion;
    operation.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => { if (this.teamProjectVersion === version) onSuccess(); },
      error: () => { if (this.teamProjectVersion === version) this.error.set(message); },
    });
  }
  teamEventLabel(event:ProjectEvent):string { const role=String(event.payload['role']??'');return `${event.type}${role?` · ${role}`:''}`; }

  loadRuns(): void {
    const projectId = this.selectedProjectId();
    if (!projectId) return;
    this.agentsApi.list(projectId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (runs) => this.runs.set(runs),
      error: () => this.error.set('NÃ£o foi possÃ­vel carregar as anÃ¡lises.'),
    });
  }

  analyzeDemand(event: Event): void {
    event.preventDefault();
    const projectId = this.selectedProjectId();
    if (!projectId || !this.demand.trim()) return;
    this.analyzing.set(true);
    this.agentsApi.create(projectId, this.demand.trim()).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => { this.demand = ''; this.analyzing.set(false); this.loadRuns(); },
      error: () => { this.analyzing.set(false); this.error.set('NÃ£o foi possÃ­vel iniciar. Confira se o Ollama estÃ¡ ativo e o modelo instalado.'); },
    });
  }

  draft(run: AnalysisRun): AnalysisDraft | null {
    if (!run.artifact_json) return null;
    try { return JSON.parse(run.artifact_json) as AnalysisDraft; } catch { return null; }
  }

  decideRun(run: AnalysisRun, decision: 'APPROVED' | 'REJECTED'): void {
    this.agentsApi.decide(run, decision).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => { this.loadRuns(); if (decision === 'APPROVED') this.selectProject(run.project_id); },
      error: () => this.error.set('NÃ£o foi possÃ­vel registrar a decisÃ£o. Recarregue a anÃ¡lise e tente novamente.'),
    });
  }

  retryRun(run: AnalysisRun): void {
    this.agentsApi.retry(run.id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({ next: () => this.loadRuns(), error: () => this.error.set('NÃ£o foi possÃ­vel repetir esta anÃ¡lise.') });
  }

  cancelRun(run: AnalysisRun): void {
    this.agentsApi.cancel(run.id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({ next: () => this.loadRuns(), error: () => this.error.set('NÃ£o foi possÃ­vel cancelar esta anÃ¡lise.') });
  }

  createTask(event: Event): void {
    event.preventDefault();
    const projectId = this.selectedProjectId();
    if (!projectId) return;
    this.busy.set(true);
    this.error.set('');
    this.tasksApi.create(projectId, {
      title: this.taskTitle,
      description: this.taskDescription,
      acceptanceCriteria: this.taskCriteria.split('\n').map((line) => line.trim()).filter(Boolean),
      dependencyIds: this.taskDependencyIds,
    }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.taskTitle = '';
        this.taskDescription = '';
        this.taskCriteria = '';
        this.taskDependencyIds = [];
        this.selectProject(projectId);
      },
      error: () => {
        this.busy.set(false);
        this.error.set('Não foi possível criar a tarefa. Confira os campos e as dependências.');
      },
    });
  }

  changeTaskStatus(task: Task, event: Event): void {
    const next = (event.target as HTMLSelectElement).value as TaskStatus;
    this.tasksApi.updateStatus(task, next).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (updated) => {
        this.tasks.update((tasks) => tasks.map((item) => item.id === updated.id ? updated : item));
        this.error.set('');
      },
      error: () => {
        this.error.set('O estado não foi alterado. As tarefas foram recarregadas; tente novamente.');
        this.selectProject(task.projectId);
      },
    });
  }

  loadHistory(taskId: string): void {
    this.historyTaskId.set(taskId);
    this.tasksApi.history(taskId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (history) => this.history.set(history),
      error: () => this.error.set('Não foi possível carregar o histórico da tarefa.'),
    });
  }

  statusLabel(status: TaskStatus): string {
    return ({ TODO: 'A fazer', IN_PROGRESS: 'Em andamento', DONE: 'Concluída' })[status];
  }
}
