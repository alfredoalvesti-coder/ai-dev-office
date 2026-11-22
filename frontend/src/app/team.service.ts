import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, Subscription, switchMap } from 'rxjs';

export interface TeamStepArtifact { summary: string; tasks: { title: string; description: string; acceptanceCriteria: string[] }[]; findings: string[]; recommendation: string; }
export interface TeamFinalArtifact { summary: string; tasks: { title: string; description: string; acceptanceCriteria: string[] }[]; steps: { role: string; artifact: TeamStepArtifact }[]; }
export interface TeamRunStep { attemptNumber: number; stepNumber: number; role: string; status: string; artifactJson: string | null; errorCode: string | null; }
export interface TeamRun {
  id: string; project_id: string; demand: string; status: string; attempt_count: number; context_version: string;
  artifact_json: string | null; artifact_hash: string | null; model: string | null; error_code: string | null; expires_at: string | null;
  steps: TeamRunStep[]; approval: { decision: string; createdTaskIds: string } | null;
}
export interface ProjectEvent { cursor: number; eventId: string; projectId: string; executionId: string; type: string; payload: Record<string, unknown>; occurredAt: string; }

@Injectable({ providedIn: 'root' })
export class TeamService {
  private readonly http = inject(HttpClient);
  list(projectId: string): Observable<TeamRun[]> { return this.http.get<TeamRun[]>(`/api/projects/${projectId}/team-runs`); }
  create(projectId: string, demand: string): Observable<TeamRun> { return this.http.post<TeamRun>(`/api/projects/${projectId}/team-runs`, { demand, idempotencyKey: crypto.randomUUID() }); }
  detail(id: string): Observable<TeamRun> { return this.http.get<TeamRun>(`/api/team-runs/${id}`); }
  retry(id: string): Observable<TeamRun> { return this.http.post<TeamRun>(`/api/team-runs/${id}/retry`, {}); }
  cancel(id: string): Observable<TeamRun> { return this.http.post<TeamRun>(`/api/team-runs/${id}/cancel`, {}); }
  decide(run: TeamRun, decision: 'APPROVED' | 'REJECTED'): Observable<TeamRun> { return this.http.post<TeamRun>(`/api/team-runs/${run.id}/decision`, { decision, artifactHash: run.artifact_hash }); }
  history(projectId: string, after: number): Observable<ProjectEvent[]> { return this.http.get<ProjectEvent[]>(`/api/projects/${projectId}/events`, { params: { after, limit: 200 } }); }

  watch(projectId: string, onEvent: (event: ProjectEvent) => void, onState: (state: 'connecting'|'connected'|'disconnected') => void): Subscription {
    let socket: WebSocket | null = null;
    let active: Subscription | undefined;
    let stopped = false;
    let retryDelay = 1000;
    let retryTimer: ReturnType<typeof setTimeout> | null = null;
    let cursor = 0;
    const recent = new Set<string>();
    const accept = (event: ProjectEvent) => {
      if (event.cursor <= cursor || recent.has(event.eventId)) return;
      cursor = event.cursor; recent.add(event.eventId);
      if (recent.size > 500) recent.clear();
      onEvent(event);
    };
    const connect = () => {
      if (stopped) return;
      onState('connecting');
      active=this.http.post<{ticket:string}>(`/api/projects/${projectId}/events/tickets`, {}).pipe(
        switchMap(({ticket}) => new Observable<WebSocket>((subscriber) => {
          if(stopped) { subscriber.complete(); return; }
          const scheme = location.protocol === 'https:' ? 'wss:' : 'ws:';
          socket = new WebSocket(`${scheme}//${location.host}/ws/projects/${projectId}`, [`ticket.${ticket}`, 'ai-office-ticket']);
          socket.onopen = () => { retryDelay=1000; onState('connected'); this.history(projectId,cursor).subscribe({next: items=>items.forEach(accept)}); };
          socket.onmessage = message => { try { accept(JSON.parse(message.data) as ProjectEvent); } catch { /* invalid frames are discarded */ } };
          socket.onerror = () => onState('disconnected');
          socket.onclose = () => { onState('disconnected'); subscriber.complete(); };
          return () => { socket?.close(); socket=null; };
        }))
      ).subscribe({
        error: () => scheduleRetry(),
        complete: () => { if (!stopped) scheduleRetry(); },
      });
    };
    const scheduleRetry = () => {
      if (stopped || retryTimer) return;
      retryTimer=setTimeout(()=>{retryTimer=null;connect();},retryDelay);
      retryDelay=Math.min(retryDelay*2,10000);
    };
    connect();
    return new Subscription(()=>{stopped=true;if(retryTimer)clearTimeout(retryTimer);active?.unsubscribe();socket?.close();});
  }
}
