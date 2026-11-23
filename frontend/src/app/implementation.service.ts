import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export interface ProposedFile {
  path: string;
  content: string;
  rationale: string;
}

export interface ImplementationProposal {
  summary: string;
  files: ProposedFile[];
}

export interface ImplementationRun {
  id: string;
  project_id: string;
  team_run_id: string;
  status: string;
  artifact_json: string | null;
  artifact_hash: string | null;
  model: string | null;
  error_code: string | null;
  expires_at: string | null;
  workspace_relative: string | null;
  workspaceFiles: string[];
  requested_by: string;
  approved_by: string | null;
  applied_at: string | null;
}

@Injectable({ providedIn: 'root' })
export class ImplementationService {
  private readonly http = inject(HttpClient);

  list(projectId: string): Observable<ImplementationRun[]> {
    return this.http.get<ImplementationRun[]>(`/api/projects/${projectId}/implementations`);
  }

  create(projectId: string, teamRunId: string): Observable<ImplementationRun> {
    return this.http.post<ImplementationRun>(
      `/api/projects/${projectId}/team-runs/${teamRunId}/implementations`,
      {},
    );
  }

  decide(run: ImplementationRun, decision: 'APPROVED' | 'REJECTED'): Observable<ImplementationRun> {
    return this.http.post<ImplementationRun>(`/api/implementations/${run.id}/decision`, {
      decision,
      artifactHash: run.artifact_hash,
    });
  }

  cancel(id: string): Observable<ImplementationRun> {
    return this.http.post<ImplementationRun>(`/api/implementations/${id}/cancel`, {});
  }

  workspaceFiles(projectId: string): Observable<string[]> {
    return this.http.get<string[]>(`/api/projects/${projectId}/workspace/files`);
  }

  workspaceFile(projectId: string, path: string): Observable<{ path: string; content: string }> {
    return this.http.get<{ path: string; content: string }>(
      `/api/projects/${projectId}/workspace/files/content`,
      { params: { path } },
    );
  }
}
