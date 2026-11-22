import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'DONE';

export interface Task {
  id: string;
  projectId: string;
  title: string;
  description: string;
  acceptanceCriteria: string[];
  dependencyIds: string[];
  status: TaskStatus;
  createdAt: string;
  updatedAt: string;
  version: number;
}

export interface TaskHistory {
  id: string;
  fromStatus: TaskStatus | null;
  toStatus: TaskStatus;
  changedAt: string;
}

@Injectable({ providedIn: 'root' })
export class TasksService {
  private readonly http = inject(HttpClient);

  list(projectId: string) {
    return this.http.get<Task[]>(`/api/projects/${projectId}/tasks`);
  }

  create(projectId: string, input: {
    title: string;
    description: string;
    acceptanceCriteria: string[];
    dependencyIds: string[];
  }) {
    return this.http.post<Task>(`/api/projects/${projectId}/tasks`, input);
  }

  updateStatus(task: Task, status: TaskStatus) {
    return this.http.put<Task>(`/api/tasks/${task.id}/status`, { status, version: task.version });
  }

  history(taskId: string) {
    return this.http.get<TaskHistory[]>(`/api/tasks/${taskId}/history`);
  }
}
