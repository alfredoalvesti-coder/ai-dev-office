import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface DraftTask { title: string; description: string; acceptanceCriteria: string[]; }
export interface AnalysisDraft { summary: string; tasks: DraftTask[]; }
export interface AnalysisRun {
  id: string; project_id: string; demand: string; status: string; artifact_json: string | null;
  artifact_hash: string | null; model: string | null; error_code: string | null; expires_at: string | null;
  attempts?: { attemptNumber: number; status: string; errorCode: string | null }[];
  events?: { toStatus: string; reason: string; occurredAt: string }[];
  approval?: { decision: string; createdTaskIds: string } | null;
}

@Injectable({ providedIn: 'root' })
export class AgentsService {
  private readonly http = inject(HttpClient);
  list(projectId: string): Observable<AnalysisRun[]> { return this.http.get<AnalysisRun[]>(`/api/projects/${projectId}/analysis-runs`); }
  create(projectId: string, demand: string): Observable<AnalysisRun> {
    return this.http.post<AnalysisRun>(`/api/projects/${projectId}/analysis-runs`, { demand, idempotencyKey: crypto.randomUUID() });
  }
  detail(id: string): Observable<AnalysisRun> { return this.http.get<AnalysisRun>(`/api/analysis-runs/${id}`); }
  decide(run: AnalysisRun, decision: 'APPROVED' | 'REJECTED'): Observable<AnalysisRun> {
    return this.http.post<AnalysisRun>(`/api/analysis-runs/${run.id}/decision`, { decision, artifactHash: run.artifact_hash });
  }
  retry(id: string): Observable<AnalysisRun> { return this.http.post<AnalysisRun>(`/api/analysis-runs/${id}/retry`, {}); }
  cancel(id: string): Observable<AnalysisRun> { return this.http.post<AnalysisRun>(`/api/analysis-runs/${id}/cancel`, {}); }
}
